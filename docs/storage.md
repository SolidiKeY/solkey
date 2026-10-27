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
for `delAt` through `delField<[alpha]>` on the select:

- `delAt(storage, path)` — the storage with the location at `path` reset. A
  struct or dynamic array there becomes the lazy `delNode` marker, so its mapping
  members survive; a fixed-size array becomes `delNodeFixed`, which also keeps its
  `size`; a mapping stays as it is; anything else collapses to its default. The
  choice is made on read, from the sort and the kind of the path's last field,
  through `delField<[alpha]>(st, f)` in `selectOnDelAtCons` (the counterpart of
  `selectOnSaveCons`) — so the rules that write it stay sort-free. Used by `delete` on a root or field and by `pop` to clear
  the removed slot. Array deletion resets indices inside the old valid range and preserves
  the symbolic tail beyond the old length.

  It names `storage` **once**. The equivalent `save(storage, path, <deleted
  value at path>)` names it twice, which doubles the storage term at every
  `pop`; a sequence of them then grows exponentially rather than
  linearly, which is what made `SolcArrays.pushThenPopRestoresLength` slow.

  The two deferrals meet when a sort-free copy reads a *cleared* location —
  `delete sp; gsp = sp;` and its field, root and `push` variants.
  `selectOnDelAtCons` instantiates its generic at the reader's sort, so a
  `find<[StValue]>` copy leaves `delField<[StValue]>(…)`, which neither
  the `Struct` rules nor `delFieldDefault` (`alphaPrim \extends Prim`) matches.
  `delFieldStValueCast` is the twin of `findStValueCast` for that shape: it
  pushes the read's cast inward, `cast<[alphaSt]>(delField<[StValue]>(st, f))` ⇝
  `delField<[alphaSt]>(st, f)`, so the reset resolves at the sort the read
  supplies. Same coherence assumption as `findStValueCast` — observing a
  sort-parametric family at a smaller sort is that family's smaller-sort
  instance — and it leaves the `Struct` rules and `delFieldDefault` disjoint. Pinned by `storage{Field,Root}DeleteThenCopy` and
  `storageFieldDeleteThenCopyDeep`.
- `delNodeIndex(st, i)` — the pending reset of reference element `i` of array `st`.
  Selecting a reference element under `delNode` creates this marker without branching.
  A subsequent field read checks `0 <= i < oldLength(st)`: it reads through a reset child
  inside that range and through the unchanged child outside it. Overwritten fields can
  therefore simplify before the old-length condition is expanded. `fixedDelNodeIndex`
  retains the child's array kind, and `conformsDelNodeIndexRight` reduces type preservation to
  the original child's layout atom; neither assumes zero tail data.
- `save(st, nil, v)` — **the leaf of a write, left as a term.** A struct
  written over a location keeps the location's mapping members: Solidity
  never copies a mapping. So `save` never collapses its leaf (there is no
  `save(st, nil, v) ⇝ v`); the five rules that read through it decide by the
  member's sort. A `MapField` member comes from the old value
  (`selectOnSaveEmptyMap`), a `RefField` member is again a leaf one level
  down (`selectOnSaveEmptyRef`), every primitive member comes from the
  written value (`selectOnSaveEmptyDefault`), and an `at(i)` element follows
  solc's array copy (`selectOnSaveEmptyIndexStruct`): below the written
  value's `size` it is again a leaf one level down, below the old `size` it is
  reset (`delNode`), and past both it keeps the old element — dangling data a
  later `push()` exposes. A cast of the leaf to a primitive sort is
  the cast of the written value (`saveOnEmptyPrim`, the shape
  `selectOnSaveCons` leaves at the end of a path). Every storage-to-storage
  copy therefore writes plain `save(storage, p1, find<[StValue]>(storage,
  p2))`; a memory-to-storage copy (`save(storage, p, copyMem(…))`) keeps the
  target's mappings the same way, and `selectOnCopyMem{Prim,Ref}` in
  `structMemoryRules.key` read the memory struct one selector at a time under
  that leaf. `selectStValueCast` collapses
  `cast<[alphaSt]>(selectSt<[StValue]>(st, a))`, which a sort-free copy of a
  written location reaches. Pinned by `storage/copyKeepsMapping.key` (the
  mapping half, which no `.sol` can state) and the `testCopy*` group of
  `TestSuite.sol`.
- `defVal` — a location reset outright, mapping members included. The
  sort-free twin of `default`. Used by the memory index delete; in storage
  every `delete` goes through `delAt`. Sorted `Prim`, so it is both an
  `StValue` and a `MemValue`.

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

**Rule 1 — field write, receiver nonsimple.** Capture the right-hand side and
alias the receiver; a field name is always simple, so nothing else needs
capturing.

    nsp => ⟨ π  T_{e} rv = e; T_{nsp} sp = nsp; sp.fld = rv; ω ⟩ φ
    --------------------------------------------------------------
                  => ⟨ π  nsp.fld = e; ω ⟩ φ

**Rule 2 — index write, receiver or index nonsimple.** Two rules per
right-hand-side kind capture all three constituents at once: `…ComplexRecv`
with `p : Path[…,complex]` and any index, and `…NonSimpleIndex` with
`p : Path[…,simple]` and `ie : NonSimpleExpression`. Together they cover
exactly "receiver complex or index nonsimple", so neither fires on a fully
simple `sp[se] = e` and re-matches its own output. The receiver is
snapshotted even when it is already a root or an alias: the index may reassign
the local storage pointer the receiver reads, and the write must land where the
receiver pointed *before* the index ran.

    ⟨ π  T_{e} rv = e; T_{p} sp = p; T_{ie} pv = ie; sp[pv] = rv; ω ⟩ φ
    -------------------------------------------------------------------
                    => ⟨ π  p[ie] = e; ω ⟩ φ

**Rule 3 — receiver and index simple, right-hand side nonsimple.**

    nse => ⟨ π  T_{nse} rv = nse; sp[se] = rv; ω ⟩ φ
    ------------------------------------------------
            => ⟨ π  sp[se] = nse; ω ⟩ φ

The partition is disjoint: Rule 1 needs a `Path[…,complex]` field receiver,
Rule 2 an index write with a complex receiver or a nonsimple index, Rule 3 `Path[…,simple]`
with a `SimpleExpression` index (or a field) and a right-hand side the
terminals reject.

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

That table is the whole `kindof` dispatch; it is why Rules 1 and 2 have five
instances each (storage receiver × three kinds, memory receiver × two kinds —
a memory location cannot hold a storage reference), and Rule 3 six.

### Instances of Rule 1

**`storageFieldWrite_unfold_leftFst`** — `nsp.fld = e`, primitive `e`

    nsp => ⟨ π  T_{e} rv = e; storage sp = nsp; sp.fld = rv; ω ⟩ φ
    ------------------------------------------------------------
                => ⟨ π  nsp.fld = e; ω ⟩ φ

**`storageFieldWriteStorageRef_unfold_leftFst`** — `nsp.fld = src`,
storage `src`,
and likewise `memoryToStorageField_unfold_leftFst` (memory `src`)

    nsp => ⟨ π  T_{src} storage rv = src; storage sp = nsp; sp.fld = rv; ω ⟩ φ
    ------------------------------------------------------------------------
                    => ⟨ π  nsp.fld = src; ω ⟩ φ

`memoryFieldWrite_unfold_leftFst` / `memoryFieldWriteMemRef_unfold_leftFst` are
the memory-receiver ones.

The value snapshot is what keeps evaluation order: aliasing the receiver runs
its index, and that may mutate what the value reads. Dropping it reproduces the
Lean model's `fieldWrite_not_sound` counterexample. Capturing a *reference*
source is not needed for order (a reference is bound, not read) but is done
anyway, so that one rule covers a receiver-level source of any shape; the
redundant alias collapses in one rebind step.

### Instances of Rule 2

**`storageIndexWriteCaptureAll{ComplexRecv,NonSimpleIndex}`** — `p[ie] = e`,
primitive `e`, and `memoryIndexWriteCaptureAll…` its memory twins

    ⟨ π  T_{e} rv = e; T_{p} storage sp = p; T_{ie} pv = ie; sp[pv] = rv; ω ⟩ φ
    ---------------------------------------------------------------------------
                    => ⟨ π  p[ie] = e; ω ⟩ φ

The snapshot is what makes `xs[i++] = i;` write the *old* `i`. Dropping it
closes
a proof of `xs[0] == 1` where the EVM writes `0`; the witnesses are
`testStorageIndexWriteImpureIndexPrimitiveRhs` and its memory and depth-2 twins
in `TestSuite.sol`.

**`storageIndexWriteStorageRefCaptureAll…`** — `p[ie] = src`, and likewise
`memoryToStorageIndexCaptureAll…` and `memoryIndexWriteMemRefCaptureAll…`

    ⟨ π  T_{src} storage rv = src; T_{p} storage sp = p; T_{ie} pv = ie;
         sp[pv] = rv; ω ⟩ φ
    --------------------------------------------------------------------
                    => ⟨ π  p[ie] = src; ω ⟩ φ

When only the receiver is nonsimple, the index temporary `pv` of `…ComplexRecv`
is redundant: a few extra nodes.

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
      ⇝  { storage := save(storage, sp1 · fld, find<[StValue]>(storage, sp2)) }

  Every `*CopySource` / `…StoreRoot` rule copies through `save`, whose leaf
  is read by member sort (§3), so a mapping member of the target is never
  overwritten. solc ≥ 0.7 rejects
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
      ⇝  { storage := save(storage, gsp, find<[StValue]>(storage, sp)) }

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
      ⇝  { storage := save(storage, gsp, find(storage, sp · b)) }

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

The field variant (`delete sp.fld`) and the index variant (`delete sp[ie]`)
apply the same `delAt` scheme at the fully-qualified path, so deleting a
struct field or a struct element also preserves its mappings.

### Mapping index access  (when `mapping(sp)`)

- `storageIndexWriteMappingSave`

      sp[ie] = se
      ⇝  { storage := save(storage, sp · at(ie), se) }

- `storageIndexWriteMappingCopySource`

      sp1[ie] = sp2
      ⇝  { storage := save(storage, sp1 · at(ie), find<[StValue]>(storage, sp2)) }

- `storageIndexReadMappingFind`

      v = sp[ie]
      ⇝  { v := find(storage, sp · at(ie)) }

- `storageIndexReadMappingBindLocalRoot`

      lsv = sp[ie]
      ⇝  { lsv := sp · at(ie) }

- `storageIndexReadMappingStoreRoot`

      gsp = sp[ie]
      ⇝  { storage := save(storage, gsp, find(storage, sp · at(ie))) }

### Array index access  (when `array(sp)`, with `ℓ = find(storage, sp · length)`)

Each array rule branches on bounds. Out-of-bounds goes to
`revert();` (consumed by §8).

- `storageIndexWriteArraySave`

      sp[ie] = se
      ⇝  if 0 ≤ ie < ℓ : { storage := save(storage, sp · at(ie), se) }
         else         : revert();

- `storageIndexWriteArrayCopySource`

      sp1[ie] = sp2
      ⇝  if 0 ≤ ie < ℓ : { storage := save(storage, sp1 · at(ie),
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
      ⇝  if 0 ≤ ie < ℓ : { storage := save(storage, gsp,
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
      ⇝  { storage := save( save(storage, sp1 · at(n),
                                   find<[StValue]>(storage, sp2)),
                            sp1 · length, n + 1 ) }

- `storagePushLengthSave` and `storagePushLengthSaveReferenceElement` (zero-arg push
  onto primitive or reference-element arrays: grow only the length)

      sp.push();
      ⇝  { storage := save(storage, sp · length, n + 1) }

  The appended slot retains its previous symbolic contents. Earlier writes and clears
  remain visible; no fresh value or default value is introduced.

- `storageLocalRootPushBind` (capture the appended slot as a storage reference)

      lsv = sp.push();
      ⇝  { storage := save(storage, sp · length, n + 1)
           || lsv := sp · at(n) }

- `storageLocalPrimitivePushBind` (read the preserved slot into a primitive local)

      v = sp.push();
      ⇝  { storage := save(storage, sp · length, n + 1)
           || v := find(storage, sp · at(n)) }

  Compound assignments and increments on `sp.push()` likewise grow the length before
  reading and updating the preserved appended slot.

- `storagePushLengthSaveMappingElement` and `storageLocalRootPushBindMappingElement`
  grow mapping arrays without clearing their entries. A returned mapping reference uses
  `atMap(n)`.

      sp.push();
      ⇝  { storage := save(storage, sp · length, n + 1) }

The model assumes no clean-tail invariant at function entry. Default-value assertions
therefore need a justified clear/write or a precondition. The compiler-specific dangling
examples in `solc/StorageDangling.sol` exercise preserved tail data; Solidity leaves these
executions undefined.

- `storagePopSave` (clears the popped slot with `delAt`, which is
  mapping-preserving in the same way `delete` is, so a mapping
  nested in the popped element survives a `pop()` — and survives a subsequent
  re-`push()` too, since `push` preserves the slot)

      sp.pop();
      ⇝  if ℓ > 0 : { storage := save( delAt(storage, sp · at(ℓ - 1)),
                                       sp · length, ℓ - 1 ) }
         else    : revert();

## 7. Compound Updates

Compound storage updates such as `s.x += e`, `s.a++`, etc., are
handled by dedicated terminal rules that read, compute and write in
one update (`storage{Root,Field}{Add,Sub,Mul,Div,Mod}Assign`, the
`storage…{Pre,Post}{in,de}crement` rules); complex receivers unfold first through their
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

## 8b. Array lengths are non-negative (the layout invariant, not an axiom)

Symbolic execution starts from an unconstrained `storage`, so nothing is known about a cell the
proof has not written — in particular a dynamic array's length could be negative. That gap is
closed by the layout atoms of §8c, not by an axiom: `conforms(emptyOf<[T[]]>, st) ==>` gives
`0 <= |st|` through `conformsDynSize`, and `conforms(emptyOf<[T[n]]>, st) ==>` rewrites `|st|` to
`n` through `conformsFixedSize`. Every generated obligation of a contract with an array assumes
`layoutInv(storage)`, and a loop that writes storage carries it in its invariant, so each
length the push/pop/index rules read is reached by one of these triggers.

An earlier axiom, `sizeNotNegative`, added `0 <= selectSt<[int]>(st, size)` for every `st`,
after Java KeY's `arrayLengthNotNegative`. It was inconsistent: `save(st, cons(size, nil), -1)`
is a struct whose length is `-1`, so `!(selectSt<[int]>(st, size) = -1)` was provable
(`unsound/sizeNotNegativeAnyStruct.key`). Java KeY's rule is sound because `length` is a
function of the object, not a heap cell. Removing the axiom changed no closed proof in
`TestSuite.sol`, `contracts/` or the benchmark.

The motivating case is `values.push(); values.pop();` (`storagePopUnknownLength`):
`storagePopSave`'s `"empty"` branch is `!(0 < n+1) -> ⟨revert⟩false` with
`n = |storage[C$values]|` over the entry storage, which `conformsDynSize` closes. Array examples
need not open with `require(arr.length == N)` unless they assert an exact length.

Nothing bounds the value of a `uint` cell (that needs per-contract layout knowledge the calculus
does not have — a field constant like `C$total : Field` carries no declared type), and no upper
bound (`< 2^256`) is stated.

## 8c. Fixed-size arrays have their declared length (`layoutInv`, types as sorts)

A fixed-size array has no length cell in the EVM; its length is a property of the declaration.
The calculus keeps that knowledge in two places, neither of which touches a read or a write:
the **typed storage** a constructor starts from, and the **layout invariant**
a function assumes on entry and establishes on exit.

**Types are sorts.** Every storage type with structure has a sort, a subsort of one of four
markers below `Struct` (`structHeader.key`):

| Solidity type | Sort | Marker |
|---|---|---|
| `T[n]` | `T[n]` (`ArraySort`, element sort and length) | `FixedArray` |
| `T[]` | `T[]` (`DynamicArraySort`) | `DynamicArray` |
| `mapping(K => V)` | `mapping<[K, V]>` (`MappingSort`) | `Mapping` |
| `struct S` of contract `C` | `C$S` (`StructSort`, the declaration) | `StructType` |

A Solidity type reads outside-in, so `uint[2][3]` is three `uint[2]` rows, the sort `int[2][3]`.
Any other type is a leaf. `TypeSorts` maps a type to its sort; the parser registers each struct's
sort when it defines the struct, and builds an array or mapping sort on first mention, so a
`.key` problem or a saved proof can write `emptyOf<[int[2][]]>` or
`emptyOf<[mapping<[int, int[3]]>]>` directly (`sortId` takes `[n]` and `[]` suffixes).

**The empty node of a type**, `emptyOf<[T]>`, is the value that carries `T`: it is what a
constructor's storage starts from, what a memory identity holds, and the first argument of the
layout predicate. A type is *trivial* when every node conforms to it whatever it holds: a leaf, a
mapping of trivial values, a struct of trivial members. The node of a trivial type is `mtSt`.
`emptyOf` is `\unique`, as the shape constructors it replaces were: the empty nodes of two
types are different values, and different from `mtSt`. Without that, a case split on
`emptyOf<[int[]]> = emptyOf<[int[][]]>` assumes one node has both types. The vocabulary:

    \unique alpha emptyOf<[alpha]> -- the empty node of the type alpha
    conforms(Struct, Struct)       -- "this node holds a value of the type of this empty node"
    layoutInv(Struct)              -- conforms of every declared root, generated per contract
    fixed(Struct)                  -- "this node is a fixed-size array", decided by its type
    Struct fieldType(MemberField)  -- the empty node of a member's declared type, = #typeOf(m)

Every field constant is a `TypedField` that remembers the member's resolved type, and
`fieldTypeDef` turns it into its empty node through the transformer `#typeOf`. Four more
transformers step through a sort: `#elemOf` (element or value), `#memberOf(t, m)` (the member
`m`), `#lengthOf` (the `n` of `T[n]`) and `#structConforms` (one atom per non-trivial member).

**The predicate.** `conforms` is defined only through `selectSt`; `save`, `delAt` and `delNode` are
untouched. With `|st|` for `selectSt<[int]>(st, size)`, `st[a]` for `selectSt<[Struct]>(st, a)` and
`⟦T⟧` for the empty node of `T`:

    conforms(mtSt,        st)  =  true
    conforms(⟦T[n]⟧,      st)  =   fixed(st) & |st| = n
                                   & forall int i.  conforms(⟦T⟧, st[at(i)]) & conforms(⟦T⟧, st[atMap(i)])
    conforms(⟦T[]⟧,       st)  =  !fixed(st) & 0 <= |st|
                                   & forall int i.  conforms(⟦T⟧, st[at(i)]) & conforms(⟦T⟧, st[atMap(i)])
    conforms(⟦mapping(K => V)⟧, st)  =  forall Prim k.  conforms(⟦V⟧, st[at(k)])
    conforms(⟦C$S⟧,       st)  =  /\ over the members m of S : conforms(fieldType(m), st[m])
    layoutInv(s)               =  /\ over the roots m of C    : conforms(fieldType(m), s[m])

(An array element that is a mapping is selected with `atMap`, everything else with `at`, so
both selectors count as array elements; a mapping value is always `at(k)`.) The node kind is
part of conformance: a length alone does not tell a dynamic node from a fixed one. An array
index is an `int` and a mapping key any `Prim` (`at : Prim -> Field`, `atMap : int -> MapField`),
and every rule below keeps to that range, in both directions: an antecedent atom hands out
only what the succedent unfolding proves. Under an array atom the node at a key of another
sort, such as `at(TRUE)`, is unconstrained. The struct line is one generic rule,
`conformsStruct`, whose `#structConforms` reads the members off the struct's sort. The
`layoutInv` line is finite and per contract, so `SolidityProblemSynthesizer` emits it as the rule
`insertLayoutInv`, next to `insertCInv`; roots of a trivial type are left out, and
`insertLayoutInv` of a contract without arrays is `true`.

`conforms` takes the empty node rather than a sort argument (`conforms<[T]>(st)`) on purpose:
each sort instance of a parametric predicate is a distinct operator, and the term order then
cycles on a conjunction of atoms of different types (`commute_and` and `shift_paren_and`
reorder it forever). With one operator, atoms compare by their arguments, as before.

**The obligations** (`StorageLayout` builds both pieces):

    constructor:  ==> {storage := storeSt(…storeSt(mtSt, C$d1, fieldType(C$d1)) …, C$dyn, fieldType(C$dyn))}
                      [ ctor ] (layoutInv(storage) & CInv & ensures)
    function:     layoutInv(storage) & CInv & requires  ==>  [ f ] (layoutInv(storage) & CInv & ensures)

Nothing is assumed before the constructor; its start term is closed in its structure, so the
exit `layoutInv` is decided by unfolding. A function's exit obligation is what justifies the
next function's entry assumption. A loop that writes storage anonymises it, so
`LoopInvariantCondition` conjoins `layoutInv(storage)` to every such loop invariant, as KeY
does with `wellFormed(heap)`.

**How the rules use it.** Three families, by where a `conforms` atom sits:

- *An empty node read* (`selectOnType*`): `|⟦T[n]⟧| ⇝ n`, `|⟦T[]⟧| ⇝ 0`, `|⟦mapping⟧| ⇝ 0`,
  an element or mapping value is `#elemOf` of the node, a struct member `#memberOf`, and a
  primitive read is its default. The type at a path `p` is `find<[Struct]>(t, p)`, read one
  selector at a time (`findOnEmptyOf`, no `isEmpty` split), and anything below a leaf is
  `mtSt` (`findOnEmptyStorage`). The empty nodes of different types are distinct constants,
  sound because `Struct` has no extensionality rule. Root reads on the `storeSt` chain need
  nothing new (`selectOnStore`).
- *Antecedent atoms are consumed by triggers, never unfolded.* A read on symbolic storage
  normalises to a `selectSt` chain rooted at `storage` and stops; the atom on exactly that
  chain answers it. `conformsFixedSize` rewrites `|st|` to `n` under `conforms(⟦T[n]⟧, st) ==>`;
  `conformsDynSize` adds `0 <= |st|`; `conforms{Fixed,Dyn}Elem[Map]` add the element's atom when
  `st[at(i)]` or `st[atMap(i)]` appears for an `int` index `i`, and `conformsMapElem` when `st[at(k)]`
  appears for any `Prim` key `k`; `conformsFixedIsFixed` / `conformsDynIsNotFixed` rewrite `fixed(st)`.
  The generic sorts `alphaFix`, `alphaDyn`, `alphaMap`, `alphaStr` (bounded by the markers) pick
  the rule by the sort of the atom's empty node. So a nested length such as `rows[i].length` for
  `uint[3][] rows` is two trigger steps. They are `\assumes` rewrite taclets with
  `\sameUpdateLevel`, the add-only ones costed in `inReachableStateImplication`, and they match syntactically: after `d2 = d1` the read `|storage'[d2]|`
  goes through `selectOnSaveCons`, `saveOnEmptyPrim` and `findStValueCast` to `|storage[d1]|`,
  a select on the *entry* storage, which laziness keeps in the atom's normal form.
- *Succedent atoms are peeled off writes, then unfolded.* `conformsSaveRight` turns
  `conforms(t, save(st, p, v))` into `conforms(t, st) & conformsSaveAt(t, st, p, v, last(p))`, and
  `conformsSaveAt{Size,Index,MapIndex,Member}` dispatch on the evaluated last selector of `p`
  (`size`, `at(k)`, `atMap(i)`, a member field; a concrete path ends in one of them), so the
  length branch's `firsts(p)` is built only for a length write:
  - `p` ends in `size` (a length write, at any depth): `0 <= cast<[int]>(v)` and
    `!fixed(find<[Struct]>(st, firsts(p)))`: the written node is a dynamic array, and the
    write changes no element and no node kind;
  - otherwise `conforms(find<[Struct]>(t, p), save(find<[Struct]>(st, p), nil, v))`: the type at
    `p` is a read of the empty node `t` along `p`, and the written node is a merge, so the leaf
    keeps the target's own fixed length.

  The length branch is needed because the type at a path ending in `size` is a leaf, which
  would drop the bound and prove `conforms(⟦T[]⟧, save(st, cons(size, nil), -1))`.
  `conformsSaveLengthRight` (ruleset `concrete`, so it wins over `conformsSaveRight`) is the root-length
  case at a dynamic type, `conforms(⟦T[]⟧, save(st, cons(size, nil), n)) ⇝ conforms(⟦T[]⟧, st) & 0 <= n`,
  where the kind follows from the atom itself. `conformsStoreRight` turns
  `conforms(t, storeSt(st, a, v))` into `!(a = size) & conforms(t, st) & conforms(t[a], cast<[Struct]>(v))`:
  a store never writes a length. `conformsDelAtRight` drops a reset, owing `!fixed` of the node
  whose length it resets when the path ends in `size` (the length becomes 0 and the node keeps
  its kind, so only a dynamic array still conforms); `conformsDelNodeRight` drops a reset;
  `conformsDelNodeFixedRight` drops one only at a `T[n]` type (a fixed-node reset is
  `fixed`, so it does not conform to `T[]`); `conformsDelNodeIndexRight` checks the original
  selected child; `conformsEmpty` and `conformsSelf` close. `conforms{Fixed,Dyn,Map}Right` (ruleset `conformsUnfold`,
  costed above the triggers so an atom that reduces to an entry atom closes first) unfold a
  leaf-of-a-write by the definition above: `conformsFixedRight` owes `fixed(st)`, `|st| = n` and the
  element atoms at `at(skx)` and `atMap(skx)`, `conformsDynRight` owes `!fixed(st)`, `0 <= |st|` and
  the same, with `skx` a fresh `int` (`\skolemTerm int`), and `conformsMapRight` owes the value atom
  at `at(skp)` with `skp` a fresh `Prim` (`\skolemTerm Prim`), since `conformsMapElem` answers at
  every `Prim` key, `bool` ones included.

  Each counterexample to a weaker form stays open in
  `keyext.solidity.core/src/test/resources/org/key_project/solidity/examples/unsound/`
  (`RulesTest#unsoundWfProblemStaysOpen`): a node of the other kind
  (`conforms{Fixed,Dyn}Right*Node`, `conformsDelNodeFixedRightDynamicType`), a negative length
  (`conforms{Save,Store}RightNegative*Length`), a mapping proved at `int` keys only
  (`conformsMapRightIntKeysOnly`), and an array element read at a `bool` key
  (`conforms{Fixed,Dyn}ElemBoolIndex`). `layoutInvLengthWrites.key` and
  `layoutInvArrayElements.key` are the closing counterparts.

**The kind of a node.** The size of a reset or copied-over node depends on whether it is a
fixed-size array (keeps its declared length) or a dynamic one (reset to `0`, or to the copied
length): `|delNode(st)| ⇝ \if(fixed(st)) \then(|st|) \else(0)` (`selectStDelNodeSize`) and
`|save(st, nil, v)| ⇝ \if(fixed(st)) \then(|st|) \else(|cast<[Struct]>(v)|)`
(`selectOnSaveEmptySize`). A write never changes the kind, so `fixed` reads it off the
node's base: `fixedSave`, `fixedStore`, `fixedDelNode`, `fixedDelAt` descend;
`fixedDelNodeIndex` reads the original child's kind. `delNodeFixed`
and `⟦T[n]⟧` are fixed (`fixedTypeFixed`), `⟦T[]⟧` (`fixedTypeDyn`) and `mtSt` are not, and
`conformsFixedIsFixed` / `conformsDynIsNotFixed` decide it from the entry atom. This is what makes
`uint[40] big = uint[20] small` keep `big.length == 40` and zero `big[30]`, and
`dyn.pop()` keep the slot's `uint[3]` length. The deletion/pop terminals no longer refuse
fixed-array elements. `selectStDelNodeIndexStruct` recursively resets a reference element
only when its index lies in the old valid range; other indices retain their previous data. Copy
selectors still distinguish the source prefix and the old destination tail. The other primitive reads through a leaf write or
a reset are split by selector (`selectOnSaveEmpty{IndexPrim,MapValue,Member}`,
`selectStDelNode{Element,MapValue,Member}`) so that `size` is never defaulted.

A length is non-negative only where a `conforms` atom says so (8b); a hand-written problem that
states no `layoutInv` knows nothing about its lengths. The design and its worked examples are also in the pre-licentiate paper
(`sections/fixed-length.tex`); `contracts/FixedLengths.sol` runs them.

### Memory objects carry their type

In memory the type sits in the identity's sort argument: `typedId<[T]>(idp)` is a `\unique`
`IdentityPrim`, and every allocation rule builds `idC(typedId<[T]>(freshIdp), nil)` with `T` bound
by `\hasTypeSort(x, \sort(alphaType))` to the sort of the declared type of `x` (a memory
variable or a member), or by `\hasElementTypeSort` to that of its element, so the type survives
every write, copy and delete. Two identities of different types are different (`equalUnique`):

| Declaration | Identity |
|---|---|
| `uint[3] memory x` | `idC(typedId<[int[3]]>(idp), nil)` |
| `uint[2][3] memory y` | `idC(typedId<[int[2][3]]>(idp), nil)` |
| `new uint[2][](n)` | `idC(typedId<[int[2][]]>(idp), nil)` |
| `Triple memory t` (`uint[3] items; uint tag`) | `idC(typedId<[C$Triple]>(idp), nil)` |

A fresh node's length is `initSize`:
`init<[int]>(idC(typedId<[T]>(idp), flds), size) ⇝ |find<[Struct]>(#nodeOf(typedId<[T]>(idp)), flds)|`,
the length of the empty node of `T` at that path (`#nodeOf` is `emptyOf<[T]>`, or `mtSt` for a
trivial type), so `n` for a `T[n]` and `0` otherwise; `initElement` and `initMember` give the
other fields their defaults and, being matched on `at(pk)` and `m`, never touch `size`. `delete x`
reallocates at the declared type of `x`, a freshened member at that of the member, a freshened
element at the element type of the array. This
closes `x.length == 3`, `y[0].length == 2` and `z[i].length == 2` for the table above
(`testMemoryFixedArrayLength`, `testMemoryNestedFixedArrayLength`,
`testNewArrayOfFixedElementLength`, all EVM-checked). An identity whose prim is not
`typedId`, as in a hand-written `.key` problem, has an unknown `size`.

An unconstrained memory, such as a `uint[3] memory p` parameter, is still untyped: its
identity is a bare program variable rather than an `idC(typedId<[T]>(…), …)`, so no rule can
read its type. Giving parameters a typed identity is the natural next step.
