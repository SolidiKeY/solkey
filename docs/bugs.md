# Known Bugs

Open defects in constructs solkey already claims to support. Missing features belong in
`docs/taclet-ideas.md` instead. Each entry has a reproducer. **Delete an entry in the same change
that fixes it**, and add a regression example to the matching `keyext.solidity.examples/taclets/` file.
`docs/limitations.md` puts these in context; most were found by the second solc port and the
real-world ports, and their faithful forms sit in `keyext.solidity.examples/semantics/open/` and
`contracts/real-world/open/`. "EVM" means `./run-key.sh FILE --solc`.
`docs/semantics-bugs-fix-ideas.md` lists the fix ideas and the plan for the semantics bugs below.

## Proves something false

Unbounded integers are a design choice: see Tier 5 in `docs/taclet-ideas.md`. They make
`a = 2**256-1; unchecked { c = a + 1; } assert(c > a);` close, and make
`uint16 r = (t ? 63 : 255) + (f ? 63 : 255);` (a `uint8` overflow, `Panic(0x11)`) close as
non-reverting. Not planned now (fix ideas 6A–6C).

- **Elementary type conversions are the identity.** `parseFunctionCall` returns the argument of
  a `typeConversion` unchanged (no truncation, no sign reinterpretation):
  `uint32 p = 0x12345678; uint16 q = uint16(p); assert(q == 0x12345678);` closes; the EVM fails
  it. `SolcTypes` `castSmallerTruncates`, `downcastTruncates`, `sameSizeReinterprets`, ….
  Not planned now (fix idea 4A).
- **`try` and `call{value}` assume the callee leaves the caller's storage alone**, also when it
  is the contract itself. `tryCallNoCallbackBox` / `sendNoCallbackBox` follow the default
  `transferSemantics:noCallback`, whose gas-stipend argument holds for `transfer` only:
  `function setIx() external { ix = 42; }` and, in a box function with `p` the contract's own
  address, `ix = 0; try C(payable(p)).setIx() { assert(ix == 0); } catch (bytes memory) {}`
  closes; the EVM fails it. Same for `payable(p).call{value: 0}("")` into a `receive` that
  writes. `-O transferSemantics:withCallback` keeps both open.
  `SolcPayments.trySuccessKeepsStorageUnsound`, `callKeepsStorageUnsound`.
  Planned later: 5A (split the option) + 5D (view/pure frame).
- **A derived constructor runs neither base initializers nor base constructors.**
  `ExpandFunctionBody` prepends only the initializers of the contract that declares the
  constructor and never walks `linearizedBaseContracts`, so inherited state starts at zero:
  `contract P { uint x = 3; } contract C is P { uint y; constructor() { assert(x == 0); y = 1; } }`
  closes for `--contract C`; on the EVM `x == 3` there (upstream `base_constructor_arguments.sol`).
  Base-constructor arguments (`is P(7)`) are dropped the same way, and a base-constructor call in
  the constructor header (`constructor() P(9)`) is stuck as `constructor()@C;`.
- **Virtual modifiers are bound statically.** `ModifierInlining` uses solc's
  `referencedDeclaration`, so a function declared in a base contract runs the base modifier, not
  the derived override:
  `contract HB { uint y; modifier vm() virtual { y = 100; _; } function baseUsesVm() internal vm {} }`
  `contract H is HB { modifier vm() override { y = 200; _; } function wrong() public { y = 0; baseUsesVm(); assert(y == 100); } }`
  closes `wrong`; the EVM fails it. An abstract virtual modifier used in the base
  (`modifier guard() virtual;`) leaves the call stuck.
- **The loop-invariant frame walks the static declaration of a virtual call.** `LoopFrame.call`
  follows `getReferencedDeclaration()`, not the override the call dispatches to, so an override
  that writes a memory argument is missed and the invariant rule keeps memory:
  `contract Base { function h(uint[] memory a) internal pure virtual {}` with a box `wrapper(n)`
  running `/// @custom:key invariant i <= n` `while (i < n) { h(arr); i++; }` then
  `assert(arr[0] == 0)`, and `contract Derived is Base { function h(uint[] memory a) internal pure override { a[0] = 9; }`
  `/// @custom:key box function callsWrapper(uint n) public pure { require(n == 1); wrapper(n); } }`:
  `callsWrapper` closes for `-c Derived`; the EVM fails the assert.
- **`MetaPow` truncates the exponent to 32 bits.** `MetaPow.transform` calls
  `bigIntArg1.pow(bigIntArg2.intValue())`, so `pow_literals` rewrites `0 ** 4294967296` to
  `0 ** 0 == 1`: `uint e = 4294967296; uint r = 0 ** e; assert(r == 1);` closes; the EVM fails
  it. Fix: `intValueExact()` inside the existing `try`, leaving `pow` symbolic.
- **`new T[](n)` never panics with `Panic(0x41)`.** `memoryArrayFreshAlloc` allocates for any
  length; the EVM panics when `n > 2**64 - 1`:
  `uint n = 2**64; uint[] memory a = new uint[](n); assert(a.length == n);` closes, also under
  box; the EVM gives `Panic(0x41)`. Port `semanticTests/array/create_memory_array_too_large.sol`
  with the fix.
- **Tuple assignment snapshots storage `string`/`bytes` values.** `bytes` and `string` are not
  reference types in `MemoryReferenceTypes.isReferenceType`, so `TupleAssignment` gives a storage
  string a snapshot temporary instead of a storage alias: `(s1, s2) = (s2, s1)` on storage strings
  proves a swap, while solc copies at store time, right to left, as it does for structs and
  arrays (no swap). Not yet checked on the EVM.
- **The overlap walk trusts the prover's integers.** `srcWalk` and `inside` compare mapping
  keys as KeY integers, so the unbounded integers and the identity conversions above let an overlapping
  recursive copy through. With `struct Node { uint v; Node[] kids; }`,
  `mapping(uint => Node) byId` and `byId[0] = {kids: [S]}`, `S.kids = [A, B]`, `B.v = 5`:
  `uint big = 2**256 - 1; uint k; unchecked { k = big + 1; }`
  `byId[k] = byId[0].kids[0]; assert(byId[k].kids[1].v == 5);` closes, since the prover takes
  `k = 2**256` and finds the paths apart; on the EVM `k == 0`, the copy runs in place and the
  assert fails. Same with `mapping(uint8 => Node)` and `uint wide = 256; uint8 k = uint8(wide);`.
  Fixed by the integer fixes (6A–6C, 4A), not by the copy rules.
- **A cyclic memory struct is copied to storage in finite time.** `memoryToStorageStoreRoot`
  writes the lazy `copyMem(mtSt, memory, mv)` and never asks whether the memory value is finite;
  the overlap walk covers storage sources only. With the same `Node` and `Node nodeTree`:
  `Node memory m; m.v = 5; m.kids = new Node[](1); m.kids[0] = m; nodeTree = m;`
  `assert(nodeTree.v == 5);` closes under the diamond; solc's copy recurses until the EVM halts
  with `TOO_MANY_STACK_ITEMS` (without `m.kids[0] = m` it succeeds). Only the IR pipeline
  compiles the copy (`solc --via-ir --bin-runtime`, run through `EvmContractRunner`); `--solc`
  rejects it with `UnimplementedFeatureError`, so the runtime cross-check never sees it.
- **Public functions without an obligation are not reported while the contract has an
  invariant.** A public/external function whose return value is unnamed or has no `.key` sort,
  or that takes a parameter without one (`fallback(bytes calldata) external returns (bytes memory)`),
  gets no obligation and the whole-file run never mentions it, so the invariant is reported
  proved although that entry point can break it:
  `/// @custom:key invariant x == 0` `contract U { uint x; function f() public returns (uint) { x = 1; return 0; } function t() public { assert(x == 0); } }`
  prints `PASS constructor`, `PASS t`, `2/2 closed`. Fix: an obligation that binds no result, or
  a FAIL/UNVERIFIED line when the contract has an invariant. Without an invariant the skip still
  hides the function's own `assert`s: `struct S { uint x; }` `function f(S calldata s) public pure { assert(s.x == 1); }`
  next to `function t() public pure { assert(true); }` prints `PASS t`, `1/1 closed`; only
  `-f f` reports "parameter s has unsupported type struct". `SolidityProblemSynthesizer.provableFunctions`
  and the `--solc` runtime check skip it the same way, so no test sees such a port
  (`semantics/open/SolcStructsMappings.sol`).
- **`@custom:key` clauses outside contract members are ignored.** A clause above a non-loop
  statement (`/// @custom:key ensures x == 0` before `x = 1;`), on a file-level free function,
  or inside a library or interface is dropped without an error, and the run reports PASS.

## Crashes at load or during the proof

A construct the parser does not support (`this`, `super`, `block.*`, `tx.*`, an event, a
user-defined operator) is refused at load with a `SolidityParseException` naming it; those are in
`docs/taclet-ideas.md`.

- **State variables whose initializers read each other.** `uint a = b + 1; uint b = a + 2;` is
  refused with "The initializer of a depends on itself". solc accepts it and the EVM initializes
  in declaration order (`a == 1`, `b == 3`). The loader parses the forward field's initializer
  only to register its declaration, so its cycle guard fires on a legal cycle. Reproducer:
  `contract V { uint a = b + 1; uint b = a + 2; function f() public view { assert(a == 1 && b == 3); } }`.
  A fix registers each field before parsing its initializer, which needs a mutable initializer
  on `StateVariableDeclaration` (`program/ast/`).
- **Same-arity, same-kind overloads.** The obligation's `f(a)@C` picks the overload by arity and
  argument kind (integer/address, bool), and obligation parameters are all `int`, so
  `function f(uint8 a) public {}` next to `function f(uint256 a) public {}` (or `uint` and
  `address`) fail at load with "Ambiguous call of the overloaded function f". Each still gets its
  own obligation, `f(uint8)` and `f(uint256)`.
- **A `return` of a conditional with tuple branches.** `function g(uint c) internal pure returns (uint, uint) { return c == 0 ? (1, 2) : (3, 4); }`
  called as `(uint a, uint b) = g(0);` fails during the proof with `IllegalStateException`
  "return of 1 values from a function with 2 return values" (`ReturnLowering.assignments`): the
  conditional counts as one value, not a 2-tuple. `semantics/open/SolcControlFlow.sol`
  `conditionalExpressionTuples`.

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
- **A conditional expression statement with assignment branches.** `c == 0 ? a = 7 : b = 7;`
  (`a`, `b` state variables) has no rule: `--why` reports "no rule executes the active
  statement". `semantics/open/SolcControlFlow.sol` `conditionalExpressionAssigns`.
- **A local of contract type.** `C c = C(address(0)); assert(address(c) == address(0));` loads
  but leaves `c = address(0);` as program text: the contract conversion is not modelled.

## True facts that cannot be proved

- **A variable mentioned only in a loop invariant loses its value.** `\dropEffectlessElementaries`
  (`DropEffectlessElementariesCondition.searchTerm`) does not see a loop's `LoopSpec` bindings, so
  `simplifyUpdate*` drops the update before `whileInvariantBox`:
  `uint i = 7; uint j = 0; /// @custom:key invariant j <= n && i == 7` `while (j < n) j = j + 1;`
  leaves `==> i = 7`. A parameter alike (`for (uint i = n; i < 100; i++)` with invariant
  `n <= i`). Workaround: mention the variable after the loop, or use a literal.
- **A loop that descends a recursive struct has no termination measure.**
  `/// @custom:key invariant true` `while (nodeTree.kids.length > 0) nodeTree = nodeTree.kids[0];`
  `assert(nodeTree.kids.length == 0);` closes under box and stays open under diamond: each
  iteration shortens the `kids[0]` chain, but no term denotes its depth, so `decreases` cannot
  state it. Fix: a depth function over a storage path, defined by the finite-tree assumption of
  `docs/storage.md`, "Overlapping copies".
- **Symbolic `sdiv` bounds remain incomplete.** The default `NON_LIN_ARITH_NONE`
  gives `defOps_div`/`defOps_sdiv` infinite cost. For a non-negative dividend,
  `sdiv`/`smod` now equal `div`/`mod` and positive-divisor quotient bounds automate;
  a negative dividend or divisor still gets no quotient bound.
- **Stored values and `msg.value` have no range facts.** Parameters, `msg.sender` (in
  specified obligations) and dynamic lengths (`conformsDynSize`: `0 <= |st| <= 2^256-1`) are in
  their type's range, and `\forall T q; P` compiles to `\forall int q; (lo <= q & q <= hi) -> P`.
  A `uint` or `address` read from storage, and `msg.value` in an unspecified function, are not
  bounded, so an assumed `\forall address a; ...` cannot be instantiated at a stored address
  unless something else bounds it. Reproducer: invariant `\forall uint k; k < 10 -> m[k] == 0`
  with `require(x == stored); m[x] = 0;` (`stored` a `uint` state variable) needing the
  invariant at `stored`. Fix ideas 10B.
- **Spec constants that `SolidityConstants.valueOf` cannot fold are unconstrained.** Truncating
  or sign-changing conversions (`uint8(uint(300))`, `uint(int(-1))`), `<<`, `>>`, `&`, `|`, `^` on
  typed operands, enum values and `keccak256` fall back to an unconstrained storage read:
  `uint8 constant C = uint8(uint(300));` with `/// @custom:key ensures C == 44` stays open.
- **A quantified spec variable named `size` or `nil` captures a function symbol.**
  `\forall uint size; size < a.length -> a[size] == 0` compiles the length read to
  `select(storage, cons(a, cons(size, nil)))` with the bound `size`, so the invariant no longer
  talks about the length. No false claim was found to close. Fix: add these names to
  `SpecCompiler.RESERVED`.
- **`pow` with a symbolic operand has no rules.** Only `pow_literals` exists (upstream
  `intPow.key` was not ported), so `x ** 0 == 1`, `x ** 1 == x`, `x ** 2 == x * x`,
  `1 << n > 0` (`require(n < 8)`) and `x >> n <= x` all stay open on `pow(x, 0)`, `pow(2, n)`, ….
- **A storage-array index target shrunk by the same tuple.** In
  `uint[] a; function f() public { a.push(); uint[] memory e; (a[0], a) = (5, e); }` solc
  bounds-checks `a[0]` against the old length and then writes a stale slot past the new length;
  SolKey also checks at store time, against the new length, and reports `Panic(0x32)`.
