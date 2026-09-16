# Storage Rules — Summary for LLM Consumption

A flat-Markdown distillation of Section 2 ("Storage Rules") of
`main.tex` / `main.pdf`. Self-contained; no LaTeX macros. Memory rules
are out of scope.

## 1. Overview

The calculus is sequent-style dynamic logic over Solidity statements.
A conclusion has the shape

    => ⟨ π stmt ω ⟩ φ

- `π` is the inactive prefix (already executed / not yet handled).
- `stmt` is the first active statement.
- `ω` is the rest of the program.
- `φ` is the post-condition.

Rules are given for the diamond modality `⟨·⟩`. The box modality
`[·]` is identical except for the two `revert();` rules
(see §8).

Storage is a single semantic variable `storage` of sort `Struct`
that is updated by parallel updates `{ x := e }`. Multiple
simultaneous assignments are written `{ a := e1 || b := e2 }`
(parallel composition).

### Three-step strategy

For every storage statement, the calculus applies in this order:

1. **Step 1 — Unfold the RHS.** Capture nonsimple sub-terms on the
   right-hand side into fresh simple aliases.
2. **Step 2 — Unfold the LHS.** Capture nonsimple sub-terms on the
   left-hand side.
3. **Step 3 — Emit an update.** Once every constituent is simple,
   replace the statement with a parallel update on the continuation
   `⟨ π ω ⟩ φ`.

This mirrors Solidity's evaluation order, which the calculus pins as
**right-hand side, then receiver, then index**. Every capture rule emits
its fragments in that order, and `SolidityRuntimeExecutionTest` checks
each of the three hand-offs against a real EVM.

## 2. Schema Variables

Schema variables fix the kind of program fragment matched, so rules
are pairwise disjoint without explicit applicability conditions.

Expressions (RHS):
- `se` — simple expression: a single stack-valued word (program
  variable or constant). No side effects.
- `nse` — nonsimple expression: anything that is not `se`.
- `e` — arbitrary expression (used where the distinction does not
  matter).

Assignment targets (LHS):
- `v` — stack program variable used as a read target.
- `lhs` — arbitrary assignment target.
- `nlhs` — a nonsimple assignment target.

Storage paths:
- `sp` — simple storage path: a contract root or a generated alias.
  Never an assignment target.
- `lsv` — a **local** storage variable, i.e. a reference declared inside a
  function (e.g. `Person storage p`). Sort `Variable[storage]`: a program
  variable whose value is a path, so an update assigns to it.
- `gsp` — a **global** simple path: a contract root, the storage location a
  write to it addresses. Sort `Path[storage,simple,global]`.
- `nsp` — nonsimple storage path (unresolved base, receiver, or
  index).
- `path` — plain storage path when the simple/nonsimple distinction
  is irrelevant.
- `ie` — simple index expression.

Type-of metavariable:
- `T_{expr}` (written `Tkindof(expr)` in the PDF) — the Solidity
  type of `expr`, used in synthesized declarations like
  `T_{nsp} storage sp = nsp;`.

## 3. Storage-Model Primitives

`storage` is a `Struct` value manipulated by these operations:

- `find(storage, path)` — read the value at `path` (a `List` of field
  selectors). Used universally for both global roots and local paths.
- `save(storage, path, val)` — write `val` at `path`. Used universally
  for both global roots and local paths. `val` has sort `StValue`, the
  supersort of everything storable in storage: `Struct` (incl. the
  dynamically created array/mapping sorts, which `SolJSONParser` builds as
  sub-sorts of `Struct` because an array or mapping value *is* a struct node
  — `mtSt` plus `at(i)` fields) and `Prim` (`int`, `bool`, contract sorts).
  `Identity`, `List` and `Memory` are not storable.
- `default` — the default value of a type (used by `delete` and
  `pop`).
- `at(i)` — coerces an index/key into a field selector so it can
  appear inside a path.
- `length` — the synthetic field holding a dynamic array's current
  length.

A rule which *clears* or *copies* a location does not have to know what that
location holds: it stays sort-free by using `find<[StValue]>` (the polymorphic
`find` at the top storage sort) for copies, and the operations below for
clears. The sort is resolved when the value is read back — for
`find<[StValue]>` and `defVal` through the cast that `selectOnStore` (and
`readOnWrite` in memory) already inserts (`findStValueCast` collapses
`cast<[alphaSt]>(find<[StValue]>(st, path))` to `find<[alphaSt]>(st, path)`),
for `delAt` through `delValue<[alpha]>` on the select:

- `delAt(storage, path)` — the storage with the location at `path` reset. A
  struct there becomes the lazy `delNode` marker, so its mapping members
  survive; anything else collapses to its default. The choice is made on read,
  by sort, through `delValue<[alpha]>` in `selectOnDelAtCons` (the
  counterpart of `selectOnSaveCons`) — so the rules that write it stay
  sort-free. Used by `delete` on a root or field, and by `push`/`pop` to clear
  the slot they add or remove.

  It names `storage` **once**. The equivalent `save(storage, path, <deleted
  value at path>)` names it twice, which doubles the storage term at every
  `push`/`pop`; a sequence of them then grows exponentially rather than
  linearly, which is what made `SolcArrays.pushThenPopRestoresLength` slow.

  The two deferrals meet when a sort-free copy reads a *cleared* location —
  `delete sp; gsp = sp;` and its field, root and `push` variants.
  `selectOnDelAtCons` instantiates its generic at the reader's sort, so a
  `find<[StValue]>` copy leaves `delValue<[StValue]>(…)`, which neither
  `delValueStruct` (concrete `Struct`) nor `delValueDefault` (`alphaPrim
  \extends Prim`) matches. `delValueStValueCast` is the twin of
  `findStValueCast` for that shape: it pushes the read's cast inward,
  `cast<[alphaSt]>(delValue<[StValue]>(v))` ⇝
  `delValue<[alphaSt]>(cast<[alphaSt]>(v))`, so the reset resolves at the sort
  the read supplies. Same coherence assumption as `findStValueCast` — observing
  a sort-parametric family at a smaller sort is that family's smaller-sort
  instance — and it leaves the `delValueStruct`/`delValueDefault` split
  disjoint, where widening `delValueDefault` back to `StValue` would make the
  two overlap again. Pinned by `storage{Field,Root}DeleteThenCopy` and
  `storageFieldDeleteThenCopyDeep`.
- `copyAt(storage, path, val)` — the storage with the location at `path`
  overwritten by `val`, except that mapping members keep what the target
  held: Solidity never copies a mapping. Every storage-to-storage copy
  writes `copyAt(storage, p1, find<[StValue]>(storage, p2))` (two mentions
  of `storage`, as the `save`-of-`find` form it replaced). Lazy like
  `delAt`: `selectOnCopyAtCons` walks the path and, at the location, reads
  `merge<[alpha]>(old, cast<[alpha]>(val))` at the reader's sort.
- `merge<[alpha]>(old, new)` — a copied location as seen by a read. A
  `MapField` member comes from `old` (`selectStMergeMap`), a `RefField`
  member recurses (`selectStMergeRef`), an `at(i)` element and every
  primitive member come from `new` (`selectStMergeIndexStruct`,
  `selectStMergeDefault`), and at a primitive sort `merge` is `new`
  (`mergePrim`). `mergeStValueCast` pushes a reader's cast into a
  `merge<[StValue]>` left by a sort-free copy of a copied location, and
  `selectStValueCast` collapses `cast<[alphaSt]>(selectSt<[StValue]>(st, a))`.
  Pinned by `storage/copyKeepsMapping.key` (the mapping half, which no
  `.sol` can state) and the `testCopy*` group of `TestSuite.sol`.
- `defVal` — a location reset outright, mapping members included. The
  sort-free twin of `default`. Used by `delete sp[i]`, which resets a
  collection element rather than preserving its mapping members. Sorted
  `Prim`, so it is both an `StValue` and a `MemValue` and serves memory too.

### Field selectors

Field selectors are not a flat sort. Mapping and struct/array members are
stamped at parse time (`SolJSONParser#fieldSortFor`) with a sub-sort of
`Field`, so a rule can say which kind of member it applies to instead of
matching all of them:

| Sub-sort | Member kind | `delete` behaviour |
|---|---|---|
| `MapField` | mapping | entries preserved |
| `RefField` | struct / array reference | recursed into |

Value members stay plain `Field` — the delete-default rule is Field-generic,
so they need no sub-sort of their own. `at(i)` stays a plain `Field` too: an
index's element sort depends on the container, not on the index, so it cannot
be stamped this way.

**Note:** Both global roots (like `alice`) and local storage aliases
(like `lsv`) are represented as `List`-typed paths. A global root
`alice` extracts to `cons(alice, nil)` — a single-element list. This
unification allows all storage operations to use `find`/`save`
uniformly.

Predicates on a simple path:
- `array(sp)` — `sp` denotes a dynamic-array path.
- `mapping(sp)` — `sp` denotes a mapping path.

Array index access additionally generates bounds branches; mapping
index access does not (mappings have no length).

## 4. Step 1: Unfolding the Right-Hand Side

General presentation schemas (these are templates; only the named
instances below are rules of the calculus). `op(path, e)` ranges over
the base operations `path.fld` and `path[e]`.

**unfold_rightFst** — capture a nonsimple receiver on the RHS.

    nsp => ⟨ π  T_{nse} se = nse; lhs = op(se, e); ω ⟩ φ
    --------------------------------------------------------
            => ⟨ π  lhs = op(nse, e); ω ⟩ φ

**unfold_rightSnd** — capture a nonsimple secondary argument
(typically an index) on the RHS.

    nse => ⟨ π  T pv = nse; lhs = op(sp, pv); ω ⟩ φ
    -----------------------------------------------
        => ⟨ π  lhs = op(sp, nse); ω ⟩ φ

**unfold_rightSndResult** — capture a nonsimple target around a
storage read that produces a value.

    nlhs => ⟨ π  T_{nlhs} pv = op(sp, se); nlhs = pv; ω ⟩ φ
    -------------------------------------------------------
            => ⟨ π  nlhs = op(sp, se); ω ⟩ φ

### Instances of unfold_rightFst

**`storageFieldRead_unfold_rightFst`** — `lhs = nsp.fld`

    nsp => ⟨ π  storage sp = nsp; lhs = sp.fld; ω ⟩ φ
    -----------------------------------------------
              => ⟨ π  lhs = nsp.fld; ω ⟩ φ

**`storageIndexRead_unfold_rightFst`** — `lhs = nsp[ie]`

    nsp => ⟨ π  storage sp = nsp; lhs = sp[ie]; ω ⟩ φ
    ------------------------------------------------
              => ⟨ π  lhs = nsp[ie]; ω ⟩ φ

The receiver is captured before the index: `unfold_rightSnd` below
takes a *simple* storage path as receiver, so a complex receiver is
aliased by the rule above first, whatever its index looks like. That
is the order solc commits to — `testNestedIndexReadImpureReceiverAndIndex`
pins it against the EVM.

### Instances of unfold_rightSnd

**`storageIndexRead_unfold_rightSndIndex`** — `lhs = path[nse]`

    nse => ⟨ π  T pv = nse; lhs = path[pv]; ω ⟩ φ
    -----------------------------------------------
          => ⟨ π  lhs = path[nse]; ω ⟩ φ

### Instances of unfold_rightSndResult

**`storageFieldRead_unfold_rightSndResult`** — `nlhs = sp.fld`

    nlhs => ⟨ π  T_{nlhs} pv = sp.fld; nlhs = pv; ω ⟩ φ
    -------------------------------------------------
              => ⟨ π  nlhs = sp.fld; ω ⟩ φ

**`storageIndexRead_unfold_rightSndResult`** — `nlhs = sp[se]`

    nlhs => ⟨ π  T_{nlhs} pv = sp[se]; nlhs = pv; ω ⟩ φ
    ---------------------------------------------------
              => ⟨ π  nlhs = sp[se]; ω ⟩ φ

### Standalone: push argument

The push argument is not an assignment RHS, but it is evaluated before
the push update fires.

**`storagePushValue_unfold_rightSndArgument`** — `sp.push(nse);`

    nse => ⟨ π  T pv = nse; sp.push(pv); ω ⟩ φ
    ------------------------------------------
        => ⟨ π  sp.push(nse); ω ⟩ φ

`nse` is `NonSimpleExpression[primitive]`, not plain `NonSimpleExpression`: the
hoisted `T pv = nse;` is a *value* declaration, so a path-shaped argument must
not match. A bare contract root such as `tok` in `tokens.push(tok);` is a
`FieldReference` and hence non-simple, and hoisting it would bind a value local
to a storage path — a rebind no rule consumes. Excluded here, it reaches
`storagePushValueCopySource` directly.

## 5. Step 2: Writing to a Nonsimple Target

A write `op(recv, ie) = rhs` is decomposed by three rules that partition on
which
constituent is not yet simple. All three capture in the order the EVM evaluates
in — **right-hand side, then receiver, then index** — which is what the runtime
cross-check pins (`testNestedIndexWriteImpureReceiverAndIndex`,
`testIndexWriteReceiverReadsMutatedVar`, `testCompoundAssignImpureReceiver`).
Each capture emits a *declaration*, which `storageLocalDeclInitDrop` /
`localValueDeclInitDrop` then strip, so the rules re-enter and a target nested
any number of levels deep decomposes by recursion rather than by enumeration.

Rules 1 and 2 form a **taclet option**, `indexWriteCapture`, because the same
decomposition can be split across applications in two ways. Rule 3 is shared,
and the field forms need no split at all (a field name is always simple), so
only the index-write rules are duplicated.

**Rule 1 — receiver nonsimple** (`indexWriteCapture:receiverThenIndex`, the
default). Capture the right-hand side and alias the receiver, leaving the index
where it is; once the receiver is an alias, Rule 2 takes whatever the index
turned out to be.

    nsp => ⟨ π  T_{e} rv = e; T_{nsp} sp = nsp; sp[ie] = rv; ω ⟩ φ
    --------------------------------------------------------------
                  => ⟨ π  nsp[ie] = e; ω ⟩ φ

**Rule 2 — receiver simple, index nonsimple** (same option). The receiver is
already a root or an alias, but it is still snapshotted: the index may reassign
the local storage pointer the receiver reads, and the write must land where the
receiver pointed *before* the index ran.

    nse => ⟨ π  T_{e} rv = e; T_{sp1} sp = sp1; T pv = nse; sp[pv] = rv; ω ⟩ φ
    ---------------------------------------------------------------------------
                       => ⟨ π  sp1[nse] = e; ω ⟩ φ

**Rules 1+2 merged** (`indexWriteCapture:allAtOnce`). One rule per right-hand-side
kind captures all three constituents at once, guarded by `\notAllSimple(p, ie)`
so it does not fire on a fully simple `sp[se] = e` and re-match its own output.
`p` is a `Path` of any simplicity, so this single rule covers both cases above.

    notAllSimple(p, ie) => ⟨ π  T_{e} rv = e; T_{p} sp = p; T_{ie} pv = ie; sp[pv] = rv; ω ⟩ φ
    ------------------------------------------------------------------------------------------
                            => ⟨ π  p[ie] = e; ω ⟩ φ

Both options evaluate in the same order and close the same proofs; they differ
only in proof size, and which is smaller depends on the shape — see
`docs/taclets-implementation.md`, "Capture partition", for the measurement and
`scripts/compare-index-write-capture.sh` to reproduce it.

**Rule 3 — receiver and index simple, right-hand side nonsimple.**

    nse => ⟨ π  T_{nse} rv = nse; sp[se] = rv; ω ⟩ φ
    ------------------------------------------------
            => ⟨ π  sp[se] = nse; ω ⟩ φ

The partition is by sort alone and is therefore disjoint: Rule 1 needs
`Path[…,complex]`, Rule 2 `Path[…,simple]` with a `NonSimpleExpression` index,
Rule 3 `Path[…,simple]` with a `SimpleExpression` index and a right-hand side
the terminals reject. The field forms (`recv.fld = rhs`) are the same three
rules
without the index capture.

### The right-hand-side kind

The capture a rule emits depends on what kind of value the right-hand side is,
because the declaration it introduces differs:

| Kind | Sort | Capture |
|---|---|---|
| primitive value | `Expression[primitive]` (Rules 1–2), `NonSimpleExpression[primitive]` (Rule 3) | `T rv = e;` |
| storage reference | `Path[storage,reference]` (Rules 1–2), `Path[storage,complex,reference]` (Rule 3) | `T storage rv = src;` |
| memory reference | `Path[memory,reference]` (Rules 1–2), `Path[memory,complex,reference]` (Rule 3) | `T memory rv = src;` |

`Expression[primitive]` is "primitive-typed and not a *complex* path": literals,
primitive variables, operator expressions and bare contract roots. A complex
path such as `p.age` is excluded, because its own receiver must be resolved by
the read unfolds first — which also evaluate it ahead of the target, so the
right-hand-side-first order is preserved either way.

That table is the whole `kindof` dispatch; it is why Rule 1 has ten instances
(storage receiver × {field, index} × three kinds, memory receiver × {field,
index} × two kinds — a memory location cannot hold a storage reference), Rule 2
five, and Rule 3 six. Only the index halves of Rules 1 and 2 are under the
`indexWriteCapture` option; the five field instances of Rule 1 are unconditional.

### Instances of Rule 1

**`storageFieldWrite_unfold_leftFst`** — `nsp.fld = e`, primitive `e`

    nsp => ⟨ π  T_{e} rv = e; storage sp = nsp; sp.fld = rv; ω ⟩ φ
    ------------------------------------------------------------
                => ⟨ π  nsp.fld = e; ω ⟩ φ

**`storageIndexWrite_unfold_leftFst`** — `nsp[ie] = e`, primitive `e`

    nsp => ⟨ π  T_{e} rv = e; storage sp = nsp; sp[ie] = rv; ω ⟩ φ
    --------------------------------------------------------------
                  => ⟨ π  nsp[ie] = e; ω ⟩ φ

**`storageFieldWriteStorageRef_unfold_leftFst`** — `nsp.fld = src`,
storage `src`,
and likewise `memoryToStorageField_unfold_leftFst` (memory `src`)

    nsp => ⟨ π  T_{src} storage rv = src; storage sp = nsp; sp.fld = rv; ω ⟩ φ
    ------------------------------------------------------------------------
                    => ⟨ π  nsp.fld = src; ω ⟩ φ

`storageIndexWrite…_unfold_leftFst` is the `nsp[ie]` twin of each (leaving `ie`
in place rather than capturing it), and
`memoryFieldWriteMemRef_unfold_leftFst` / `memoryIndexWriteMemRef_unfold_leftFst`
the memory-receiver ones.

The value snapshot is what keeps evaluation order: aliasing the receiver runs
its index, and that may mutate what the value reads. Dropping it reproduces the
Lean model's `fieldWrite_not_sound` counterexample. Capturing a *reference*
source is not needed for order (a reference is bound, not read) but is done
anyway, so that one rule covers a receiver-level source of any shape; the
redundant alias collapses in one rebind step.

### Instances of Rule 2

**`storageIndexWriteNonSimpleIndexCapture`** — `sp1[nse] = e`, primitive `e`

    nse => ⟨ π  T_{e} rv = e; T_{sp1} storage sp = sp1; T pv = nse; sp[pv] = rv; ω ⟩ φ
    -----------------------------------------------------------------------------------
                          => ⟨ π  sp1[nse] = e; ω ⟩ φ

The snapshot is what makes `xs[i++] = i;` write the *old* `i`. Dropping it
closes
a proof of `xs[0] == 1` where the EVM writes `0`; the witnesses are
`testStorageIndexWriteImpureIndexPrimitiveRhs` and its memory and depth-2 twins
in `TestSuite.sol`.

**`storageIndexWriteStorageRefNonSimpleIndexCapture`** — `sp1[nse] = src`, and
likewise `memoryToStorageIndexNonSimpleIndexCapture` and
`memoryIndexWriteMemRefNonSimpleIndexCapture`

    nse => ⟨ π  T_{src} storage rv = src; T_{sp1} storage sp = sp1; T pv = nse;
                sp[pv] = rv; ω ⟩ φ
    ----------------------------------------------------------------------------
                     => ⟨ π  sp1[nse] = src; ω ⟩ φ

Under `indexWriteCapture:allAtOnce` these five rules and the five Rule-1 ones
above are replaced by five `…CaptureAll` rules, one per right-hand-side kind.

### Instances of Rule 3

**`fieldWriteValueRhsCapture`** / **`indexWriteValueRhsCapture`** capture a
nonsimple primitive right-hand side at a simple target; they are data-location
neutral (`Path[simple]`), so one rule serves storage and memory.
**`storageFieldWriteCaptureSrc`** / **`storageIndexWriteStorageRefRhsCapture`**
and their `memory…` twins capture a complex reference source.
**`storageRootWriteValueRhsCapture`** is the root-target form, which has no
receiver and no index.

### Why the index may carry a side effect

`Path[…]` classifies `persons[acc.balance++]` as an ordinary complex path — the
sort places no purity requirement on an index. Soundness comes from the capture
order instead: every rule above captures the right-hand side before it evaluates
the receiver, and the receiver before the index. The one rule that emits a
side-effect-capable path twice is `ternaryToIfStorage`
(`path = se ? e1 : e2` ⟹ `if (se) path = e1; else path = e2;`); its two
occurrences sit in mutually exclusive branches, so the path is still resolved
exactly once per trace, and its guard is a `SimpleExpression`.

No complex path SV is ever lowered into a term or an update — only
`Path[…,simple]` SVs occur there, and a simple path is a root, so the
"matches a `Path` SV ⟹ lowerable" direction the terminals rely on still holds.

### Standalone receiver / delete-target simplifications

These exist because their active statement is not assignment-shaped
(`op(nsp,fld) = se`); it is `delete`, `push`, `pop`, or a push-return
binding. The receiver *is* an `op(nsp, ·)` in each of them — what differs is
that nothing is assigned to it. Each replaces `nsp` with a fresh
`storage sp = nsp;` capture, then continues against `sp`.

**`storageFieldDelete_unfold_leftFst`** — `delete nsp.fld;`

    nsp => ⟨ π  storage sp = nsp; delete sp.fld; ω ⟩ φ
    ------------------------------------------------
            => ⟨ π  delete nsp.fld; ω ⟩ φ

**`storageIndexDelete_unfold_leftFst`** — `delete nsp[ie];`

    nsp => ⟨ π  storage sp = nsp; delete sp[ie]; ω ⟩ φ
    -------------------------------------------------
            => ⟨ π  delete nsp[ie]; ω ⟩ φ

A delete keeps its last selector and aliases only the receiver, like
every other left-hand side: `delete lsv;` on a local storage pointer is
not Solidity, so there is no whole-target alias.

**`storagePushValue_unfold_leftFstReceiver`** — `nsp.push(e);`

    nsp => ⟨ π  storage sp = nsp; sp.push(e); ω ⟩ φ
    -----------------------------------------------
            => ⟨ π  nsp.push(e); ω ⟩ φ

**`storagePush_unfold_leftFstReceiver`** — `nsp.push();`

    nsp => ⟨ π  storage sp = nsp; sp.push(); ω ⟩ φ
    ----------------------------------------------
            => ⟨ π  nsp.push(); ω ⟩ φ

**`storagePop_unfold_leftFstReceiver`** — `nsp.pop();`

    nsp => ⟨ π  storage sp = nsp; sp.pop(); ω ⟩ φ
    ---------------------------------------------
            => ⟨ π  nsp.pop(); ω ⟩ φ

**`storageLocalRootPush_unfold_leftFstReceiver`** — `lsv = nsp.push();`

    nsp => ⟨ π  storage sp = nsp; lsv = sp.push(); ω ⟩ φ
    ---------------------------------------------------
            => ⟨ π  lsv = nsp.push(); ω ⟩ φ

## 6. Step 3: Generating an Update

Once every constituent is simple, the statement is replaced by a
parallel update on `⟨ π ω ⟩ φ`. Format below: source statement ⇝
emitted update.

### Local-storage declarations

- `storageLocalDeclInitDrop` (the `storage` keyword in the pattern is
  matched; sibling rules `localValueDeclInitDrop` /
  `memoryLocalDeclInitDrop` handle the other locations; the declared
  variable is registered as a program variable)

      T storage lsv = path;
      ⇝  lsv = path;                        (lsv not occurring in path)

- `storageLocalDeclSkip`

      T storage lsv ;
      ⇝  (skip; produces no update)        (lsv not used)

### Field write

- `storageFieldWriteSave`

      sp.fld = se
      ⇝  { storage := save(storage, sp · fld, se) }

- `storageFieldWriteCopySource` (RHS is itself a storage path —
  the *value at* `sp2` is copied, not the path; sort-free
  `find<[StValue]>`, like `storageRootWriteCopySource`)

      sp1.fld = sp2
      ⇝  { storage := copyAt(storage, sp1 · fld, find<[StValue]>(storage, sp2)) }

  Every `*CopySource` / `…StoreRoot` rule copies through `copyAt` (§3), so
  a mapping member of the target is never overwritten. solc ≥ 0.7 rejects
  a copy whose type carries a mapping and both front ends enforce that at
  parse time (`ParserUtils.parseAssignmentMaybe`,
  `StorageReferenceTypes.containsMapping`); the calculus nevertheless
  gives the shape the mapping-preserving meaning. Storage-pointer rebinds
  (`storageLocalRootRebind`) and `delete` (mapping-preserving, §6) are
  unaffected.

### Root write (whole struct / primitive at a root)

- `storageRootWriteStore`

      gsp = se
      ⇝  { storage := save(storage, gsp, se) }

- `storageRootWriteCopySource` (one rule for primitive and struct
  sources — `find<[StValue]>` is sort-free, the sort is resolved on read)

      gsp = sp
      ⇝  { storage := copyAt(storage, gsp, find<[StValue]>(storage, sp)) }

- `storageLocalRootRebind` (rebinds a local storage reference; does
  **not** copy)

      lsv = sp
      ⇝  { lsv := sp }

  Note: Since both `lsv` and `sp` are now `List`-typed paths, this is
  a direct assignment without any wrapping.

### Field / root read

- `storageFieldReadFind`

      v = sp.fld
      ⇝  { v := find(storage, sp · fld) }

- `storageRootReadSelect`

      v = sp
      ⇝  { v := find(storage, sp) }

- `storageFieldReadBindLocalRoot`

      lsv = sp.b
      ⇝  { lsv := sp · b }

- `storageFieldReadStoreRoot`

      gsp = sp.b
      ⇝  { storage := copyAt(storage, gsp, find(storage, sp · b)) }

### Delete

`delete` writes a *reset value* that is lazy for structs, so that **mapping
members survive** — Solidity's `delete` does not clear mappings (they cannot be
enumerated).

- `storageRootDelete` (a contract root)

      delete gsp
      ⇝  { storage := delAt(storage, gsp) }

- `storageFieldDelete`

      delete sp.fld
      ⇝  { storage := delAt(storage, sp · fld) }

- `delAt(storage, p)` leaves the reset lvalue unresolved: on read it becomes
  `default` for a primitive, and the lazy marker `delNode(…)` for a struct.
  Deferring the choice rather than writing the resolved value is what lets the
  rule stay sort-free — the sort arrives with the read.
- Reading a member `f` of `delNode(v)`:
    - `f` is a **mapping** member → read the original (`v`), i.e. preserved;
    - `f` is a **struct/array** member → recurse (nested mappings survive too);
    - `f` is a **primitive** member → `default`.

The field variant (`delete sp.fld`) applies the same `delAt` scheme at the
fully-qualified path, so deleting a struct field also preserves its mappings.
The index variant (`delete sp[ie]`) resets that single entry/element outright,
mapping members included — `{ storage := save(storage, sp · at(ie), defVal) }`.

### Mapping index access  (when `mapping(sp)`)

- `storageIndexWriteMappingSave`

      sp[ie] = se
      ⇝  { storage := save(storage, sp · at(ie), se) }

- `storageIndexWriteMappingCopySource`

      sp1[ie] = sp2
      ⇝  { storage := copyAt(storage, sp1 · at(ie), find<[StValue]>(storage, sp2)) }

- `storageIndexReadMappingFind`

      v = sp[ie]
      ⇝  { v := find(storage, sp · at(ie)) }

- `storageIndexReadMappingBindLocalRoot`

      lsv = sp[ie]
      ⇝  { lsv := sp · at(ie) }

- `storageIndexReadMappingStoreRoot`

      gsp = sp[ie]
      ⇝  { storage := copyAt(storage, gsp, find(storage, sp · at(ie))) }

### Array index access  (when `array(sp)`, with `ℓ = find(storage, sp · length)`)

Each array rule branches on bounds. Out-of-bounds goes to
`revert();` (consumed by §8).

- `storageIndexWriteArraySave`

      sp[ie] = se
      ⇝  if 0 ≤ ie < ℓ : { storage := save(storage, sp · at(ie), se) }
         else         : revert();

- `storageIndexWriteArrayCopySource`

      sp1[ie] = sp2
      ⇝  if 0 ≤ ie < ℓ : { storage := copyAt(storage, sp1 · at(ie),
                                            find<[StValue]>(storage, sp2)) }
         else         : revert();

- `storageIndexReadArrayFind`

      v = sp[ie]
      ⇝  if 0 ≤ ie < ℓ : { v := find(storage, sp · at(ie)) }
         else         : revert();

- `storageIndexReadArrayBindLocalRoot`

      lsv = sp[ie]
      ⇝  if 0 ≤ ie < ℓ : { lsv := sp · at(ie) }
         else         : revert();

- `storageIndexReadArrayStoreRoot`

      gsp = sp[ie]
      ⇝  if 0 ≤ ie < ℓ : { storage := copyAt(storage, gsp,
                                             find(storage, sp · at(ie))) }
         else         : revert();

### Push / pop  (let `n = find(storage, sp · length)` and `ℓ` likewise)

- `storagePushLhsToPushValue` (desugar push-return assignment into
  push-with-value)

      path.push() = se
      ⇝  path.push(se);

- `storagePushValueSave`

      sp.push(se)
      ⇝  { storage := save( save(storage, sp · at(n), se),
                            sp · length, n + 1 ) }

- `storagePushValueCopySource` (sort-free `find<[StValue]>` for the
  copied value, like `storageFieldWriteCopySource`)

      sp1.push(sp2)
      ⇝  { storage := save( copyAt(storage, sp1 · at(n),
                                   find<[StValue]>(storage, sp2)),
                            sp1 · length, n + 1 ) }

- `storagePushLengthSave` (zero-arg push: append the default-valued
  slot, return nothing — the appended slot is cleared with `delAt`, so a
  struct element's mapping members survive being pushed over)

      sp.push();
      ⇝  { storage := save( delAt(storage, sp · at(n)),
                            sp · length, n + 1 ) }

- `storageLocalRootPushBind` (zero-arg push whose returned slot is
  captured into a local reference)

      lsv = sp.push();
      ⇝  { storage := save(storage, sp · length, n + 1)
           || lsv := sp · at(n) }

- `storagePopSave` (clears the popped slot with `delAt`, which is
  mapping-preserving in the same way `delete` is, so a mapping
  nested in the popped element survives a `pop()` — and survives a subsequent
  re-`push()` too, since `push` clears with the very same marker)

      sp.pop();
      ⇝  if ℓ > 0 : { storage := save( delAt(storage, sp · at(ℓ - 1)),
                                       sp · length, ℓ - 1 ) }
         else    : revert();

## 7. Compound Updates

Compound storage updates such as `s.x += e`, `s.a++`, etc., are
handled by dedicated terminal rules that read, compute and write in
one update (`storage{Root,Field}{Add,Sub,Mul,Div,Mod}Assign`, the
`storageIncDec` family); complex receivers unfold first through their
`_unfold_leftFst` twins, exactly as for plain assignments. The indexed
terminals come in a mapping and an array form, split by the receiver's
sort like the plain index rules: `storageIndexMapping…` rewrites to the
single update, `storageIndexArray…` carries the same `0 ≤ ie < ℓ` /
`revert();` branch pair as `storageIndexWriteArraySave`, so
`values[i] += 1` on an out-of-range `i` reverts instead of writing.

## 8. Abrupt Termination

A `revert();` aborts the entire transaction, so `π` and `ω` are
irrelevant. These are the only two storage rules that differ between
modalities:

- `revertDiamond`:   `=> ⟨ π revert(); ω ⟩ φ`   ⇝  `=> ⊥`
- `revertBox`:       `=> [ π revert(); ω ] φ`   ⇝  `=> ⊤`

## 8b. Array lengths are non-negative (`sizeNotNegative`)

Symbolic execution starts from an unconstrained `storage`, so nothing
is known about a cell the proof has not written — in particular a
dynamic array's length could be negative. `sizeNotNegative`
(`structRules.key`) closes exactly that gap, mirroring Java KeY's
`arrayLengthNotNegative` (`heapRules.key` in `key.core`):

    sizeNotNegative {
        \find(selectSt<[int]>(st, size))
        \sameUpdateLevel
        \add(0 <= selectSt<[int]>(st, size) ==>)
        \heuristics(inReachableStateImplication)
    };

One static rule suffices even though a schematic trigger on paths does
not exist: `consr` is a defined function, so a `\find` on
`find<[int]>(storage, consr(xs, size))` never matches — but
normalization does not stop at `cons`-normal paths. `findDefinitionCons`
peels every `find<[alpha]>(st, cons(a, flds))` down to a
`selectSt<[alpha]>(st, a)` leaf, so each length read the push/pop/index
rules emit reaches the stable form `selectSt<[int]>(st, size)` with the
global `\unique Field size` in matchable position — for a root array,
a struct member, or a mapping value alike. The normalization rules all
cost far less than the taclet's +100, so the trigger form is always
reached before the rule is selected.

**Soundness** is an external-invariant argument: `size` cells are
written only by the push/pop rules — `n+1`, and `n-1` guarded by
`0 < n`; `.length` is not assignable in Solidity — so in every
reachable storage a length cell is non-negative (the Lean
formalization's `wellTypedStorageB`, preserved by
`TypeSoundness.execBlock_preserves_wellTyped`). The axiom is also
consistent inside the calculus: `selectSt<[int]>(mtSt, size)` and the
`delNode`/`delAt` reads reduce to `defaultValue<[int]> = 0`.

The rule is always on. Its ruleset `inReachableStateImplication`
(declared in `ruleSetDeclarations.key`) is costed at +100 and filtered
by `NonDuplicateAppModPositionFeature` in `SolidityDLStrategy`'s cost
and approval dispatchers, so the add-only rule fires once per distinct
length term and cannot loop when formulas move.

The motivating case is `values.push(); values.pop();`
(`storagePopUnknownLength`): `storagePopSave`'s `"empty"` branch is
`!(0 < n+1) -> ⟨revert⟩false` with
`n = selectSt<[int]>(selectSt<[Struct]>(storage, C$values), size)`
over the original storage, which only `0 <= n` closes. Because of this
rule, array examples no longer have to open with
`require(arr.length == N)` unless they assert an exact length.

The rule covers lengths only: nothing bounds the value of a `uint`
cell (that needs per-contract layout knowledge the calculus does not
have — a field constant like `C$total : Field` carries no declared
type), and no upper bound (`< 2^256`) is stated.

## 9. Discipline and Termination

**Pairwise disjointness.** Schema-variable kinds, the step
organization, and the `array(sp)` / `mapping(sp)` predicates together
ensure that exactly one rule applies to any storage statement.

**Termination.** The calculus terminates by the lexicographic measure

    ( #initializedStorageDeclarations,
      #complexExpressions,
      compositionDepth,
      #statements ).

- The `*DeclInitDrop` rules strictly decrease component 1.
- The unfolding, capture, and split rules strictly decrease a later
  component.
- Step-3 terminal rules decrease the number of statements.
- A `revert();` introduced on an out-of-bounds branch is immediately
  consumed by `revertDiamond` or `revertBox`, again decreasing the
  number of statements.

**Roots are bare.** `gsp` is a bare contract root (a `FieldReference`)
and `lsv` a bare local storage pointer; a final field or index segment
is always spelled out (`sp.fld`, `sp[ie]`), which is what keeps
`storageRootWriteStore` and `storageFieldWriteSave` disjoint.

## 10. Worked Examples (terse traces)

### `alice.age = ageVal;`  (single-field write)

    ⟨[ alice.age = ageVal; ]⟩ φ
    ⇝ { storage := save(storage, alice · age, ageVal) } φ

(`storageFieldWriteSave`.)

### `alice.account = acc;`  with `Account storage acc = bob.account;`

    ⟨[ Account storage acc = bob.account;
       alice.account = acc; ]⟩ φ
    ⇝ { acc := bob · account
        || storage := save(storage, alice · account,
                           find(storage, bob · account)) } φ

The write copies the value at the alias's target path, not the alias
path itself.

### `alice.account.balance = 10;`  (depth-2 field write)

    ⟨[ alice.account.balance = 10; ]⟩ φ
    ⇝ ⟨[ Account storage acc = alice.account; acc.balance = 10; ]⟩ φ
    ⇝ { acc := alice · account } ⟨[ acc.balance = 10; ]⟩ φ
    ⇝ { acc := alice · account }
      { storage := save(storage, acc · balance, 10) } φ
    ⇝ { acc := alice · account
        || storage := save(storage, alice · account · balance, 10) } φ

### `v = alice.account.balance;`  (depth-2 field read)

    ⟨[ v = alice.account.balance; ]⟩ φ
    ⇝ ⟨[ Account storage acc = alice.account; v = acc.balance; ]⟩ φ
    ⇝ { acc := alice · account } ⟨[ v = acc.balance; ]⟩ φ
    ⇝ { acc := alice · account
        || v := find(storage, alice · account · balance) } φ

### `alice.account.token.value = 5;`  (depth-3 field write)

Two captures are needed before the terminal write:

    ⇝ { aliceAcc := alice · account
        || aliceTok := alice · account · token
        || storage := save(storage,
                           alice · account · token · value, 5) } φ

### `uint v = total;`  (root read of a primitive state variable)

    ⟨[ uint v = total; ]⟩ φ
    ⇝ { v := find(storage, total) } φ

where `total` extracts to `cons(total, nil)`. Whole-struct paths cannot
be read into a location-free local; they must be copied through memory
or aliased through storage.

### `alice = pVal;`  (whole-struct write to a root)

    ⟨[ alice = pVal; ]⟩ φ
    ⇝ { storage := save(storage, alice, pVal) } φ

where `alice` extracts to `cons(alice, nil)`.

### `alice = bob;`  (whole-struct root-to-root copy)

    ⟨[ alice = bob; ]⟩ φ
    ⇝ { storage := copyAt(storage, alice, find(storage, bob)) } φ

where both `alice` and `bob` extract to single-element lists. The path
`bob` is *not* stored as the value; its struct value is read and stored,
and a mapping member of `alice` keeps its own entries (§3, `merge`).

## 11. Quick Reference: Statement → Rule

Use this when looking up which Step-3 rule fires.

**Unified path representation:** Both global roots (`gsp`) and local
storage aliases (`lsv`) are `List`-typed paths. A global root `alice`
extracts to `cons(alice, nil)`. All storage operations use `find`/`save`.

| Source statement           | Rule                                   | Update operation         |
|----------------------------|----------------------------------------|--------------------------|
| `sp.fld = se`              | `storageFieldWriteSave`                | `save`                   |
| `sp1.fld = sp2`            | `storageFieldWriteCopySource`          | `copyAt`/`find<[StValue]>` |
| `gsp = se`                 | `storageRootWriteStore`                | `save`                   |
| `gsp = sp`                 | `storageRootWriteCopySource`           | `copyAt`/`find<[StValue]>` |
| `lsv = sp`                 | `storageLocalRootRebind`               | direct assign            |
| `v = sp.fld`               | `storageFieldReadFind`                 | `find`                   |
| `v = sp`                   | `storageRootReadSelect`                | `find`                   |
| `lsv = sp.b`               | `storageFieldReadBindLocalRoot`        | direct assign            |
| `gsp = sp.b`               | `storageFieldReadStoreRoot`            | `copyAt`/`find<[StValue]>` |
| `delete gsp;`              | `storageRootDelete`                    | `delAt`                  |
| `delete sp.fld;`           | `storageFieldDelete`                   | `delAt`                  |
| `delete sp[ie];`           | `storageIndexDelete`                   | `save`/`defVal`          |
| `sp[ie] = se`  (mapping)   | `storageIndexWriteMappingSave`         | `save`                   |
| `sp1[ie] = sp2`  (mapping) | `storageIndexWriteMappingCopySource`   | `copyAt`/`find<[StValue]>` |
| `sp[ie] = mv`  (mapping)   | `memoryToStorageIndexMappingCopyRoot`  | `save`/`copyMem`         |
| `v = sp[ie]`  (mapping)    | `storageIndexReadMappingFind`          | `find`                   |
| `lsv = sp[ie]`  (mapping)  | `storageIndexReadMappingBindLocalRoot` | direct assign            |
| `gsp = sp[ie]`  (mapping)  | `storageIndexReadMappingStoreRoot`     | `copyAt`/`find<[StValue]>` |
| `sp[ie] = se`  (array)     | `storageIndexWriteArraySave`           | `save`                   |
| `sp1[ie] = sp2`  (array)   | `storageIndexWriteArrayCopySource`     | `copyAt`/`find<[StValue]>` |
| `sp[ie] = mv`  (array)     | `memoryToStorageIndexArrayCopyRoot`    | `save`/`copyMem`         |
| `v = sp[ie]`  (array)      | `storageIndexReadArrayFind`            | `find`                   |
| `lsv = sp[ie]`  (array)    | `storageIndexReadArrayBindLocalRoot`   | direct assign            |
| `gsp = sp[ie]`  (array)    | `storageIndexReadArrayStoreRoot`       | `copyAt`/`find<[StValue]>` |
| `sp.push(se);`             | `storagePushValueSave`                 | `save`                   |
| `sp1.push(sp2);`           | `storagePushValueCopySource`           | `copyAt`/`find<[StValue]>` |
| `sp.push();`               | `storagePushLengthSave`                | `save`                   |
| `lsv = sp.push();`         | `storageLocalRootPushBind`             | `save`                   |
| `path.push() = se;`        | `storagePushLhsToPushValue` (desugar)  | —                        |
| `sp.pop();`                | `storagePopSave`                       | `save`                   |
| `revert();` (in `⟨·⟩`)     | `revertDiamond`                        | —                        |
| `revert();` (in `[·]`)     | `revertBox`                            | —                        |

The memory twins of the compound-update rows (`mv.fld += se`, `++mv.fld`,
`mv[ie] += se`, …) are in `memory.md` §11b; they use `read`/`write` in place of
`find`/`save` and have no root or mapping form.
