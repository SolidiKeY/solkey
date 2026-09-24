# Known Bugs

Open defects in constructs solkey already claims to support. Missing features belong in
`docs/taclet-ideas.md` instead. Each entry has a reproducer. **Delete an entry in the same change
that fixes it**, and add a regression example to `keyext.solidity.examples/TestSuite.sol`.

## Proves something false

None known. Unbounded integers are a design choice: see Tier 5 in `docs/taclet-ideas.md`.

## Crashes at load or during the proof

None known.

## Proof gets stuck on program text

- **`new` written straight into a memory-struct field.** `m.f = new uint[](2);` is stuck,
  because `memoryArrayFreshAlloc` binds only a local. Workaround: allocate into a local first,
  then assign it.
- **Internal calls inside expressions.** `uint y = one() + 1;` and `a[f()] = g();` are stuck,
  because `functionBodyExpand` inlines only a call that is the whole statement. A capture rule
  that binds the call to a temporary would let the existing expansion take over.

## True facts that cannot be proved

- **A fixed-size array's `size` is not tied to its declared length.** For `uint[3] f`,
  `f.length == 3` is unprovable. This is sound: `size` stays arbitrary, and `delete f` keeps it
  (`delNodeFixed`). It needs a length axiom per declaration, or a PO antecedent.
- **A symbolic `bool` is not known to be `TRUE` or `FALSE`.** No rule states
  `b = TRUE | b = FALSE`, so `bool x = b == true; bool y = b == false; assert(x || y);` ends at
  `==> b = TRUE, b = FALSE`. Reading an unwritten `bool` mapping key hits the same wall:
  after `m[true] = 1; m[false] = 2;`, `assert(m[b] == 1 || m[b] == 2)` stays open. A
  `\find(==> b = TRUE) \replacewith(b = FALSE ==>)` rule would close both.
