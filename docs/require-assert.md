# Require and Assert Rules — Summary for LLM Consumption

A flat-Markdown distillation of the "Require and Assert" subsection of
`main.tex` / `main.pdf`. Self-contained; no LaTeX macros.

## 1. Overview

`require(c)` and `assert(c)` both evaluate a Boolean condition `c`
before deciding whether normal execution may continue. They look
identical in Solidity source, but their proof rules differ on the
false branch — and that difference is the entire point of having two
keywords.

- `require` models a **recoverable guard**: if the condition fails,
  the transaction aborts via an explicit `revert();`.
- `assert` models a **verification obligation**: the condition must
  be discharged by the proof; failure is a bug, not a graceful abort.

This mirrors Solidity's intent: `require` is for input/state
validation that may legitimately fail at runtime; `assert` is for
invariants that should never fail.

## 2. Two-Step Strategy

For both keywords:

1. **Capture.** If the condition is nonsimple (`nse`), first lift it
   into a Boolean stack temporary `bool pv = nse;`.
2. **Decide.** Once the condition is a simple Boolean (`se`), split
   into a true and a false branch.

The true branch is the same for both: drop the statement and continue
with `⟨ π ω ⟩ φ`. The false branch is where they diverge.

## 3. Capture Rules

Both capture rules are structural: they just hoist the nonsimple
sub-expression into a temporary so the simple rule can fire next.

- `requireConditionCapture`

      require(nse);
      ⇝  ⟨ π  bool pv = nse; require(pv);  ω ⟩ φ

- `assertConditionCapture`

      assert(nse);
      ⇝  ⟨ π  bool pv = nse; assert(pv);  ω ⟩ φ

Schema variable `nse` is any nonsimple expression. The result `pv` is
a fresh simple Boolean. The capture rules carry no condition: the
Boolean type is fixed by the substitution.

## 4. Simple Rules — The Modality-Sensitive Step

Both simple rules split on the Boolean value `se`. The true branch
drops the statement. The false branch differs.

- `requireSimple`

      require(se);
      ⇝  se      : ⟨ π ω ⟩ φ
         ¬se     : ⟨ π revert(); ω ⟩ φ

- `assertSimple`

      assert(se);
      ⇝  se      : ⟨ π ω ⟩ φ
         ¬se     : ⊥

The `require` false branch reduces to `revert();`. The `assert` false
branch closes with `⊥` directly, without going through `revert();`.

## 5. Why the Branches Differ

The two `revert();` rules close the proof differently in box vs
diamond (see the storage doc, §8):

- `revertDiamond`:  `⟨ π revert(); ω ⟩ φ`  ⇝  `⊥`
- `revertBox`:      `[ π revert(); ω ] φ`  ⇝  `⊤`

Combining these with the simple rules gives the effective semantics:

| Statement     | Diamond `⟨·⟩`   | Box `[·]`        |
| ------------- | --------------- | ---------------- |
| `require(c)`  | `c ∧ φ`         | `c → φ`          |
| `assert(c)`   | `c ∧ φ`         | `c ∧ φ`          |

- For `require` in box, the false branch is vacuously true (`¬c →
  ⊤`), so the rule degenerates to `c → φ`. That is exactly the
  recoverable-guard reading: the proof only cares about executions
  that actually continue.
- For `assert` in box, the false branch must instead close with `⊥`
  to keep the obligation alive. Routing through `revert();` would
  vacuously discharge it (`¬c → ⊤`), which would silently weaken
  every box-modality assert — the bug `assert` is supposed to flag.

So `assert` is the same in both modalities; only `require` inherits
the box / diamond split from the `revert();` rules.

## 6. Discipline

**Pairwise disjointness.** `requireConditionCapture` matches when the
operand is nonsimple; `requireSimple` matches when it is simple. Same
for `assert`. Exactly one rule applies to any well-typed call.

**Termination.** Capture decreases the complex-expression count
(component 2 of the storage termination measure). The simple rules
decrease the statement count (component 4), either by dropping the
statement (true branch) or by replacing it with `revert();`, which
`revertDiamond` / `revertBox` immediately consume.

## 7. Quick Reference

| Rule                       | Trigger              | Effect on the false branch |
| -------------------------- | -------------------- | -------------------------- |
| `requireConditionCapture`  | `require(nse);`      | (n/a — capture)            |
| `assertConditionCapture`   | `assert(nse);`       | (n/a — capture)            |
| `requireSimple`            | `require(se);`       | `⟨ π revert(); ω ⟩ φ`      |
| `assertSimple`             | `assert(se);`        | `⊥`                        |
