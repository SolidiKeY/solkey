# Known Bugs

Open defects in constructs solkey already claims to support. Missing features belong in
`docs/taclet-ideas.md` instead. Each entry has a reproducer. **Delete an entry in the same change
that fixes it**, and add a regression example to `keyext.solidity.examples/TestSuite.sol`.

## Proves something false

None known. Unbounded integers are a design choice: see Tier 5 in `docs/taclet-ideas.md`.

## Crashes at load or during the proof

- **Hex literals.** `uint x = 0x42;` throws `NumberFormatException: "x42"` in
  `SolJSONParser.parseLiteral`, which reads every number literal as decimal. The whole file
  fails to load. Parse the `0x` prefix with radix 16.
- **Self-recursive struct types.** `struct s2 { mapping(k => s2) recursive; }` throws an NPE in
  `getOrCreateMappingKeYSolidityType`, and the whole file fails to load. The solc ports work
  around it by unrolling the hierarchy.
- **Non-integer mapping keys.** `mapping(bool => uint)` loads, then the first index write dies
  with a `TermCreationException`, because `at(Field)` expects an `int` argument and gets
  `TRUE:bool`. `address` keys can be reached only from a `.key` problem, because the obligation
  generator refuses an `address` parameter. Either widen the key sort, or have the loader reject
  the declaration and say why.

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
