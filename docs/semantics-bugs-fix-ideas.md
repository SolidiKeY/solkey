# SolKey semantics bugs: fix ideas

Oct 7, 2026 · @Someone

## Scope

This covers 12 semantics bugs: places where SolKey's model of a construct disagrees with the EVM. Six prove something false (unsound); six leave a true fact unprovable because the model forgets a value Solidity guarantees.

All come from `docs/bugs.md` (commit c6b8fc2, "added more tests and found more bugs") plus the unbounded-integer design choice it opens with. Each bug lists two to four fix ideas, labelled A, B, C, and listed in my order of preference, best first. The letters are stable labels, so they no longer run in order.

Left out on purpose: crashes, proofs stuck on program text, strategy gaps (`sdiv` heuristics, `ex_pull_out`, `\dropEffectlessElementaries`, the mapping-reference `cast` chain) and the two `--solc` runner bugs. Those are tool defects, not wrong semantics.

## Plan

Six bugs are for now (1, 2, 3, 8, 11, 12), two are for later (5, 7), three are not planned (4, 6, 10), and bug 9 is already fixed. The Plan column is the idea to implement.

| # | Bug | Status | Plan |
| --- | --- | --- | --- |
| 1 | Named arguments bind by position | Fixed | 1B |
| 2 | Modifier locals shared | Fixed | 2B |
| 3 | Virtual call binds to base | Fixed | 3A |
| 4 | Type conversions are the identity | Not now | Not now |
| 5 | `try` / `call{value}` keep storage | Later | 5A + 5D |
| 6 | Unbounded integers | Not now | Not now |
| 7 | `address` named return unconstrained | Later | 7B (small once 8C lands) |
| 8 | Uninitialised local unconstrained | Fixed | 8C |
| 9 | `constant` reads as storage | Fixed | Fixed in a094a3a |
| 10 | No type range on parameters, `msg.value` | Not now | Not now |
| 11 | Static → dynamic copy loses length | Fixed | 11B |
| 12 | Shorter static copy leaves the tail | Fixed | 12D (with 11B) |

## Bugs that prove something false

These six close proofs the EVM refutes, so they matter most. Bugs 1 and 2 are small parser fixes; 5 and 6 are design decisions.

### 1. Named arguments bind by position

`digits({q: 2, s: 3, p: 1})` binds `p := 2`. `SolJSONParser.parseFunctionCall` and `SolidityToKeyConverter.visitFunctionCallArguments` ignore the call's `names`.

```solidity
function digits(uint p, uint q, uint s) internal pure returns (uint r) { r = p*100 + q*10 + s; }
function f() public pure {
    uint r = digits({q: 2, s: 3, p: 1});
    assert(r == 231); // SolKey: closes. EVM: fails, r == 123
}
```

- **1B. One shared helper for both parsers.** A `NamedArguments.reorder(params, names, args)` used by the JSON and the ANTLR path, so the two cannot drift. Same cost as 1A plus a unit test.
- **1A. Reorder at parse time.** Look up the callee's parameter list through `referencedDeclaration` and permute the arguments to match `names`. About 20 lines; fixes the bug outright.
- **1C. Reject until fixed.** Throw `SolidityParseException` when `names` is not already in parameter order. Five lines, sound today, but drops real contracts.
- **Open question:** if an argument has side effects, does solc evaluate in written or parameter order? Hoist side-effecting arguments into temporaries in written order, then check with `--solc`.

### 2. Nested applications of one modifier share body locals

`h() m(2) m(5)` ends with `x == 5`; the EVM gives 2. `ModifierInlining.enter` renames the parameters only, so both copies of `uint c` are one program variable.

```solidity
uint x;
modifier m(uint y) { uint c = y; _; x = c; }
function h() internal m(2) m(5) {}
function f() public {
    h();
    assert(x == 5); // SolKey: closes. EVM: fails, x == 2
}
```

- **2B. One alpha-renaming pass for all inlining.** Factor `ExpandFunctionBody.declareFresh` and the modifier renaming into one visitor that freshens every declaration of an inlined body. Also protects nested and repeated internal calls; solidity-lean already does this (`renameStmts`).
- **2A. Rename every local declared in the modifier body.** Collect the body's `StatementVariableDeclaration`s into the existing `fresh` map before `ProgVarReplaceVisitor` runs. Smallest change.
- **2C. Desugar a modifier into two internal functions** (before and after `_`). Reuses call inlining, but locals that span `_` must be passed across, so it is the largest of the three.

### 3. A virtual call inside a base function binds to the base

`B.callsG()` calls `B.g` even when `D` overrides `g`. `parseIdentifier` keeps solc's `referencedDeclaration`, which names the statically visible declaration.

```solidity
contract B {
    function g() internal pure virtual returns (uint) { return 1; }
    function callsG() internal pure returns (uint) { return g(); }
}
contract D is B {
    function g() internal pure override returns (uint) { return 2; }
    function f() public pure {
        assert(callsG() == 1); // SolKey: closes. EVM: fails, callsG() == 2
    }
}
```

- **3A. Re-resolve against the most-derived contract at parse time.** Build an override map from `linearizedBaseContracts` and each function's `baseFunctions`, and rewrite references to `virtual` functions when parsing the selected contract. Parser-only; `super.g()` uses the same map (next in linearization).
- **3B. Late binding at inlining time.** Emit a `VirtualFunctionReference(name, signature)` and let `ExpandFunctionBody` pick the implementation from the obligation's contract. One AST shared by all derived contracts, but touches the rule layer.
- **3C. Refuse for now.** Fail the load when an internal call targets a function overridden in the selected contract. Sound, blocks OpenZeppelin-style hooks.

### 4. Elementary type conversions are the identity

`uint16(0x12345678)` stays `0x12345678`. `parseFunctionCall` returns the argument of every `typeConversion`.

```solidity
function f() public pure {
    uint32 p = 0x12345678;
    uint16 q = uint16(p);
    assert(q == 0x12345678); // SolKey: closes. EVM: fails, q == 0x5678
}
```

- **4A. A conversion function in the logic.** Emit a `TypeConversion(T, e)` node; taclets rewrite it to `wrapU(N, x) = mod(x, 2^N)` or `wrapS(N, x)` for signed. Widening stays the identity by a range lemma. Exact; adds an LDT function and a few rules.
- **4B. Sound but incomplete: havoc on narrowing.** Widening stays the identity. Narrowing yields a fresh `r` with `inRange(r, T)` and `inRange(x, T) -> r = x`. No modular arithmetic in proofs, but truncating examples stay open.
- **4D. Reject narrowing and sign-changing conversions at load.** Interim guard only.
- **4C. Fold into the integer redesign (bug 6).** If arithmetic becomes width-aware, a conversion is the same wrap operator. Cheapest if 6 goes that way, but 6 is deferred.

### 5. `try` and `call{value}` assume the callee keeps the caller's storage

A self-call that writes `ix` still proves `ix == 0`. `tryCallNoCallbackBox` and `sendNoCallbackBox` follow `transferSemantics:noCallback`, whose 2300-gas argument holds only for `transfer`.

```solidity
contract C {
    uint ix;
    function setIx() external { ix = 42; }
    function f(address p) public { // called with p == address of this contract
        ix = 0;
        try C(payable(p)).setIx() {
            assert(ix == 0); // SolKey: closes. EVM: fails, ix == 42
        } catch (bytes memory) {}
    }
}
```

- **5A. Split the option.** `transferSemantics` keeps governing `transfer`/`send`; `try` and `call{value}` always use the `withCallback` shape (havoc `storage`/`net`, assume `CInv`). Principled; specs without an invariant lose the success branch.
- **5D. Mutability frame.** A `view`/`pure` callee keeps storage even with callbacks (a static call cannot write). Complements 5A and wins back precision.
- **5C. Run self-calls.** When the target is provably `self` and the callee is known, inline the external body with `msg.sender := self`. Precise for `this.f()`, more work in `ExpandFunctionBody`.
- **5B. Case-split on the target.** Under `noCallback`, add a branch `addr = self` that havocs storage, keep the frame when `addr != self`. Fixes self-calls only and leaves "no reentrancy" as a stated assumption.
- solidity-lean has the same gap: its `noCallback` `try` never runs the callee either.

### 6. Integers are unbounded mathematical integers

Deferred: overflow is out of scope for now. Listed as a design choice, but it proves `unchecked { c = a + 1; } assert(c > a)` at `2**256-1` and misses `Panic(0x11)`.

```solidity
function f() public pure {
    uint a = 2**256 - 1;
    uint c;
    unchecked { c = a + 1; }
    assert(c > a); // SolKey: closes. EVM: fails, c == 0
}
```

- **6B. A taclet option, as KeY Java does.** `intSemantics:mathematical | checkedOverflow | evm`, mirroring Java KeY's `intRules`. Users choose speed or faithfulness; the default could stay `mathematical` with a warning, or become `checkedOverflow`.
- **6C. Overflow as revert only, no wrapping.** Checked operators assert the range; `unchecked` blocks are rejected. Covers solc ≥ 0.8 code without modular arithmetic, and is a stepping stone to 6A.
- **6A. Checked by default, wrapping in `unchecked`.** Each operator at type T gets a range side goal, else revert; inside `unchecked` it is `mod 2^N`. Matches the EVM and solidity-lean (`BinOp.retTy`). Highest proof cost: range facts everywhere.

## True facts the model loses

These six are sound but leave EVM-true facts open. Bugs 7, 8 and 10 share one need with bug 4: a per-type table of default value and range.

### 7. An `address` named return starts unconstrained

`ExpandFunctionBody.zero` covers only `INTEGER` and `BOOLEAN`, so `returns (address r) {}` leaves `r` free.

```solidity
function z() internal pure returns (address r) {}
function f() public pure {
    address a = z();
    assert(a == address(0)); // SolKey: open (r = 0 unproved). EVM: holds
}
```

- **7B. One `defaultValue(KeYSolidityType)` in the AST**, shared with bug 8 and memory `delete`. Named returns, uninitialised locals and deleted slots then agree by construction.
- **7A. Complete `zero()`.** Add `ADDRESS`, enum and every other value kind. One switch; fixes this bug only.

### 8. A local declared without an initializer starts unconstrained

`valueDeclSkip` drops `T v;` with `\addprogvars(v)` and no value, so `uint x; assert(x == 0)` stays open.

```solidity
function f() public pure {
    uint x;
    assert(x == 0); // SolKey: open. EVM: holds
    bool b;
    assert(!b);     // SolKey: open. EVM: holds
}
```

- **8C. Both, via 7B.** The parser emits a `DefaultValue(T)` expression, a single taclet evaluates it. Recommended if 7B is picked.
- **8A. Desugar in the parser.** `T v;` becomes `T v = <default of T>;`; `valueDeclSkip` is then never reached for value types. This is what solidity-lean's elaborator does.
- **8B. Fix the taclet.** `valueDeclSkip` becomes `{v := defaultValue<[sort]>}`, reusing `defaultValueInt` / `defaultValueBool` from `memoryRules.key`. Keeps the AST faithful to the source; needs a default rule per sort.

### 9. A `constant` state variable reads as storage

Fixed in a094a3a ("fixed crashing parser"): `SolJSONParser` now inlines a constant's initializer at each read, which is 9A, and the entry is gone from `docs/bugs.md`. The ideas below are kept for reference; 9C (immutables) is still open.

```solidity
uint constant X = 56;
function f() public pure {
    uint r = X;
    assert(r == 56); // SolKey: open, X read from storage. EVM: holds
}
```

- **9A. Inline the initializer at each read** (the work in progress). Simple; repeats the expression and needs it side-effect free, which solc guarantees for `constant`.
- **9B. A rigid logic constant.** Emit `C$X` as a function symbol with an axiom taclet `C$X = 56`. Specs can name it, and big initializers are evaluated once.
- **9C. Immutables, separately.** Keep them in storage but add `selectSt(storage, C$X) = C$X@deploy` to every non-constructor obligation, and reject writes outside the constructor.

### 10. Parameters, `msg.value` and `msg.sender` carry no type range

`function g(uint x) public pure { assert(x >= 0); }` stays open. `SolidityProblemSynthesizer` adds `msg.value == 0` only when the function has a `@custom:key` clause.

```solidity
enum Choice { A, B, C }
function g(uint x) public pure { assert(x >= 0); }               // SolKey: open. EVM: holds
function h(uint8 y) public pure { assert(y <= 255); }            // SolKey: open. EVM: holds
function k(Choice c) public pure { assert(uint(c) < 3); }        // SolKey: open. EVM: holds
function v() public payable { assert(msg.value >= 0); }          // SolKey: open. EVM: holds
```

- **10A. Range preconditions in the synthesizer.** For each parameter add `inRange(x, T)` (uint8 ≤ 255, enum < member count, address < 2^160); always add `msgValue >= 0` and a non-payable `msgValue = 0`. Small, local change.
- **10C. Only fix `msg.value` now.** Move the non-payable `msgValue = 0` out of the specified-only branch. A one-line quick win to take regardless.
- **10B. A general `inType<[T]>(v)` predicate.** One predicate from `KeYSolidityType`, used for parameters, return values and every value read from storage (storage starts arbitrary, so state variables lack ranges too). Bigger; also serves 4A, 6A and 7B.

### 11. A static → dynamic storage array copy loses the length

`d2 = d1` with `uint[9] d1; uint[] d2;` leaves `d2.length` free. `storageRootWriteCopySource` copies the raw value; the 9 lives only in `typed(fixedArr(..))`.

```solidity
uint[9] d1;
uint[] d2;
function f() public {
    d2 = d1;
    assert(d2.length == 9); // SolKey: open. EVM: holds
}
```

- **11B. Lazy length: record n in the save, find it on lookup.** No new axiom on fixed-array values. **Save:** when `storageRootWriteCopySource` copies a fixed source into a dynamic target, the save also records the source's static length n (taken from its declared type when the rule fires) on the target's length, as a resize does. **Find:** `.length` and `size` of a dynamic storage array resolve by walking the save chain to the newest length save, through the select-on-save rules; `size(v)` of the fixed value is never asked. Lazy: nothing is derived until a read meets that save. Fixes 11 and gives 12D its n; changes only the copy save and the length lookup.
- **11A. Type-guarded copy rules.** A variant of `storageRootWriteCopySource` for a fixed source and dynamic target that also sets `size` to n. Small, but one more rule in the copy family.

### 12. Copying a shorter static array into a longer one leaves the tail

`big = small` with `uint[40] big; uint[20] small;` should zero `big[20..39]`. SolKey reads `big[30]` as `small[30]`, which is out of bounds and unconstrained: `selectOnSaveEmptyDefault` fires for primitive elements and never compares the index with the source size.

```solidity
uint[40] big;
uint[20] small;
function f() public {
    big[30] = 4;
    big = small;
    assert(big[30] == 0); // SolKey: open, big[30] reads the unconstrained small[30]. EVM: holds
}
```

- **12D. A size-aware read rule for primitive elements** (from the earlier session on this bug). Add a primitive twin of `selectOnSaveEmptyIndexStruct` (`structRules.key:209`) with its three cases: `i < size(v)` gives `v[i]`; `size(v) ≤ i < size(old)` gives 0; otherwise `old[i]`. `selectOnSaveEmptyDefault` must then stop matching an `at(i)` read. No new function symbol; in place of `size(v)` it uses the n that 11B records in the same save.
- **12A. Desugar to `delete big; big = small;`** in the parser when the static lengths differ. Reuses the existing delete rules; no new taclet.
- **12C. Shared with 11.** One `convertArray(from, to, v)` function covers fixed → dynamic (bug 11) and fixed → longer fixed (this bug).
- **12B. A conversion function** `resize<[n,m]>(v)`: elements below n kept, above n `defaultValue`. Exact and general (also memory → storage), but new rules and lemmas.

## Cross-cutting ideas

Three shared pieces would each fix several bugs at once, so they are worth deciding first.

- **X1. A per-type value table.** One place mapping a `KeYSolidityType` to its default, its range predicate and its wrap function. It is the base for 4A, 6A, 7B, 8C, 10B and 12B.
- **X2. One alpha-renaming pass** for every inlined body (2B). Removes the class of bug behind 2, and future ones from recursion or repeated calls.
- **X3. A soundness gate in CI.** Run `SolidityRuntimeCheck` on every function whose proof closes; a closed proof whose EVM run fails `assert` fails the build. That is how bugs 1–5 were found. It needs constructor support in the runtime check first, since that skips any contract with a constructor or constant today.
- **Mirror in solidity-lean.** It already has checked arithmetic, defaults and fresh renaming, so it can be the reference for 6, 7, 8 and 2. It shares bug 5 (its `noCallback` `try` never runs the callee), so a fix there should land in both.

## Order of work

1. Now: 1B and 2B, two small parser fixes that remove unsoundness.
2. Now: 11B then 12D, the storage array copies; 12D reads the size 11B stores.
3. Now: 8C, which adds the `DefaultValue(T)` node that bug 7 reuses later.
4. Now: 3A, together with `super` support.
5. Later: 5A plus 5D, with an example per call kind in `TestSuite.sol`; then 7B.
6. Not now: 4, 6 and 10 (conversions, integer width, type ranges).
