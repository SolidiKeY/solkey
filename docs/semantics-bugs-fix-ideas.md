# SolKey semantics bugs: fix ideas

## Scope

This covers the open semantics bugs: places where SolKey's model of a construct disagrees with the EVM. Three prove something false (unsound); one leaves a true fact unprovable because the model forgets a value Solidity guarantees.

All come from `docs/bugs.md` plus the unbounded-integer design choice it opens with. Each bug lists two to four fix ideas, labelled A, B, C, and listed in order of preference, best first. The numbers and letters are stable labels (`docs/bugs.md` cites them), so they do not run in order. **When a bug here is fixed, delete its section and its row in the plan.**

Left out on purpose: crashes, proofs stuck on program text, strategy gaps (`sdiv` heuristics, `ex_pull_out`, `\dropEffectlessElementaries`, the mapping-reference `cast` chain) and the `--solc` runner bugs. Those are tool defects, not wrong semantics.

Already fixed, and so removed: 1 (named arguments bind by position, 1B), 2 (modifier locals shared, 2B), 3 (virtual call binds to base, 3A), 7 (`address` named return unconstrained: returns take the locals' `ParserUtils.defaultValue`), 8 (uninitialised local unconstrained, 8A: `ParserUtils.defaultValue`), 9 (`constant` reads as storage, 9A), 11 (static → dynamic copy loses the length, 11B) and 12 (shorter static copy leaves the tail, 12D).

## Plan

| # | Bug | Status | Plan |
| --- | --- | --- | --- |
| 4 | Type conversions are the identity | Not now | — |
| 5 | `try` / `call{value}` keep storage | Later | 5A + 5D |
| 6 | Unbounded integers | Not now | — |
| 10 | No type range on parameters, `msg.value` | Not now | — |

## Bugs that prove something false

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

Bug 10 shares one need with bug 4: a per-type table of value ranges.

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
- **10B. A general `inType<[T]>(v)` predicate.** One predicate from `KeYSolidityType`, used for parameters, return values and every value read from storage (storage starts arbitrary, so state variables lack ranges too). Bigger; also serves 4A and 6A.

## Cross-cutting ideas

- **X1. A per-type value table.** One place mapping a `KeYSolidityType` to its default, its range predicate and its wrap function. It is the base for 4A, 6A and 10B.
- **X3. A soundness gate in CI.** Run `SolidityRuntimeCheck` on every function whose proof closes; a closed proof whose EVM run fails `assert` fails the build. That is how bugs 1–5 were found. It needs constructor support in the runtime check first, since that skips any contract with a constructor or constant today. `OpenExamplesStayOpenTest` already fails CI when an `open/` function starts to close.
- **Mirror in solidity-lean.** It already has checked arithmetic, defaults and fresh renaming, so it can be the reference for 6. It shares bug 5 (its `noCallback` `try` never runs the callee), so a fix there should land in both.

## Order of work

1. Later: 5A plus 5D, with an example per call kind in `TestSuite.sol`.
2. Not now: 4, 6 and 10 (conversions, integer width, type ranges).
