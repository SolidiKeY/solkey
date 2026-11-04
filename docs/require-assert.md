# Require and Assert Rules

## 1. Overview

At runtime `require(c)` and `assert(c)` behave the same way: if `c` is false, the transaction
reverts. Proofs treat the two failures differently:

- `require` is a **recoverable guard**. Its failure is an accepted way for the
  transaction to end: it reverts and the state is unchanged.
- `assert` is a **verification obligation**. Its failure is a bug, so the proof must
  show it cannot happen.

## 2. Outcomes and modalities

A statement ends in one of three outcomes, and the modality decides which ones it accepts:

```
                       ┌──────────┬───────────┬───────────┐
                       │  normal  │  revert   │   panic   │
                       │ (ends ok)│ (require) │ (assert)  │
  ┌────────────────────┼──────────┼───────────┼───────────┤
  │ [p]φ   box         │    φ     │     ⊤     │     ⊥     │
  │ ⟨p⟩φ   diamond     │    φ     │     ⊥     │     ⊥     │
  └────────────────────┴──────────┴───────────┴───────────┘

  box     = "reverts are fine, panics never, φ if it ends normally"
  diamond = "it must end normally, and then φ"
```

Reading the table for each keyword:

```
  require(c)  :=  if ¬c then REVERT          assert(c)  :=  if ¬c then PANIC

  [require(c)]φ  ≡  c → φ                    [assert(c)]φ  ≡  c ∧ φ
  ⟨require(c)⟩φ  ≡  c ∧ φ                    ⟨assert(c)⟩φ  ≡  c ∧ φ
```

**`require` differs from `assert` only in box, where its false branch is dropped.**

## 3. Capture rules

If the condition is nonsimple (`nse`), it is first lifted into a fresh Boolean temporary so
the rules of §4 only ever see a simple condition `se` (a Boolean variable or literal):

```
  requireConditionCapture   {π require(nse); ω}φ  ⇝  {π bool pv = nse; require(pv); ω}φ
  assertConditionCapture    {π assert(nse);  ω}φ  ⇝  {π bool pv = nse; assert(pv);  ω}φ
```

## 4. Simple rules

All three put the condition into the antecedent of the continuing goal:

```
(requireBox)              Γ, c ⊢ [π ω]φ
                 ─────────────────────────────────          one goal
                   Γ ⊢ [π require(c); ω]φ


(requireDiamond)     Γ ⊢ c        Γ, c ⊢ ⟨π ω⟩φ
                 ─────────────────────────────────          "Reverts" / "Holds"
                   Γ ⊢ ⟨π require(c); ω⟩φ


(assertSimple)       Γ ⊢ c        Γ, c ⊢ {π ω}φ             {·} ∈ { [·], ⟨·⟩ }
                 ─────────────────────────────────          "Violated" / "Holds"
                   Γ ⊢ {π assert(c); ω}φ
```

- `requireBox`: a failed `require` reverts, which box accepts, so nothing is proved for it.
  The rule matches only in the succedent (`\find(==> …)`). In the antecedent, dropping the
  `¬c` case would be unsound.
- `requireDiamond` and `assertSimple` have the same shape. Both match anywhere with
  `\sameUpdateLevel`, so the added `c` is read in the state the statement runs in.
  In the antecedent they are still sound, since `⟨require(c)⟩φ` and `{assert(c)}φ` both imply
  `c ∧ {…}φ`.

`require` does not go through `revert();`. An explicit `revert();` statement keeps its own
two rules (`docs/storage.md` §8): `revertBox` ⇝ `⊤`, `revertDiamond` ⇝ `⊥`.

## 5. Discipline

**Pairwise disjointness.** Capture matches a nonsimple operand, the simple rules a simple one.
`requireBox` and `requireDiamond` are split by modality, so exactly one rule applies.

**Termination.** Capture decreases the complex-expression count (component 2 of the storage
termination measure); each simple rule removes the statement (component 4).

## 6. Quick reference

| Rule                       | Trigger                   | Goals                          |
| -------------------------- | ------------------------- | ------------------------------ |
| `requireConditionCapture`  | `require(nse);`           | 1 (capture)                    |
| `assertConditionCapture`   | `assert(nse);`            | 1 (capture)                    |
| `requireBox`               | `⊢ [… require(se); …]φ`   | 1: `se` assumed                |
| `requireDiamond`           | `⟨… require(se); …⟩φ`     | 2: prove `se`; `se` assumed    |
| `assertSimple`             | `{… assert(se); …}φ`      | 2: prove `se`; `se` assumed    |
