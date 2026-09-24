# Known Bugs

Open defects in constructs solkey already claims to support. Missing features belong in
`docs/taclet-ideas.md` instead. Each entry has a reproducer. **Delete an entry in the same change
that fixes it**, and add a regression example to `keyext.solidity.examples/TestSuite.sol`.

## Proves something false

Unbounded integers are a design choice: see Tier 5 in `docs/taclet-ideas.md`.

- **A fresh memory fixed-size array has length 0.** A fresh memory object's `size` resolves
  through `readOnAddM` and `defaultDef` to 0 at every nesting level, which is right only for
  dynamic arrays. `uint[3] memory x; assert(x.length == 0);` closes, and so does
  `x[1] = 5; assert(false);` in box mode, because the bounds check always reverts. The same holds
  for a fixed member of a memory struct (`S memory s; s.items.length == 0`), for
  `uint[2][3] memory y; y[0].length == 0`, and for elements of `new uint[2][](n)`. The EVM
  (`--solc`) fails all of them.

## Crashes at load or during the proof

None known.

## Proof gets stuck on program text

- **Internal calls are never inlined.** Inside a Solidity body, `one();`, `uint y = one();`,
  `uint y = one() + 1;` and `a[f()] = g();` are all stuck: a call there is a
  `FunctionCallExpression`, while `functionBodyExpand` matches only the synthesized top-level
  `f()@C` form (`FunctionBodyStatement`), and only as the whole modality program. A rule that
  expands `v = f(args);` inside a context, reusing `ExpandFunctionBody`, would cover every
  shape, since declarations drop to assignments and operator unfolds already capture the call.
  Callees written with `return e;` stay stuck on their own: no rule consumes a `ReturnStatement`.

## True facts that cannot be proved

- **A storage fixed-size array's `size` is not tied to its declared length.** For `uint[3] f`,
  `f.length == 3` is unprovable, and so is `f[2] = 1;` in diamond mode (its out-of-bounds branch
  stays feasible). This is sound: `size` stays arbitrary, and `delete f` keeps it
  (`delNodeFixed`). It needs a length axiom per declaration, or a PO antecedent. Memory fixed-size
  arrays have the opposite problem: see "Proves something false".
