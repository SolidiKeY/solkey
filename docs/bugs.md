# Known Bugs

Open defects in constructs solkey already claims to support. Missing features belong in
`docs/taclet-ideas.md` instead. Each entry has a reproducer. **Delete an entry in the same change
that fixes it**, and add a regression example to `keyext.solidity.examples/TestSuite.sol`.
`docs/limitations.md` puts these in context; most were found by the second solc port and the
real-world ports, and their faithful forms sit in `keyext.solidity.examples/solc/open/` and
`real-world/open/`. "EVM" means `./run-key.sh FILE --solc`.

## Proves something false

Unbounded integers are a design choice: see Tier 5 in `docs/taclet-ideas.md`. They make
`a = 2**256-1; unchecked { c = a + 1; } assert(c > a);` close, and make
`uint16 r = (t ? 63 : 255) + (f ? 63 : 255);` (a `uint8` overflow, `Panic(0x11)`) close as
non-reverting.

- **Named arguments bind by position.** `SolJSONParser.parseFunctionCall` (and
  `SolidityToKeyConverter.visitFunctionCallArguments`) ignore the call's `names`:
  `function digits(uint p, uint q, uint s) internal pure returns (uint r) { r = p*100 + q*10 + s; }`
  then `uint r = digits({q: 2, s: 3, p: 1}); assert(r == 231);` closes; the EVM fails it (123).
  `solc/open/SolcFunctionCallsOpen.sol` `namedArgsUnordered`, `disorderedNamedArgs`.
- **Nested applications of one modifier share its body locals.** `ModifierInlining.enter`
  renames only the parameters:
  `modifier m(uint y) { uint c = y; _; x = c; } function h() internal m(2) m(5) {}` then
  `h(); assert(x == 5);` closes; the EVM ends with `x == 2`. `SolcModifiersOpen.modifierMultipleTimesLocalVars`.
- **A virtual call inside a base-contract function is bound to the base implementation.**
  `parseIdentifier` keeps solc's `referencedDeclaration`; nothing re-resolves the override:
  `contract B { function g() internal pure virtual returns (uint) { return 1; } function callsG() internal pure returns (uint) { return g(); } }`
  `contract D is B { function g() internal pure override returns (uint) { return 2; } function f() public pure { assert(callsG() == 1); } }`
  closes; the EVM fails it. `SolcHigherLevelOpen.internalVirtualFunctionCalls`.
- **Elementary type conversions are the identity.** `parseFunctionCall` returns the argument of
  a `typeConversion` unchanged (no truncation, no sign reinterpretation):
  `uint32 p = 0x12345678; uint16 q = uint16(p); assert(q == 0x12345678);` closes; the EVM fails
  it. `SolcTypesOpen` `castSmallerTruncates`, `downcastTruncates`, `sameSizeReinterprets`, ….
- **`try` and `call{value}` assume the callee leaves the caller's storage alone**, also when it
  is the contract itself. `tryCallNoCallbackBox` / `sendNoCallbackBox` follow the default
  `transferSemantics:noCallback`, whose gas-stipend argument holds for `transfer` only:
  `function setIx() external { ix = 42; }` and, in a box function with `p` the contract's own
  address, `ix = 0; try C(payable(p)).setIx() { assert(ix == 0); } catch (bytes memory) {}`
  closes; the EVM fails it. Same for `payable(p).call{value: 0}("")` into a `receive` that
  writes. `-O transferSemantics:withCallback` keeps both open.
  `SolcPaymentsOpen.trySuccessKeepsStorageUnsound`, `callKeepsStorageUnsound`.

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

- **An `address` named return starts unconstrained.** `ExpandFunctionBody.zero` initialises
  only `int` and `bool` returns, so after `function z() internal pure returns (address r) {}`,
  `address a = z(); assert(a == address(0));` leaves `==> r = 0` open; solc returns
  `address(0)`. Every value type should start at its default (solidity-lean already does).
- **A local declared without an initializer starts unconstrained.** `valueDeclSkip`
  (`solidityProgramRules.key`) drops `T v;` with `\addprogvars(v)` and no default:
  `uint x; assert(x == 0);` leaves `==> x = 0`; `bool b; assert(!b);` leaves `b = TRUE ==>`.
  `defaultValue` already exists (`memoryRules.key`). Workaround: `uint x = 0;`.
- **A variable mentioned only in a loop invariant loses its value.** `\dropEffectlessElementaries`
  (`DropEffectlessElementariesCondition.searchTerm`) does not see a loop's `LoopSpec` bindings, so
  `simplifyUpdate*` drops the update before `whileInvariantBox`:
  `uint i = 7; uint j = 0; /// @custom:key invariant j <= n && i == 7` `while (j < n) j = j + 1;`
  leaves `==> i = 7`. A parameter alike (`for (uint i = n; i < 100; i++)` with invariant
  `n <= i`). Workaround: mention the variable after the loop, or use a literal.
- **`sdiv`/`smod` on symbolic operands get no bounds.** The default `NON_LIN_ARITH_NONE`
  gives `defOps_div`/`defOps_sdiv` infinite cost and binds `defOps_mod` only for literals, and
  `smod_NumPos`/`smod_geZero` (`intDiv.key`) have no heuristics:
  `require(a >= 0 && b >= 1); assert(a % b < b);` leaves `geq(smod(a, b), b) ==>`;
  `assert(a / b >= 0)` and `(2*x) % 2 == 0` alike. The `.sol` path cannot pick another mode.
- **Parameters, `msg.value` and `msg.sender` carry no type range.** `function g(uint x) public
  pure { assert(x >= 0); }` leaves `leq(x, -1) ==>`; `uint8`, enum parameters and
  `assert(msg.value >= 0)` alike. A non-payable function's `msg.value == 0` is assumed only when
  the function has a `@custom:key` clause (`SolidityProblemSynthesizer`, unspecified branch).
  Known in Tier 3/5 of `docs/taclet-ideas.md`.
- **A static → dynamic storage array copy loses the length.** `uint[9] d1; uint[] d2;`
  `d2 = d1; assert(d2.length == 9);` stays open: `storageRootWriteCopySource` copies the raw
  value, whose `size` is unconstrained; the 9 lives only in `typed(fixedArr(..))`.
- **Copying a shorter static storage array into a longer one leaves the tail.**
  `uint[40] big; uint[20] small;` `big[30] = 4; big = small; assert(big[30] == 0);` leaves
  `==> selectSt<[int]>(selectSt<[Struct]>(storage, C$small), at(30)) = 0`.
- **A succedent `\exists` whose body has a nested quantifier is never instantiated.**
  `ex_pull_out0..3` (`formulaNormalizationRules.key`) lack `\heuristics(pullOutQuantifierEx)`,
  unlike Java KeY: `requires \forall uint i; i < vals.length -> vals[i] <= vals[k]`,
  `ensures \exists uint w; w < vals.length && res == vals[w] && (\forall uint i; … vals[i] <= vals[w])`
  with `res = vals[k]` stays open. `real-world/open/DocsBallot.sol` `winnerName`.
