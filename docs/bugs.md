# Known Bugs

Open defects in constructs solkey already claims to support. Missing features belong in
`docs/taclet-ideas.md` instead. Each entry has a reproducer. **Delete an entry in the same change
that fixes it**, and add a regression example to `keyext.solidity.examples/TestSuite.sol`.
`docs/limitations.md` puts these in context; most were found by the second solc port and the
real-world ports, and their faithful forms sit in `keyext.solidity.examples/solc/open/` and
`real-world/open/`. "EVM" means `./run-key.sh FILE --solc`.
`docs/semantics-bugs-fix-ideas.md` lists the fix ideas and the plan for the semantics bugs below.

## Proves something false

Unbounded integers are a design choice: see Tier 5 in `docs/taclet-ideas.md`. They make
`a = 2**256-1; unchecked { c = a + 1; } assert(c > a);` close, and make
`uint16 r = (t ? 63 : 255) + (f ? 63 : 255);` (a `uint8` overflow, `Panic(0x11)`) close as
non-reverting. Not planned now (fix ideas 6A–6C).

- **Elementary type conversions are the identity.** `parseFunctionCall` returns the argument of
  a `typeConversion` unchanged (no truncation, no sign reinterpretation):
  `uint32 p = 0x12345678; uint16 q = uint16(p); assert(q == 0x12345678);` closes; the EVM fails
  it. `SolcTypesOpen` `castSmallerTruncates`, `downcastTruncates`, `sameSizeReinterprets`, ….
  Not planned now (fix idea 4A).
- **`try` and `call{value}` assume the callee leaves the caller's storage alone**, also when it
  is the contract itself. `tryCallNoCallbackBox` / `sendNoCallbackBox` follow the default
  `transferSemantics:noCallback`, whose gas-stipend argument holds for `transfer` only:
  `function setIx() external { ix = 42; }` and, in a box function with `p` the contract's own
  address, `ix = 0; try C(payable(p)).setIx() { assert(ix == 0); } catch (bytes memory) {}`
  closes; the EVM fails it. Same for `payable(p).call{value: 0}("")` into a `receive` that
  writes. `-O transferSemantics:withCallback` keeps both open.
  `SolcPaymentsOpen.trySuccessKeepsStorageUnsound`, `callKeepsStorageUnsound`.
  Planned later: 5A (split the option) + 5D (view/pure frame).

## Crashes at load or during the proof

None known. A construct the parser does not support (`this`, `super`, `block.*`, `tx.*`, an
event) is refused at load with a `SolidityParseException` naming it; those are in
`docs/taclet-ideas.md`.

## Proof gets stuck on program text

- **A compound assignment whose right side is a storage path.** `pot += other;`,
  `m_a *= m_a;`, `x += m_a;` (local target) stay as program text: the terminals take a
  `SimpleExpression`, the `*AssignValueRhsCapture` rules a `NonSimpleExpression[primitive]`,
  which excludes paths (`NonSimpleExpressionSVSort.isPathShaped`). Workaround:
  `uint o = other; pot += o;`. A storage read as the mapping key (`m[r] += 1`) stalls the same
  way.
- **A state variable assigned from a memory array element.** `uint[] memory b = new uint[](2);`
  `b[0] = 7; x = b[0];` stays as program text; `uint y = b[0]; x = y;` closes. Same when `b` is
  a memory parameter.
- **A local of contract type.** `C c = C(address(0)); assert(address(c) == address(0));` loads
  but leaves `c = address(0);` as program text: the contract conversion is not modelled.

## True facts that cannot be proved

- **A variable mentioned only in a loop invariant loses its value.** `\dropEffectlessElementaries`
  (`DropEffectlessElementariesCondition.searchTerm`) does not see a loop's `LoopSpec` bindings, so
  `simplifyUpdate*` drops the update before `whileInvariantBox`:
  `uint i = 7; uint j = 0; /// @custom:key invariant j <= n && i == 7` `while (j < n) j = j + 1;`
  leaves `==> i = 7`. A parameter alike (`for (uint i = n; i < 100; i++)` with invariant
  `n <= i`). Workaround: mention the variable after the loop, or use a literal.
- **Symbolic `sdiv` bounds remain incomplete.** The default `NON_LIN_ARITH_NONE`
  gives `defOps_div`/`defOps_sdiv` infinite cost. For a non-negative dividend,
  `sdiv`/`smod` now equal `div`/`mod` and positive-divisor quotient bounds automate;
  a negative dividend or divisor still gets no quotient bound.
- **Parameter upper bounds, enum ranges and environment ranges are missing.** Generated
  obligations now assume `uint`/`uintN` inputs are non-negative, closing the three
  compound-subtraction ports. `assert(y <= 255)` for a `uint8` parameter and enum
  parameter bounds remain unprovable. `msg.value >= 0` and non-payable `msg.value == 0`
  are still assumed only for specified functions; `msg.sender` lacks its address range.
  Full range support remains deferred (10A/10B; 10C fixes unspecified `msg.value`).
- **A succedent `\exists` whose body has a nested quantifier is never instantiated.**
  `ex_pull_out0..3` (`formulaNormalizationRules.key`) lack `\heuristics(pullOutQuantifierEx)`,
  unlike Java KeY: `requires \forall uint i; i < vals.length -> vals[i] <= vals[k]`,
  `ensures \exists uint w; w < vals.length && res == vals[w] && (\forall uint i; … vals[i] <= vals[w])`
  with `res = vals[k]` stays open. `real-world/open/DocsBallot.sol` `winnerName`.
