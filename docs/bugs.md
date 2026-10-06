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

None known.

## True facts that cannot be proved

- **An `address` named return starts unconstrained.** `ExpandFunctionBody.zero` initialises
  only `int` and `bool` returns, so after `function z() internal pure returns (address r) {}`,
  `address a = z(); assert(a == address(0));` leaves `==> r = 0` open; solc returns
  `address(0)`. Every value type should start at its default (solidity-lean already does).
