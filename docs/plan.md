# Plan: enable `testMemoryUintArrayAuxiliaryCases` and `testStorageEvaluationOrder`

## Context

Two examples in `keyext.solidity.examples/mainFeatures/` are wired into
`PaperTestExamplesTest` only as `@Disabled` placeholders:

- `testMemoryUintArrayAuxiliaryCases.key` — `carolValues[++i] = 77` on a **memory `uint[]`**
- `testStorageEvaluationOrder.key` — `a[++i] = ++i` on a **storage `uint[]`**, preceded by
  `a.push(100); a.push(100); a.push(100);`

The disabled comments blame "no `++i`-in-index desugaring" and "the void-call statement-suffix
reconstruction bug". **Both comments are now stale.** I re-ran both `.key` files through
`solidityCli` and dumped the partial proof trees; the real causes are different and are documented
below. The goal is to make both proofs close and flip the two tests from `@Disabled` to entries in
the `examples()` stream.

## How this was diagnosed

`solidityCli` only prints a goal count, so each case was run with `-o <file>.proof` and the proof
tree / open goal inspected, plus minimal repros under the scratchpad. Key observations:

| Repro | Result |
|---|---|
| `testMemoryUintArrayAuxiliaryCases` (as-is) | **SortException** at node 17, 1 goal |
| `uint t = ++i; c[t] = 77; assert(c[1]==77)` (hand-desugared) | **closes** |
| `testMemoryTokenArrayAuxiliaryCases` (struct array, enabled) | **closes** |
| `testStorageEvaluationOrder` (as-is, no precond) | **30001-node loop** |
| same **with** `find(storage,a·size)=0` precond | terminates, 1 goal stuck on `a[++i]=++i` |
| `a.push×3; a[i]=i;` (plain index, no precond) | **loops** |
| `a.push×3; assert(a.length==3)` (no index write) | closes-ish, **no loop** |
| `a.push×1; a[0]=7;` (single push + index write) | **no loop** |

## Root cause A — memory test: `SortException` from a tie-broken rule race

The proof of the memory case applies `memoryIndexWriteNonSimpleIndexCapture`
(`solidityProgramRules.key:2731`) correctly, rewriting

```
carolValues[++i] = 77;   →   uint _t = ++i;  carolValues[_t] = 77;
```

where `_t` is a fresh temp created by `\newTypeOf(pv, ++i)`. The next step is where it diverges:

- **Working struct proof** (`testMemoryTokenArrayAuxiliaryCases`): the specific
  `localDeclPreincrement` (`:2674`) fires on `uint _t = ++i;` → `{_t := i+1}{i := i+1}` → closes.
- **Failing primitive proof**: the **generic** `valueDeclInitSplit` (`:1216`, matches `Type vp = e`
  for *any* `e`) fires first, splitting into `uint _t; _t = ++i;`, then `valueDeclSkip` re-registers
  `_t`; the subsequent update build throws
  `SortException: Sort of SV is not compatible with its instantiation's sort`.

Both rules match `uint _t = ++lv` and both sit in the **same `simplify_prog` rule set**
(`SymExStrategy.java:88`, cost `-100/200`), so the winner is an arbitrary tie-break. The struct case
won the tie with the good rule; the primitive case lost it. `localDeclPreincrement` works because it
consumes `pv` directly; the `valueDeclInitSplit`→`valueDeclSkip` path re-materialises the temp from
the `\newTypeOf` type and trips the sort check.

**Fix A:** make the dedicated increment-resolution rules strictly preferred over the generic split.
Add a high-priority rule set (e.g. `resolve_local_increment`) in
`SymExStrategy.setupCostComputationF()` bound at a strongly-negative cost (≈ `-4000`, like
`simplify_prog_subset`), and change `localDeclPreincrement` and `localAssignPreincrement`
(`:2674`, `:2688`) from `\heuristics(simplify_prog)` to that set. This forces the proven-good path
deterministically. (The struct example already validates that path closes.)

## Root cause B — storage test: two independent blockers

The storage example has **no precondition** and must clear two separate problems.

### B1 — `a[++i] = ++i` has no storage desugaring (confirmed with precond)

With a precondition added, the loop disappears and the proof gets *stuck* on `a[++i] = ++i;`
(open goal: storage already holds `[100,100,100]`, `i=0`). The storage index-write rules
(`storageIndexWriteArraySave_root` `:302`, `storageIndexWriteNonSimpleIndexCapture` `:2715`) all
require a **simple RHS**, and there is **no rule to capture a non-simple RHS** for a storage index
write. Solidity evaluates the **RHS before the LHS index**, which the test asserts (`a[2]==1` ⇒ RHS
`++i`→1 happens first, index `++i`→2 second).

**Fix B1:** add a storage analogue of `memoryIndexWriteMemRefRhsCapture` (`:2699`):

```
storageIndexWriteNonSimpleRhsCapture {
    sp  : Path[name=storage.simple.global.array];
    e   : Expression;          // index, possibly non-simple
    nse : NonSimpleExpression; // RHS
    \find( sp[e] = nse; )
    \varcond(\newTypeOf(pv, nse), \newTypeOf(pvType, nse))
    \replacewith( pvType pv = nse;  sp[e] = pv; )   // RHS temp emitted FIRST
    \addprogvars(pv) \heuristics(simplify_prog)
}
```

Ordering is self-enforcing: the index-capture rule (`:2715`) cannot fire while the RHS is
non-simple, so RHS-capture necessarily runs first, giving `uint _rhs = ++i; uint _idx = ++i;
a[_idx] = _rhs;` — i.e. `_rhs=1`, `_idx=2`, `a[2]=1`. Both `uint _ = ++i;` temps are then resolved
by **Fix A**. (Root-level `storage.simple.global.array` is sufficient for this test; complex
receivers are out of scope.)

### B2 — pre-existing push/strategy infinite loop (the actual 30001-node wall)

Independent of `++i`: `a.push×3` followed by *any* array index-write with a **symbolic length**
loops on `storagePushLhsToPushValue` (`:508`, `sp.push() = se → sp.push(se)`), ~155+ identical
applications on the 3rd push. It does **not** reproduce with a precondition, with a single push, or
without a following index-write — it needs the deeply-nested `save(save(save(…)))` term from
multiple pushes plus a following index write. Pushes 1–2 are consumed by `storagePushValueSave`
(`:542`, rule set `simplify_expression`); only push 3 flips to the looping rule. This is the
"void-call statement-suffix" family the disabled comment referenced (the earlier
`FunctionDeclaration.visit` fix only addressed the crash, not this loop).

**Fix B2 (recommended):** make the terminal saves strictly dominate the lhs-rewrites so the save
always consumes `sp.push(value)` before the rewrite can re-fire — e.g. give `storagePushValueSave` /
`storagePushLengthSave` / `storagePushValueCopySource` a strictly lower cost than
`storagePushLhsToPushValue` / `storagePushLhsToPushValueCopySource`, or guard the lhs-rewrites so
they cannot re-match their own `sp.push(se)` output.
**Confirm first with a debugger** why `storagePushLhsToPushValue` (whose `\find` is an *assignment*)
applies to a function-call `a.push(100)` at all — if the cause is a `CreatingASTVisitor`
reconstruction of the push `FunctionCallExpression` (analogous to the `FunctionDeclaration.visit`
fix), fix that instead, as it is the more robust root-cause fix.

## Files to change

- `keyext.solidity.core/src/main/java/org/key_project/solidity/strategy/SymExStrategy.java`
  — new high-priority rule set for increment resolution (Fix A); possibly push cost ordering (B2).
- `keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/solidityProgramRules.key`
  — `\heuristics` of `localDeclPreincrement`/`localAssignPreincrement` (Fix A); new
  `storageIndexWriteNonSimpleRhsCapture` (Fix B1); guard/heuristics on the push lhs-rewrites (B2).
- `keyext.solidity.core/src/main/java/org/key_project/solidity/program/ast/visitor/CreatingASTVisitor.java`
  — only if B2 turns out to be a push reconstruction bug.
- `keyext.solidity.core/src/test/java/org/key_project/solidity/taclets/PaperTestExamplesTest.java`
  — remove the two `@Disabled` methods, add `testMemoryUintArrayAuxiliaryCases.key` and
  `testStorageEvaluationOrder.key` to the `examples()` stream.
- `docs/taclets-implementation.md` — move both cases from "disabled/missing support" to supported;
  document `storageIndexWriteNonSimpleRhsCapture` and the increment-priority change.

## Suggested order of work

1. **Fix A** (memory): smallest, self-contained, validated-by-analogy. Land + verify the memory
   test closes before touching storage.
2. **Fix B2** (loop): unblocks the storage push phase; confirm mechanism with a debugger first.
3. **Fix B1** (storage RHS capture): finishes `a[++i] = ++i`.
4. Flip the tests and update docs.

## Risks / open questions

- Fix A assumes deterministic priority is sufficient (struct example supports this); confirm by
  running the memory test. A deeper alternative is fixing the `\newTypeOf`/`valueDeclSkip` typing of
  the captured temp, but that is higher-risk and unnecessary if A closes the proof.
- Fix B2's exact mechanism (why an assignment-shaped `\find` matches a call) is not yet pinned;
  the cost-ordering fix masks it reliably, but a `CreatingASTVisitor` root-cause fix is preferable
  if confirmed.
- Reprioritising rule sets is global; re-run `:keyext.solidity.core:test` to ensure none of the 30+
  currently-passing examples regress.

## Verification

```bash
# Per-file (absolute path; CLI resolves relative paths against test-resources, not this dir):
./gradlew :keyext.solidity.core:solidityCli --args="-m 30000 -s \
  /abs/path/keyext.solidity.examples/mainFeatures/testMemoryUintArrayAuxiliaryCases.key"
./gradlew :keyext.solidity.core:solidityCli --args="-m 30000 -s \
  /abs/path/keyext.solidity.examples/mainFeatures/testStorageEvaluationOrder.key"
# Expect: "Loading and proof successful" for each.

# Then the test class (must keep all existing examples green):
./gradlew :keyext.solidity.core:test --tests \
  "org.key_project.solidity.taclets.PaperTestExamplesTest"

./gradlew spotlessApply
```
