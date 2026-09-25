# Known Bugs

Open defects in constructs solkey already claims to support. Missing features belong in
`docs/taclet-ideas.md` instead. Each entry has a reproducer. **Delete an entry in the same change
that fixes it**, and add a regression example to `keyext.solidity.examples/TestSuite.sol`.

## Proves something false

Unbounded integers are a design choice: see Tier 5 in `docs/taclet-ideas.md`.

- **A root-anchored fresh memory fixed-size array has length 0.** A fresh memory object's
  `size` resolves through `readOnAddM` and `defaultDef` to 0, which is right only for dynamic
  arrays. A fixed member of a memory struct is exempt: its path ends in a `FixedField` constant
  that carries the declared length (`defaultFixedSize`, docs/storage.md section 8c). A local
  array has no such field, so `uint[3] memory x; assert(x.length == 0);` closes, and so does
  `x[1] = 5; assert(false);` in box mode, because the bounds check always reverts. The same
  holds for `uint[2][3] memory y; y[0].length == 0` and for elements of `new uint[2][](n)`. The
  EVM (`--solc`) fails all of them. The fix is to stamp the length at allocation: split
  `memoryReferenceDeclFreshAlloc` by a varcond on `aliasType` that writes `size` the way
  `memoryArrayFreshAlloc` does, and record the element length per identity for symbolic
  element counts. Copying such an array into storage carries the 0 along, but `fixedSize`
  speaks only about the initial storage, so no contradiction arises from it.

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

- **The length of a fixed-size array element of a dynamic array is unknown.** For
  `uint[3][] g`, `g[i].length == 3` is unprovable: the element is reached through `at(i)`, which
  carries no field kind, so `fixedSize` (docs/storage.md section 8c) cannot see the declared
  length. Root fields and struct members are covered.
