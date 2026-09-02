# Rule Generalizations

Many taclets in `solidityProgramRules.key` are instances of one generic pattern
`op(a, b) = c` that differs only by operator — e.g. `storageRootAddAssign`,
`storageRootSubAssign`, and `storageRootMulAssign` are the same rule with
`+=`/`+` swapped for `-=`/`-` and `*=`/`*`. Each such taclet carries a
machine-checked annotation naming its family, and
`RuleGeneralizationTest` verifies — deterministically, in the ordinary Gradle
test run — that the claim is true.

## The annotation

One comment line in the block directly above the taclet name:

```
// generalized by: storageCompoundAssign(op=div, loc=root, variant=guarded)
```

Grammar: `// generalized by: <familyId>(<key>=<value>, ...)` with keys in the
fixed order `op, loc, fixity, variant`; a key is omitted when the family does
not use it. The annotation text is *rendered from the spec in
`RuleGeneralizationTest`* — the test requires the comment to be byte-identical
to the rendered form, so a drifted or hand-edited annotation fails the build.

## How verification works

The test does not need a theorem prover or any external tool. For every family
member the spec declares an ordered list of **hole strings** — the only parts
of the taclet that are allowed to differ across the family:

- a *name hole* replaced in the taclet name (`Add` in `storageRootAddAssign`),
- *body holes* replaced in the taclet body (`+=`, then `+`).

Each member is reduced to a **skeleton**: comments are stripped, whitespace
runs collapse to single spaces, and each hole string is replaced (all
occurrences, in spec order) by a numbered placeholder `⟨0⟩⟨1⟩…`. All members of
a group must produce a **byte-identical skeleton**; any difference beyond the
declared holes — a changed guard, a reordered update, a different heuristic —
fails the test with a pointer to the first divergence.

Structural variants each form their own group, so the deviation itself is
still verified as uniform across operators:

| Variant | What differs |
|---|---|
| `plain` | the unguarded effect rule |
| `guarded` | `/=`, `%=` and `/`, `%` wrap the effect in `\if(se != 0) … \else(revert)`; div and mod must be identical to each other |
| `unfold` | receiver capture (`_unfold_leftFst`); uniform across all five ops, no guard |
| `stmt` / `assign` / `decl` | inc/dec as a statement vs `result = …` vs declaration form |
| `fixity=pre` vs `fixity=post` | pre writes storage before binding the result, post binds first — separate groups |

Hole ordering rule: holes apply in spec order, so a string must come before
its substrings (`+=` before `+`, `++s#gp` before `+`); the test rejects a spec
where an earlier hole is a substring of a later one.

## Families

| Family | Members | Covers |
|---|---|---|
| `storageCompoundAssign` | 25 | `storage{Root,Field,Index}{Add,Sub,Mul,Div,Mod}Assign` (+ `_unfold_leftFst`) |
| `binaryOp` | 18 | `{addition,…,modulo}_unfold_left/right` + `…Assignment` |
| `storageIncDec` | 32 | `storage{Root,Field,Index}{Pre,Post}{in,de}crement` (+ `Assignment`, `_unfold_leftFst`) |
| `localIncDec` | 12 | `localDecl…` / `localAssign…` / `local…` inc/dec |
| `localCompoundAssign` | 5 | `local{Add,Sub,Mul,Div,Mod}Assign` |
| `compoundAssignRhsCapture` | 5 | `{add,…,mod}AssignValueRhsCapture` |

97 annotated taclets in total.

## Running the check

```bash
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.taclets.RuleGeneralizationTest"
```

It runs as part of the normal module test suite (and therefore in CI). The
suite includes a self-test that injects a divergence into a fabricated member
and asserts it is detected, so a vacuously green checker also fails.

## Adding a member to a family

1. Write the new taclet so it matches its group's skeleton exactly — copy an
   existing member and swap the operator tokens.
2. Add the member to the matching group in `RuleGeneralizationTest`'s `SPEC`
   (name, `op`, optional `fixity`, name hole, body holes — most specific hole
   first).
3. Run the test. The failure message for a missing annotation prints the exact
   comment line to paste above the taclet.

If the new rule genuinely deviates (a new guard, different update order), do
not stretch an existing group: add a new variant group so the deviation is
named in the annotation and checked for uniformity across its own members.
