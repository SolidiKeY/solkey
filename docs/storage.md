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

This mirrors Solidity's evaluation order: RHS is evaluated before LHS.

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
- `lp` — simple path that is a **local** storage reference declared
  inside a function (e.g. `Person storage p`).
- `gp` — simple path that is a **global** contract root.
- `nsp` — nonsimple storage path (unresolved base, receiver, or
  index).
- `path` — plain storage path when the simple/nonsimple distinction
  is irrelevant.
- `i` — simple index expression.

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
  dynamically created array/mapping sorts) and `Prim` (`int`, `bool`,
  contract sorts). `Identity`, `List` and `Memory` are not storable.
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
- `defVal` — a location reset outright, mapping members included. The
  sort-free twin of `default`. Used by `delete sp[i]`, which resets a
  collection element rather than preserving its mapping members. Sorted
  `Prim`, so it is both an `StValue` and a `MemValue` and serves memory too.

### Field selectors

Field selectors are not a flat sort. Every field constant is stamped at parse
time (`SolJSONParser#fieldSortFor`) with exactly one sub-sort of `Field`, so a
rule can say which kind of member it applies to instead of matching all of them:

| Sub-sort | Member kind | `delete` behaviour |
|---|---|---|
| `MapField` | mapping | entries preserved |
| `IdField` | struct / array reference | recursed into |
| `PrimField` | value | reset to its default |

The partition is total, so the base `Field` sort means "any field" and nothing
else. `at(i)` stays a plain `Field`: an index's element sort depends on the
container, not on the index, so it cannot be stamped this way.

**Note:** Both global roots (like `alice`) and local storage aliases
(like `lp`) are represented as `List`-typed paths. A global root
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
the base operations `path.a` and `path[e]`.

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

**`storageFieldRead_unfold_rightFst`** — `lhs = nsp.a`

    nsp => ⟨ π  storage sp = nsp; lhs = sp.a; ω ⟩ φ
    -----------------------------------------------
              => ⟨ π  lhs = nsp.a; ω ⟩ φ

**`storageIndexRead_unfold_rightFst`** — `lhs = nsp[e]`

    nsp => ⟨ π  storage sp = nsp; lhs = sp[e]; ω ⟩ φ
    ------------------------------------------------
              => ⟨ π  lhs = nsp[e]; ω ⟩ φ

### Instances of unfold_rightSnd

**`storageIndexRead_unfold_rightSndIndex`** — `lhs = sp[nse]`

    nse => ⟨ π  T pv = nse; lhs = sp[pv]; ω ⟩ φ
    ---------------------------------------------
          => ⟨ π  lhs = sp[nse]; ω ⟩ φ

### Instances of unfold_rightSndResult

**`storageFieldRead_unfold_rightSndResult`** — `nlhs = sp.a`

    nlhs => ⟨ π  T_{nlhs} pv = sp.a; nlhs = pv; ω ⟩ φ
    -------------------------------------------------
              => ⟨ π  nlhs = sp.a; ω ⟩ φ

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

## 5. Step 2: Unfolding the Left-Hand Side

**unfold_leftFst** — capture a nonsimple receiver on the LHS.

    nsp => ⟨ π  T_{nsp} sp = nsp; op(sp, a) = se; ω ⟩ φ
    ---------------------------------------------------
            => ⟨ π  op(nsp, a) = se; ω ⟩ φ

**unfold_leftSnd** — capture a nonsimple index on the LHS.

    nse => ⟨ π  T pv = nse; op(sp, pv) = se; ω ⟩ φ
    ------------------------------------------------
        => ⟨ π  op(sp, nse) = se; ω ⟩ φ

### Instances of unfold_leftFst

**`storageFieldWrite_unfold_leftFst`** — `nsp.a = se`

    nsp => ⟨ π  storage sp = nsp; sp.a = se; ω ⟩ φ
    ----------------------------------------------
            => ⟨ π  nsp.a = se; ω ⟩ φ

**`storageIndexWrite_unfold_leftFst`** — `nsp[e] = se`

    nsp => ⟨ π  storage sp = nsp; sp[e] = se; ω ⟩ φ
    -----------------------------------------------
            => ⟨ π  nsp[e] = se; ω ⟩ φ

### Instances of unfold_leftSnd

**`storageIndexWrite_unfold_leftSndIndex`** — `sp[nse] = se`

    nse => ⟨ π  T pv = nse; sp[pv] = se; ω ⟩ φ
    --------------------------------------------
         => ⟨ π  sp[nse] = se; ω ⟩ φ

### Standalone receiver / delete-target simplifications

These exist because their active statement is not assignment-shaped
(`op(sp,a) = se`); it is `delete`, `push`, `pop`, or a push-return
binding. Each replaces `nsp` with a fresh `storage sp = nsp;` capture,
then continues against `sp`.

**`storageDelete_unfold_leftFst`** — `delete nsp;`

    nsp => ⟨ π  storage sp = nsp; delete sp; ω ⟩ φ
    ----------------------------------------------
            => ⟨ π  delete nsp; ω ⟩ φ

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

**`storageLocalRootPush_unfold_leftFstReceiver`** — `lp = nsp.push();`

    nsp => ⟨ π  storage sp = nsp; lp = sp.push(); ω ⟩ φ
    ---------------------------------------------------
            => ⟨ π  lp = nsp.push(); ω ⟩ φ

## 6. Step 3: Generating an Update

Once every constituent is simple, the statement is replaced by a
parallel update on `⟨ π ω ⟩ φ`. Format below: source statement ⇝
emitted update.

### Local-storage declarations

- `storageLocalDeclInitDrop` (the `storage` keyword in the pattern is
  matched; sibling rules `localValueDeclInitDrop` /
  `memoryLocalDeclInitDrop` handle the other locations; the declared
  variable is registered as a program variable)

      T storage lp = path;
      ⇝  lp = path;                        (lp not occurring in path)

- `storageLocalDeclSkip`

      T storage lp ;
      ⇝  (skip; produces no update)        (lp not used)

### Field write

- `storageFieldWriteSave`

      sp.a = se
      ⇝  { storage := save(storage, sp · a, se) }

- `storageFieldWriteCopySource` (RHS is itself a storage path —
  the *value at* `sp2` is copied, not the path)

      sp1.a = sp2
      ⇝  { storage := save(storage, sp1 · a, select(storage, sp2)) }

  All the `*CopySource` copy rules assume the copied type carries no
  mapping: solc ≥ 0.7 rejects assignments whose target type transitively
  contains one, and both front ends enforce that at parse time
  (`ParserUtils.parseAssignmentMaybe`,
  `StorageReferenceTypes.containsMapping`), so the illegal shapes never
  reach the calculus. Storage-pointer rebinds (`storageLocalRootRebind`)
  and `delete` (mapping-preserving, §6) are unaffected.

### Root write (whole struct / primitive at a root)

- `storageRootWriteStore`

      gp = se
      ⇝  { storage := save(storage, gp, se) }

- `storageRootWriteCopySource` (one rule for primitive and struct
  sources — `find<[StValue]>` is sort-free, the sort is resolved on read)

      gp = sp
      ⇝  { storage := save(storage, gp, find<[StValue]>(storage, sp)) }

- `storageLocalRootRebind` (rebinds a local storage reference; does
  **not** copy)

      lp = sp
      ⇝  { lp := sp }

  Note: Since both `lp` and `sp` are now `List`-typed paths, this is
  a direct assignment without any wrapping.

### Field / root read

- `storageFieldReadFind`

      v = sp.a
      ⇝  { v := find(storage, sp · a) }

- `storageRootReadSelect`

      v = sp
      ⇝  { v := find(storage, sp) }

- `storageFieldReadBindLocalRoot`

      lp = sp.b
      ⇝  { lp := sp · b }

- `storageFieldReadStoreRoot`

      gp = sp.b
      ⇝  { storage := save(storage, gp, find(storage, sp · b)) }

### Delete

`delete` writes a *reset value* that is lazy for structs, so that **mapping
members survive** — Solidity's `delete` does not clear mappings (they cannot be
enumerated).

- `storageDeleteSimpleTarget`

      delete sp
      ⇝  { storage := delAt(storage, sp) }

- `delAt(storage, p)` leaves the reset lvalue unresolved: on read it becomes
  `default` for a primitive, and the lazy marker `delNode(…)` for a struct.
  Deferring the choice rather than writing the resolved value is what lets the
  rule stay sort-free — the sort arrives with the read.
- Reading a member `f` of `delNode(v)`:
    - `f` is a **mapping** member → read the original (`v`), i.e. preserved;
    - `f` is a **struct/array** member → recurse (nested mappings survive too);
    - `f` is a **primitive** member → `default`.

The field variant (`delete sp.a`) applies the same `delAt` scheme at the
fully-qualified path, so deleting a struct field also preserves its mappings.
The index variant (`delete sp[i]`) resets that single entry/element outright,
mapping members included — `{ storage := save(storage, sp · at(i), defVal) }`.

### Mapping index access  (when `mapping(sp)`)

- `storageIndexWriteMappingSave`

      sp[i] = se
      ⇝  { storage := save(storage, sp · at(i), se) }

- `storageIndexWriteMappingCopySource`

      sp1[i] = sp2
      ⇝  { storage := save(storage, sp1 · at(i), select(storage, sp2)) }

- `storageIndexReadMappingFind`

      v = sp[i]
      ⇝  { v := find(storage, sp · at(i)) }

- `storageIndexReadMappingBindLocalRoot`

      lp = sp[i]
      ⇝  { lp := sp · at(i) }

- `storageIndexReadMappingStoreRoot`

      gp = sp[i]
      ⇝  { storage := save(storage, gp, find(storage, sp · at(i))) }

### Array index access  (when `array(sp)`, with `ℓ = find(storage, sp · length)`)

Each array rule branches on bounds. Out-of-bounds goes to
`revert();` (consumed by §8).

- `storageIndexWriteArraySave`

      sp[i] = se
      ⇝  if 0 ≤ i < ℓ : { storage := save(storage, sp · at(i), se) }
         else         : revert();

- `storageIndexWriteArrayCopySource`

      sp1[i] = sp2
      ⇝  if 0 ≤ i < ℓ : { storage := save(storage, sp1 · at(i),
                                          select(storage, sp2)) }
         else         : revert();

- `storageIndexReadArrayFind`

      v = sp[i]
      ⇝  if 0 ≤ i < ℓ : { v := find(storage, sp · at(i)) }
         else         : revert();

- `storageIndexReadArrayBindLocalRoot`

      lp = sp[i]
      ⇝  if 0 ≤ i < ℓ : { lp := sp · at(i) }
         else         : revert();

- `storageIndexReadArrayStoreRoot`

      gp = sp[i]
      ⇝  if 0 ≤ i < ℓ : { storage := save(storage, gp,
                                           find(storage, sp · at(i))) }
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

- `storagePushValueCopySource`

      sp1.push(sp2)
      ⇝  { storage := save( save(storage, sp1 · at(n),
                                 select(storage, sp2)),
                            sp1 · length, n + 1 ) }

- `storagePushLengthSave` (zero-arg push: append the default-valued
  slot, return nothing — the appended slot is cleared with `delAt`, so a
  struct element's mapping members survive being pushed over)

      sp.push();
      ⇝  { storage := save( delAt(storage, sp · at(n)),
                            sp · length, n + 1 ) }

- `storageLocalRootPushBind` (zero-arg push whose returned slot is
  captured into a local reference)

      lp = sp.push();
      ⇝  { storage := save(storage, sp · length, n + 1)
           || lp := sp · at(n) }

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
desugared into explicit read–compute–write statements **before** the
rules above apply. The calculus does not contain dedicated
compound-update rules.

## 8. Abrupt Termination

A `revert();` aborts the entire transaction, so `π` and `ω` are
irrelevant. These are the only two storage rules that differ between
modalities:

- `revertDiamond`:   `=> ⟨ π revert(); ω ⟩ φ`   ⇝  `=> ⊥`
- `revertBox`:       `=> [ π revert(); ω ] φ`   ⇝  `=> ⊤`

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

**Root-write convention.** In root-write rules, `gp` and `lp` denote
the **whole** storage lvalue, including any final field or index
segment.

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
    ⇝ { storage := save(storage, alice, find(storage, bob)) } φ

where both `alice` and `bob` extract to single-element lists. The path
`bob` is *not* stored as the value; its struct value is read and stored.

## 11. Quick Reference: Statement → Rule

Use this when looking up which Step-3 rule fires.

**Unified path representation:** Both global roots (`gp`) and local
storage aliases (`lp`) are `List`-typed paths. A global root `alice`
extracts to `cons(alice, nil)`. All storage operations use `find`/`save`.

| Source statement                | Rule                                  | Update operation |
|--------------------------------|---------------------------------------|------------------|
| `sp.a = se`                    | `storageFieldWriteSave`               | `save`           |
| `sp1.a = sp2`                  | `storageFieldWriteCopySource`         | `save`           |
| `gp = se`                      | `storageRootWriteStore`               | `save`           |
| `gp = sp`                      | `storageRootWriteCopySource`          | `save`/`find<[StValue]>`|
| `lp = sp`                      | `storageLocalRootRebind`              | direct assign    |
| `v = sp.a`                     | `storageFieldReadFind`                | `find`           |
| `v = sp`                       | `storageRootReadSelect`               | `find`           |
| `lp = sp.b`                    | `storageFieldReadBindLocalRoot`       | direct assign    |
| `gp = sp.b`                    | `storageFieldReadStoreRoot`           | `save`/`find<[StValue]>`|
| `delete sp;`                   | `storageDeleteSimpleTarget`           | `delAt`        |
| `sp[i] = se`  (mapping)        | `storageIndexWriteMappingSave`        | `save`           |
| `sp1[i] = sp2`  (mapping)      | `storageIndexWriteMappingCopySource`  | `save`           |
| `v = sp[i]`  (mapping)         | `storageIndexReadMappingFind`         | `find`           |
| `lp = sp[i]`  (mapping)        | `storageIndexReadMappingBindLocalRoot`| direct assign    |
| `gp = sp[i]`  (mapping)        | `storageIndexReadMappingStoreRoot`    | `save`/`find<[StValue]>`|
| `sp[i] = se`  (array)          | `storageIndexWriteArraySave`          | `save`           |
| `sp1[i] = sp2`  (array)        | `storageIndexWriteArrayCopySource`    | `save`           |
| `v = sp[i]`  (array)           | `storageIndexReadArrayFind`           | `find`           |
| `lp = sp[i]`  (array)          | `storageIndexReadArrayBindLocalRoot`  | direct assign    |
| `gp = sp[i]`  (array)          | `storageIndexReadArrayStoreRoot`      | `save`/`find<[StValue]>`|
| `sp.push(se);`                 | `storagePushValueSave`                | `save`           |
| `sp1.push(sp2);`               | `storagePushValueCopySource`          | `save`           |
| `sp.push();`                   | `storagePushLengthSave`               | `save`           |
| `lp = sp.push();`              | `storageLocalRootPushBind`            | `save`           |
| `path.push() = se;`            | `storagePushLhsToPushValue` (desugar) | —                |
| `sp.pop();`                    | `storagePopSave`                      | `save`           |
| `revert();` (in `⟨·⟩`)         | `revertDiamond`                       | —                |
| `revert();` (in `[·]`)         | `revertBox`                           | —                |
