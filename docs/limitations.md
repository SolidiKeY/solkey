# Why SolKey's Solidity is incomplete or buggy

What the second solc port (`keyext.solidity.examples/solc/`, `solc/open/`), the real-world ports
(`real-world/`, `real-world/open/`) and the benchmark (`benchmark/`) could not prove, and why.
Every claim below was reproduced with `./run-key.sh`, and every disputed fact was run on the EVM
with `./run-key.sh FILE --solc` (solc 0.8.34, in-process Besu). Reproducers and root causes for
the defects are in `docs/bugs.md`; the backlog items are in `docs/taclet-ideas.md`, "Raised by
the second solc port and the real-world ports".

Read "true fact" as: true under Solidity 0.8 semantics and confirmed on the EVM (or, where the
runtime check skips the contract, true by the language definition).

## Headline numbers

| Port | New files | Functions that close | Kept open (`open/`) | EVM |
|---|---|---|---|---|
| solc, second round (11 themes, `semanticTests/` + `smtCheckerTests/`) | 11 + 11 open | 482 | 137 | no closing function fails an `assert` |
| real-world (OpenZeppelin, solmate, Solidity docs, Solidity by Example) | 22 + 3 open | 75 | 6 | no failing `assert` |
| benchmark (published as published) | 6 added | 37/39 obligations | — | compiles, no asserts |

Defects found (all in `docs/bugs.md` until fixed): **5 that prove something false** (three
since fixed), 10 crash families
(one of them in the `--solc` harness; all since fixed), 1 stuck-on-program-text gap and 10 true
facts that cannot be proved (five since fixed). Of the 14 porting agents, 6 independently hit the `ClassCastException` on indexing a
parameter, and 10 hit a `NullPointerException` at the same line, `SolJSONParser.java:1079`.

## (c) Genuine bugs

### Proves something false

These close in KeY and fail with `Panic(0x01)` on the EVM. Each is a soundness bug.

| Bug | Minimal reproducer | Where found |
|---|---|---|
| *(fixed)* **Named arguments bind by position.** `parseFunctionCall` ignores the call's `names` | `digits({q: 2, s: 3, p: 1})` with `r = p*100 + q*10 + s` proves `r == 231` (true value 123) | `functionCall/named_args.sol`, `disordered_named_args.sol` |
| *(fixed)* **Nested applications of one modifier share its body locals.** `ModifierInlining` renames only parameters | `modifier m(uint y) { uint c = y; _; x = c; }` on `h() m(2) m(5)` proves `x == 5` (true: 2) | `modifiers/function_modifier_multiple_times_local_vars.sol` |
| *(fixed)* **A virtual call made inside a base-contract function is bound to the base implementation** | base `callsG() { return g(); }`, derived override `g()` returns 2; KeY proves `callsG() == 1` | `virtualFunctions/internal_virtual_function_calls.sol`, `virtual_function_calls.sol` |
| **Elementary type conversions are the identity** (`uint16(x)`, `int8(x)`, `uint(int(-1))`). `SolJSONParser` returns the argument unchanged | `uint32 p = 0x12345678; uint16 q = uint16(p); assert(q == 0x12345678);` closes | 7 `typecast/` and `types/` tests (downcast, same_size, upcast, packing_signed_types, …) |
| **`try` and `call{value}` assume the callee leaves the caller's storage alone** (default `transferSemantics:noCallback`), even when the callee is the contract itself; the gas-stipend argument holds for `transfer` only | `try C(p).setIx() { assert(ix == 0); }` with `p` the contract itself, `setIx` writing 42 | `try_catch/try_2.sol`, `receive/empty_calldata_calls_receive.sol` |

Two more proofs of false facts come from the documented design choice of unbounded integers
(below), not from a defect: an `unchecked` overflow (`a = 2**256-1; unchecked { c = a + 1; }
assert(c > a);`) and a checked overflow at a literal's `uint8` type
(`uint16 r = (t ? 63 : 255) + (f ? 63 : 255);`, which reverts with `Panic(0x11)`).

### Crashes

The ten crash families the ports found are fixed (`docs/taclets-implementation.md`,
"Declarations outside the contract"). A construct the parser still does not support (`this`,
`super`, `block.*`, `tx.*`, events) is refused at load with a `SolidityParseException` naming
it.

### Proof gets stuck on program text

- A compound assignment whose right side is a bare storage path (`pot += other;`, `m_a *= m_a;`,
  `proposals[p].voteCount += sender.weight;`) matches neither the terminal rules
  (`SimpleExpression`) nor the capture rules (`NonSimpleExpression[primitive]` excludes paths).
  A non-simple right side that reads storage (`map[p] += map[p] + x`) closes. Workaround: bind
  the read to a local.
- The same holds for a storage read as the mapping key of a compound assignment
  (`m[r] += 1` with `r` a state variable), which leaves `WithdrawalContract.becomeRichest` open
  and is bound to locals in `KingOfEther` and `SimpleAuction.bid`.

### True facts that cannot be proved

| Gap | Reproducer (open goal) | Reports |
|---|---|---|
| *(fixed)* A local declared without an initializer is unconstrained (`valueDeclSkip` binds no default) | `uint x; assert(x == 0);` → `==> x = 0`; `bool b;` and `address a;` alike | 4 (loops, constructors, payments, smt control flow); ≥ 10 upstream tests |
| A variable mentioned only in a loop invariant loses its value (`DropEffectlessElementaries` does not see `LoopSpec` bindings) | `uint i = 7; /// invariant j <= n && i == 7` → `==> i = 7` | 2; `dynamic_arrays_in_storage.sol`, `ArrayRemoveByShifting` |
| `sdiv`/`smod` on symbolic operands get no bounds (default arithmetic mode `NON_LIN_ARITH_NONE`; `smod_*` lemmas have no heuristics) | `require(b >= 1); assert(a % b < b);` → `geq(smod(a,b), b) ==>` | 2; `mod_n`, `mod_signed`, `mod_even`, OZ `Math.tryDiv/tryMod/ceilDiv` |
| Parameters carry no type range (`uint >= 0`, `uintN < 2^N`, enum `< #members`) | `function g(uint x) { assert(x >= 0); }` → `leq(x, -1) ==>` | 4; known (Tier 3/5 of `taclet-ideas.md`) |
| `msg.value`/`msg.sender` carry no range; the non-payable `msg.value == 0` is assumed only in specified obligations | `function u() payable { assert(msg.value >= 0); }` → `leq(msgValue, -1) ==>` | 3; `msg_value_*`, `range_check.sol`, `payable_1.sol` |
| *(fixed)* Static → dynamic storage array copy loses the length | `uint[9] d1; uint[] d2; d2 = d1; assert(d2.length == 9)` | `array_copy_storage_storage_static_dynamic.sol` |
| *(fixed)* A shorter static array copied into a longer one leaves the tail | `big[30] = 4; big = small; assert(big[30] == 0)` | `array_copy_storage_storage_static_static.sol` |
| A succedent `\exists` whose body has a nested quantifier is never instantiated (`ex_pull_out*` have no heuristics) | `ensures \exists w; … && (\forall i; … vals[i] <= vals[w])` with witness `k` | voting.rst `Ballot.winnerName` |

## (a) Design limitations of the model

Choices the calculus makes on purpose, each of which makes some Solidity behaviour diverge.

- **Unbounded mathematical integers.** No checked-overflow revert, no `unchecked` wrapping, no
  width. `SolcArithmeticOpen` keeps 7 wrap tests that hold on the EVM and stay open
  (`uint8 0 - 1 == 255`, `int8 127 + 1 == -128`, `int8 100 * 2 == -56`, `++max == 0`, …); the
  reverse direction proves false (above). Ports must `require` both bounds by hand; OZ
  `tryAdd`/`tryMul` and `Nonces`/`Counters` are specified only on their non-wrapping case.
- **No bitwise LDT.** `&`, `|`, `^`, `~`, `<<`, `>>` and their compound forms are stuck on the
  program text. Blocks OZ `Math.average`, `AccessRestriction.forceOwnerChange`, every
  `operators/shifts/` and `bitwise_*` test, and `exponent & 1` in `freeFunctions/recursion.sol`.
- **No `bytes`, `bytesN`, `string` values.** No `KeYSolidityType` for `bytes32`/`bytes`, string
  literals fail with "Not yet supported literal", `string` parameters get no obligation. With
  them go `keccak256`, `abi.encode*`, `msg.data`, `require(c, "msg")`.
- **Addresses are integers without an environment.** No `this`, no `<address>.balance`, no
  `block.*`/`tx.*`/`gasleft`, and contract-typed parameters get no obligation. Specs use a
  `timeNow` state variable for time.
- **Storage starts arbitrary.** Every obligation ranges over any reachable storage, so a fact
  that holds only from a fresh deployment needs a `require` under a box. In particular,
  `push()` of a reference-typed element (inner array, struct with array member) does not clear
  the slot, on purpose: a dangling reference can leave data there, and the EVM agrees
  (`r = a[0]; a.pop(); r.push(7); a.push(); assert(a[0].length == 0)` fails with
  `Panic(0x01)`; `docs/storage.md`). Blocks `push_no_args_2d`, `dynamic_multi_array_cleanup`,
  `struct_storage_push_zero_value` and 6 array ports in their faithful form.
- **Immutables are storage.** An immutable is read from storage, so outside the constructor
  it is unconstrained. Constants are inlined at their reads.
- **Every internal call is inlined.** Recursion unfolds without end (`functions_recursive.sol`
  with an unpinned counter), large callees are re-executed.
- **A `try` callee is never executed**, only havocked: its return value and its effects on the
  caller (`try_2`, `try_nested_1`, `simple_notuple`) are unknown in the success branch.
- **Inherited public functions get no obligation** of their own; only functions declared in the
  selected contract are proved.
- **Proof cost.** Two true facts close only far beyond the default budget: nested memory arrays
  built in nested loops (`binomial(5,3)` closes in 154 s at `-m 200000`, `binomial(9,5)` did not
  finish in 25 min) and repeated `push` into struct-array members inside a storage struct (13 774
  steps but ~65 s, over the suite's 30 s limit).
- **Runtime cross-check reach.** `SolidityRuntimeCheck` installs runtime bytecode without
  running creation code, so it skips every contract with a constructor or a non-constant
  state-variable initializer; constructor obligations are never EVM-checked. Enum and `bool`
  parameters cannot be pinned, so their functions are skipped.

## (b) Unsupported constructs, ranked

Deduplicated across all reports; counts are the named upstream files and published contracts
each one blocks (lower bounds: whole families are counted once where a report said "all").

| Rank | Construct | Error | Blocks |
|---|---|---|---|
| 1 | Events, `emit`, custom errors | `Unknown node type EventDefinition` / `ErrorDefinition`, `Statement does not have type EmitStatement` | all of `events/`, `errors/`; 12 published contracts (Coin, ERC20, SimpleAuction, Purchase, SendContract, WithdrawalContract, AccessRestriction, BlindAuction, Ownable, StateMachine, WETH9, modular Token) |
| 2 | `this`, `block.*`, `tx.*`, `<address>.balance` | refused at load ("The built-in this is not supported"); `Unresolved member access` | `blockchain_state/*`, `special/*`, `bmc_coverage/timestamp.sol`; 8 published contracts |
| 3 | String literals (`require`/`revert` messages, string arguments) | `Not yet supported literal` | `revertStrings/*`, `strings/*`; every Solidity by Example and OZ contract (messages dropped); 5 benchmark contracts |
| 4 | Struct constructors `S(..)`, `S({..})` | `Unexpected reference declaration S expected a state variable.` | 17 tests (`simple_struct_allocation`, `struct_named_constructor`, `struct_temporary`, …); Todos, Ballot, BlindAuction |
| 5 | `bytes32`/`bytes`, `keccak256`, `abi.*` | `No KeYSolidityType for bytes32` | `crypto/*`, `abi/*`, `abicoder/*`; Ballot, BlindAuction, Ownable |
| 6 | Push forms: `x = a.push()`, `a.push(memArr)`, `a.push(memStruct)`, `a.push() -= 1`, `++a.push()`, `f().push()` | stuck | 11 tests (`push_no_args_1d`, `push_as_lhs_*`, `array_push_nested_from_memory`, `array_push_struct`, …) |
| 7 | An assignment or compound assignment used as a value: `a = (b = c)`, `a += b += c`, `a[0] = a[1] = 1`, `(m = m2)[2] = 21`, modifier argument `m(r = 2)` | stuck | 10 tests (`functions_storage_var_*`, `compound_add_chain`, `short_circuit_*_touched`, `multiple_initializations`, …) |
| 8 | Function types | `Type FunctionTypeName not covered` | 10 tests (`functionTypes/*`, `*_via_pointer`, `store_function_in_constructor*`, …) |
| 9 | Modifiers with `return` or two `_;` | not inlined, the obligation stays open | 8 tests (`return_in_modifier`, `stacked_return_with_modifiers`, `function_modifier_multi_invocation`, `modifier_two_placeholders`, …) |
| 10 | Inline array literals `[uint(1), 2, 3]` | `Not yet supported expression type` | 7 tests; Solidity by Example array contracts (rewritten with `push`) |
| 11 | Contract creation `new D()` | stuck; multi-contract file needs `--contract` | `deployment/*`, `new_operator.sol`, `multi_creation.sol` |
| 12 | Inline assembly | `Statement does not have type InlineAssembly` | `inlineAssembly/*` |
| 13 | Nested or parenthesized tuple targets `(((a, ), )) = …`, `((a, b)) = …` | load error / stuck | 5 tests |
| 14 | Integer → enum conversion `E(x)` | stuck | 4 tests; docs `StateMachine.nextStage` |
| 15 | `super.f()`, base-qualified `Base.f()` (lowered to `address.f()`) | refused at load / stuck | 9 tests (`super_overload`, `diamond_super_*`, `explicit_base_class`, `inherited_function`, …) |
| 16 | `for` loop in a modifier body (not lowered; `while` is) | stuck | 3 tests (`break_in_modifier`, `continue_in_modifier`, `function_modifier_loop`) |
| 17 | `new T[](e)` with a non-simple length; `storageArr = new T[](n)` | stuck | 4 tests |
| 18 | External call statement outside `try`; `try` argument with a side effect; `c.f{value: v}()`; `(bool, bytes memory) = a.call(..)` | stuck / load error | 5 tests; SbE `DenialOfService` attack |
| 19 | Constant (expression) as a fixed array length `uint[LEN]` | `Array length LEN is not supported` | 2 tests |
| 20 | `delete` on a local | stuck | 2 tests (`delete_local`, `delete_locals`) |
| 21 | Effect-free expression statements `a + b;`, `arr.pop;`, `S[7][];`; a conditional callee `(c ? g : h)(..)` | stuck / load error | 4 tests |
| 22 | Imports; pragma `^0.4` | `Source not found`; compiler version | ERC20 (inlined), Ownable; WETH9 |

## (d) Spec-language gaps

- **Loop invariants cannot name the lowered `break` flag**, so a fact established on the break
  exit is lost: `for_1_break`, `while_1_break`, `while_nested_break`, `while_nested_continue`
  stay open (the anonymised state pairs `brk = TRUE` with any invariant-allowed `x`).
- **Loop invariants cannot use `\old`** (`SpecException: \old is only allowed in ensures`), and
  the loop rule anonymises all of storage, so the full spec of `ArrayRemoveByShifting.remove`
  (length − 1, prefix kept, suffix shifted) stays open.
- **Nested `\old` is rejected** (`\old(m[\old(k)])`); `\old(m[k])` evaluates `k` in the old
  state anyway.
- **Spec `/` and `%` are floor `div`/`mod`; program `/` and `%` are truncating `sdiv`/`smod`.**
  `ensures r == a / b` cannot state Solidity's signed division.
- **No ether/time units and no `block`** in clauses (`1 ether` → parse error); write the
  literal. Named constants work.
- **A function returning an unnamed value or a tuple gets no obligation**, silently; ports add
  a named return (`returns (bool success)`).
- **Library struct members are not visible** in clauses (`struct Counter has no member _value`).

## (e) The port, theme by theme

solc themes (closing file / `open/` file). "Not ported" counts the upstream groups each report
listed as outside the fragment or duplicated.

| Theme | File | Closed | Open | Not-ported groups | Main reasons for open |
|---|---|---|---|---|---|
| Loops | `SolcLoops` | 49 | 10 | 6 | break flag in invariants, pushed inner array not empty, `new T[](n+1)`, cost |
| Array members | `SolcArrayMembers` | 61 | 19 | 10 | push forms, pushed inner array not empty, static copies |
| Function calls | `SolcFunctionCalls` | 29 | 7 | 11 | named args (proves false), `&`, assignment as value, conditional callee |
| Structs and mappings | `SolcStructsMappings` | 50 | 5 | 9 | pushed struct not zero, cost |
| Constructors | `SolcConstructors` | 35 | 11 (1 closes) | 11 | uninitialised locals, `delete v`, `msg.value` |
| Payments, reverts, try | `SolcPayments` | 35 | 15 (2 close: proves-false witnesses) | 11 | `msg.*` ranges, `try` callee havocked, noCallback unsound |
| Modifiers | `SolcModifiers` | 34 | 13 | 9 | `return`/two `_;`, `for` in modifier, shared locals (proves false) |
| smtChecker control flow | `SolcSmtControlFlow` | 85 | 13 | 11 | uninitialised locals, `msg.value`, modifier `return` |
| Arithmetic | `SolcArithmetic` | 46 | 20 | 10 | wrapping, shifts, `smod` lemmas |
| Inheritance, getters | `SolcHigherLevel` | 19 | 7 | 13 | virtual dispatch (proves false), `Base.f()` |
| Types, enums, tuples, literals | `SolcTypes` | 39 | 17 (1 closes: proves false) | 11 | identity casts (proves false), `E(x)`, enum ranges |
| **Total** | 11 files | **482** | **137** | | |

Real-world and benchmark:

| Source | Files | Closed | Open |
|---|---|---|---|
| OpenZeppelin v5.0.0/v4.9.0, solmate v7, docs `voting.rst` | `OZPausable`, `OZReentrancyGuard`, `OZNonces`, `OZOwnable2Step`, `OZMath`, `OZCounters`, `SolmateOwned`, `DocsBallot` | 36 | `open/OZMath.sol` (`tryDiv`, `tryMod`, `average`, `ceilDiv`), `open/DocsBallot.sol` (`winnerName`, strong spec) |
| Solidity by Example | `ArrayReplaceFromEnd`, `Enum`, `FunctionModifier`, `ArrayRemoveByShifting`, `Counter`, `Mapping`, `NestedMapping`, `KingOfEther`, `EtherGame`, `EtherStore`, `Account`, `IterableMapping`, `SendEther`, `TimeLock` | 39 | `open/ArrayRemoveByShifting.sol` (`remove`, full spec); `EtherStore.withdraw` under `-O transferSemantics:withCallback` (the re-entrancy, as intended) |
| benchmark | 14 files (6 new) | 37/39 | `WithdrawalContract.becomeRichest` (`m[storageKey] += e`), `AccessRestriction.forceOwnerChange` (`&`) |

Not ported as real-world contracts: Todos (struct literal, `string` parameters), Vault and
CrowdFund (external IERC20 calls), MultiSigWallet (`bytes` calls), Payable/ReceiveEther
(`address(this).balance`), OZ `Math.mulDiv/sqrt/log*` (assembly, bitwise), solmate `Auth`
(external authority), BlindAuction, Ownable v5, StateMachine, modular Token, the
`DenialOfService` attack (see the benchmark README).
