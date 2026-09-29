# Known Bugs

Open defects in constructs solkey already claims to support. Missing features belong in
`docs/taclet-ideas.md` instead. Each entry has a reproducer. **Delete an entry in the same change
that fixes it**, and add a regression example to `keyext.solidity.examples/TestSuite.sol`.

## Proves something false

Unbounded integers are a design choice: see Tier 5 in `docs/taclet-ideas.md`.

None known.

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

None known.
