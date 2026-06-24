# Taclet Starters

This directory contains the runnable taclet subset inspired by the
Pre-licenciate-paper storage rules. The authoritative storage rules now live
in `keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/solidityProgramRules.key`
(loaded automatically via `standardSolidityRules.key`); the `.key` files in
this directory are examples that declare only example variables and define
closing problems.

## Implemented Here

- The storage taclets live in `solidityProgramRules.key` (see the path
  above). It collects the currently runnable subset of the
  Pre-licenciate-paper storage taclets. The split follows the
  `solidity-key-taclets` skill guidance: root storage rules use
  `SimpleStoragePath`, field/index paths use `Path[...]` plus
  `\sameAsTerm`, and complex member/index cases stay as structural `.key`
  source patterns.
- `storage-root-read-write.key` demonstrates `storageRootWriteStore` and
  `storageRootReadSelect` for contract storage fields using `\sameAsTerm`.
- `storage-root-copy-source.key` demonstrates `storageRootWriteCopySource` for
  copying one contract storage field into another.
- `storage-root-multiple-writes.key` shows last-write-wins on a root field
  (`age = 1; age = 2; result = age;` closes with `result = 2`).
- `storage-root-disjoint.key` shows that writing one root field does not
  perturb another (`age = 7; balance = 9; result = age;` closes with
  `result = 7`).
- `storage-field-global-age.key` demonstrates two-level member-access write and
  read on a contract field (`alice.age`) via the `_globalPath` rule pair, which
  matches a whole `Path[name=field.global]` directly without per-terminal
  decomposition. Uses the `&`-joined form (separate write and read diamonds).
- `storage-field-write-read.key` is the single-program counterpart: writes
  `alice.age = 34` and then reads it into `result`, closing via
  `findAfterSaveSameDeepPath`. Matches the user-facing `a.b = v; u = a.b;`
  shape.
- `storage-field-disjoint-roots.key` shows that the same field on different
  root structs is isolated: after `alice.age = 1; bob.age = 2;` we still have
  `alice.age = 1` and `bob.age = 2`. `&`-joined because each branch asserts a
  different read.
- `storage-field-decomposition.key` demonstrates nested member-access lowering
  for `alice.account.balance` via the generic `_globalPath` rule pair. The
  Solidity loader registers each struct field under its namespaced
  `Contract$Struct$field` constant (e.g. `PaperStore$Person$account`,
  `PaperStore$Account$balance`), and `Services.memberFieldTerm` reconstructs
  that name at lowering time by walking the member-access chain to identify
  the owning struct.
- `storage-field-deep-write-read.key` is the single-program write+read at
  depth 3: `alice.account.balance = 34; result = alice.account.balance;`.
  The proof exercises the paper's `storageFieldWrite_unfold_leftFst` (§5):
  the nonsimple receiver `alice.account` is captured into a fresh storage
  alias `sp` by emitting `T storage sp = alice.account; sp.balance = 34;`
  literally into the modality. The downstream chain
  `storageLocalDeclInitSplit` → `storageLocalDeclSkip` →
  `storageFieldReadBindLocalRoot` → `storageFieldWriteSave_local` closes the
  proof, matching storage.md:499-505 step for step.
- `storage-field-deep-value.key` demonstrates four-level member-access write
  and read on `alice.account.token.value`, chaining `_decomposeToken` followed
  by `_decomposeValue` against the `Path[name=field.global]` base.
- `storage-index-root-array.key` demonstrates root-level dynamic-array index
  write and read (`values[1]`) via `storageIndexWriteMappingSave_root` and the
  sibling `storageIndexReadMappingFind_root`. Arrays and mappings share the
  rule because the bounds branch is not yet modelled, so a separate mapping
  example is not needed.
- `storage-index-multiple-writes.key` shows last-write-wins on an indexed
  root slot (`values[3] = 5; values[3] = 8; result = values[3];` closes with
  `result = 8`).
- `storage-field-disjoint-fields.key` shows that distinct struct fields under
  the same parent do not interfere: writing `alice.account.balance` and
  `alice.account.token.value` preserves each. Works because the field
  constants are declared `\unique`, unlike `at(1)` vs `at(2)` which would
  require additional injectivity reasoning.
- `storage-index-decomposition.key` demonstrates the indexed counterpart:
  rules with source patterns such as `s#a[s#i]` match nested indexed paths like
  `matrix[2][3]`, bind `a` to `matrix[2]` and `i` to `3`, and append
  `at(i)` as the `Field`-sorted index segment in the replacement.
- `storage-matrix-write-read.key` is the single-program write+read on the same
  nested indexed path: `matrix[2][3] = 99; result = matrix[2][3];`.
- `storage-local-decl-skip.key` demonstrates `storageLocalDeclSkip`: an
  uninitialized `Person storage p;` declaration is dropped without
  emitting an update, and the surrounding root read/write closes as if
  the declaration were not there.
- `storage-field-read-bind-local.key` demonstrates
  `storageFieldReadBindLocalRoot`: a standalone `acc = bob.account;`
  rebind (no declaration prefix) re-points an existing local storage
  alias at a member-rooted path; the subsequent `acc.balance` read goes
  through the rebound path and reads back the most recent write.
- `storage-field-read-store-root.key` demonstrates
  `storageFieldReadStoreRoot`: `total = alice.age;` copies the value
  at a global field path into a global int root via
  `storeSt(storage, total, find<[int]>(storage, alice·age))`.

### Increment and Decrement Operators

Increment and decrement operators are now implemented using **direct storage update rules** that modify storage without desugaring to assignment statements. This approach follows KeY's pattern for Java heap operations and avoids the complexity of evaluating arithmetic expressions at the program level.

**Implementation approach**:
- Operators like `++age` directly create storage updates: `{storage := save(storage, path, find<[int]>(storage, path) + 1)}`
- Arithmetic happens at the logic term level, not the program level
- No intermediate desugaring step that would create complex expression patterns

**Operator matching fix**:
The pattern matching infrastructure was fixed to check operator fields in `AssignExpression`, `UnaryExpression`, and `BinaryExpression`. Previously, taclet patterns like `s#lhs += s#rhs` would incorrectly match regular assignments `s#lhs = s#rhs` because matching only checked AST node class, not the operator enum value. The fix adds `match()` overrides that verify `operator.equals()` before accepting a match.

**Implemented rules**:

*Root-level rules:*
- `storageRootPreincrement` - `++age;` on storage roots
- `storageRootPostincrement` - `age++;` on storage roots
- `storageRootPredecrement` - `--age;` on storage roots
- `storageRootPostdecrement` - `age--;` on storage roots
- `storageRootPreincrementAssignment` - `result = ++age;`
- `storageRootPostincrementAssignment` - `result = age++;`
- `storageRootPredecrementAssignment` - `result = --age;`
- `storageRootPostdecrementAssignment` - `result = age--;`

*Field-level rules:*
- `storageFieldPreincrement` - `++alice.age;` on nested fields
- `storageFieldPostincrement` - `alice.age++;` on nested fields
- `storageFieldPredecrement` - `--alice.age;` on nested fields
- `storageFieldPostdecrement` - `alice.age--;` on nested fields
- `storageFieldPreincrementAssignment` - `result = ++alice.age;`
- `storageFieldPostincrementAssignment` - `result = alice.age++;`
- `storageFieldPredecrementAssignment` - `result = --alice.age;`
- `storageFieldPostdecrementAssignment` - `result = alice.age--;`

*Index-level rules:*
- `storageIndexPreincrement` - `++values[i];` on indexed storage
- `storageIndexPostincrement` - `values[i]++;` on indexed storage
- `storageIndexPredecrement` - `--values[i];` on indexed storage
- `storageIndexPostdecrement` - `values[i]--;` on indexed storage
- `storageIndexPreincrementAssignment` - `result = ++values[i];`
- `storageIndexPostincrementAssignment` - `result = values[i]++;`
- `storageIndexPredecrementAssignment` - `result = --values[i];`
- `storageIndexPostdecrementAssignment` - `result = values[i]--;`

*Unfold rules for complex paths:*
- `storageFieldPreincrement_unfold_leftFst` - `++nsp.a;` unfolds complex receiver
- `storageFieldPostincrement_unfold_leftFst` - `nsp.a++;` unfolds complex receiver
- `storageFieldPredecrement_unfold_leftFst` - `--nsp.a;` unfolds complex receiver
- `storageFieldPostdecrement_unfold_leftFst` - `nsp.a--;` unfolds complex receiver
- `storageIndexPreincrement_unfold_leftFst` - `++nsp[i];` unfolds complex receiver
- `storageIndexPostincrement_unfold_leftFst` - `nsp[i]++;` unfolds complex receiver
- `storageIndexPredecrement_unfold_leftFst` - `--nsp[i];` unfolds complex receiver
- `storageIndexPostdecrement_unfold_leftFst` - `nsp[i]--;` unfolds complex receiver

**Example files**:

*Root-level examples:*
- `storage-root-preincrement.key` - Tests `++age;` on simple storage root
- `storage-root-postincrement.key` - Tests `age++;` on simple storage root
- `storage-root-predecrement.key` - Tests `--age;` on simple storage root
- `storage-root-preincrement-assign.key` - Tests `result = ++age;`
- `storage-root-postincrement-assign.key` - Tests `result = age++;`
- `storage-root-postdecrement-assign.key` - Tests `result = age--;`

*Field-level examples:*
- `storage-field-preincrement.key` - Tests `++alice.age;` on nested field
- `storage-field-postincrement.key` - Tests `alice.age++;` on nested field
- `storage-field-predecrement.key` - Tests `--alice.age;` on nested field
- `storage-field-postdecrement.key` - Tests `alice.age--;` on nested field
- `storage-field-preincrement-assign.key` - Tests `result = ++alice.age;`
- `storage-field-postincrement-assign.key` - Tests `result = alice.age++;`
- `storage-field-predecrement-assign.key` - Tests `result = --alice.age;`
- `storage-field-postdecrement-assign.key` - Tests `result = alice.age--;`

*Index-level examples:*
- `storage-index-preincrement.key` - Tests `++values[i];` on indexed storage
- `storage-index-postincrement.key` - Tests `values[i]++;` on indexed storage
- `storage-index-predecrement.key` - Tests `--values[i];` on indexed storage
- `storage-index-postdecrement.key` - Tests `values[i]--;` on indexed storage
- `storage-index-preincrement-assign.key` - Tests `result = ++values[i];`
- `storage-index-postincrement-assign.key` - Tests `result = values[i]++;`
- `storage-index-predecrement-assign.key` - Tests `result = --values[i];`
- `storage-index-postdecrement-assign.key` - Tests `result = values[i]--;`

*Deep path examples (unfold rules):*
- `storage-deep-field-preincrement.key` - Tests `++alice.account.balance;`
- `storage-deep-field-postincrement.key` - Tests `alice.account.balance++;`

All 24 increment/decrement examples close successfully.

### Compound assignment operators

Storage `+=` is implemented with direct storage update rules, matching the
increment/decrement approach:

- `storageRootAddAssign` - `age += 5;` on storage roots
- `storageFieldAddAssign` - `alice.age += 4;` on nested fields
- `storageIndexAddAssign` - `values[1] += 2;` on indexed storage paths
- `storageFieldAddAssign_unfold_leftFst` - unfolds complex field receivers such
  as `alice.account.balance += 4;` through a fresh storage alias before the
  terminal field rule fires
- `storageIndexAddAssign_unfold_leftFst` - unfolds complex indexed receivers
  such as `sp[i][j] += 4;` through a fresh storage alias before the
  terminal index rule fires

Example files:

- `storage-root-add-assign.key` - Tests `age += 5;`
- `storage-field-add-assign.key` - Tests `alice.age += 4;`
- `storage-index-add-assign.key` - Tests `values[1] += 2;`
- `storage-field-deep-add-assign.key` - Tests `alice.account.balance += 4;`

The remaining compound assignment operators (`-=`, `*=`, `/=`, `%=`, `&=`,
`|=`, `^=`, `<<=`, `>>=`) are parsed correctly but not yet implemented.

### Storage-alias examples

`solidityProgramRules.key` includes four local-storage alias taclets:

- `storageLocalDeclInitSplit_rootRebind` — `T storage lp = sp;`
- `storageLocalDeclInitSplit_globalField` — `T storage lp = sp.b;`
- `storageLocalDeclInitSplit_localField` — `T storage lp = lp2.b;`
- `storageLocalRootRebind` — `lp = sp;` (standalone rebind, no declaration)

The pattern shape is `s#aliasType storage s#lp = s#rhs;` (and `s#lp = s#sp;`
for the standalone variant). The `Type` schema-variable slot is display-only
per `StatementVariableDeclaration` line 32, and `\replacewith` cannot emit a
schematic declaration (line 23 of that class), so the paper's
`storageLocalDeclInitSplit` + separate-assignment split is collapsed into one
combined taclet per RHS shape. Per `storage.md` §6 ("Local-Root" rules), the
alias binds to the **path**, not to the value at the path: the rules emit
`{lp := cons1(rhsField)}` (root rhs) or `{lp := pathFields}` (member-rooted
rhs).

Two pieces of plumbing make this work end-to-end:

1. **List-sorted storage locals.** `SolidityToKeyConverter#asStorageAliasType`
   resorts `T storage lp` from `Struct` to `List` so the path-shaped update
   type-checks and `Services#convertToLogicElement` lowers `lp.b` as
   `consr(lp, b)` (path append) instead of wrapping in a fresh single-element
   list.

2. **`\program Variable[name=…]` filters.** `ProgramVariableSVSort` accepts a
   parameter (`storage` / `value`) that filters by `DataLocation`. This makes
   `storageRootReadSelect` (int-typed reads of a global root)
   and `storageLocalRootRebind` (List-typed rebind of a
   storage-local) pairwise disjoint, even though both have shape
   `s#x = s#rootRhs;`.

Example files (consume `paper_test.sol:165-184`):

- `storage-alias-write-balance.key` — from `testStorageAliases`, asserting
  `alice.account.balance = 100` after the alias-rooted writes
  `acc.balance = 100; p.account.token.value = 3;`.
- `storage-alias-write-token.key` — same scenario, token branch.
- `storage-alias-rebind-original.key` — asserts `alice.age` is unchanged by
  the rebind sequence `Person storage alicePath = alice; bob.age = 20;
  alicePath = bob;`. Compares against a snapshot taken into `before` so the
  assertion does not depend on the symbolic initial storage value.
- `storage-alias-rebind-alias.key` — same scenario, asserts that the rebound
  `alicePath.age` now reads back as the value written via `bob.age = 20`.

- Solidity `delete target` now parses to `UnaryExpression(Operator.DELETE,
  target)` and prints as `delete target`.
- Path-aware program schema-variable sorts are available. The fixed names
  `StoragePath`, `SimpleStoragePath`, `ComplexStoragePath`, `MemoryPath`,
  `SimpleMemoryPath`, and `ComplexMemoryPath` cover common cases. More precise
  filters use `Path[name=...]`, with dotted flags such as `storage`, `memory`,
  `simple`, `complex`, `root`, `field`, `index`, `array`, `mapping`,
  `primitive`, `reference`, `local`, and `global`. Roots are simple; member
  and indexed paths such as `alice.account` are complex. Storage paths may be
  `local` or `global`; memory paths are always local. Field/index paths lower
  to the `List` sort consumed by `save` and `find` in `structRules.key`.
- `\sameAsTerm` lowers full program paths to logic path terms. Root contract
  fields still lower to `Field` constants for `storeSt` and `selectSt`; nested
  member and indexed paths such as `alice.account.balance`, `tokens[i]`, and
  `ledger.balances[k]` lower to `List` terms for `save` and `find`. Indexed
  segments are represented as `at(index)` so they have sort `Field` and can be
  unfolded by the structural storage taclets.

Run these through the focused JUnit test added in `keyext.solidity.core`, or
load an individual file with the Solidity CLI from the repository root.

## Work Needed For The Full Paper Rule Set

The remaining paper taclets should be added as focused `.key` patterns only
when the matcher can preserve the relevant Solidity path shape faithfully.

1. Continue direct complex-path pattern support. Implemented for member-field
   and indexed-path decomposition, and for the paper's Step-2
   `storageFieldWrite_unfold_leftFst` rule (`s#nsp.s#a = s#se` with a fresh
   storage alias on the receiver). The supporting infrastructure adds a
   `\program Field` schema-variable sort matching the field-name child of a
   `MemberExp`, and storage-aliased fresh-variable instantiation in
   `TacletApp#getProgramElement` so `\newTypeOf(sp, nsp)` yields a
   `Variable[name=storage]`-sorted local. The paper's Step-3 `..._global`
   and `..._local` terminal rules were rewritten to decompose the LHS the
   same way as the unfold rule, so a simple `SIMPLICITY=SIMPLE` constraint
   on the receiver SV suffices to keep the unfold rule winning on
   complex-receiver writes.

   Member-field decomposition is implemented for patterns such as
   `s#a.field`; indexed-path decomposition is implemented for patterns such as
   `s#a[s#i]`. Further work in this direction should keep field/index base
   unfolding in structural `.key` patterns where possible.

2. Preserve storage vs. memory delete semantics.

   The paper/Lean model distinguishes storage delete targets from memory delete
   targets, and distinguishes simple from complex targets. Do not collapse this
   into a single `delete path` rule: examples such as `delete carol.age` depend
   on whether `carol` is a memory object, storage root, storage alias, or a
   complex computed path.

   Storage `delete` at root, field, and indexed levels (`storageRootDelete`,
   `storageFieldDelete`, `storageIndexDelete`) saves
   `defaultF(last(spPath))` — i.e. it delegates the cleared value to the
   field-keyed default function. `defaultF` was flattened from the previous
   polymorphic `defaultF<[alpha]>(Field)` to `any defaultF(Field)`;
   existing call sites in `selectOnEmptyStorage` and `defaultDef` now cast
   the result to the read sort (`(alpha) defaultF(fld)`). Per-shape unfold
   rules (`storageFieldDelete_unfold_leftFst`,
   `storageIndexDelete_unfold_leftFst`) alias only the receiver when it is
   complex — mirroring the per-shape `++`/`+=` unfold structure to avoid
   clashing with the simple-receiver terminal rules.

   `defaultF(F)` is resolved at apply time by the `#defaultOf(Field)`
   term-transformer meta-construct (`MetaDefaultOf`), which looks up the
   field's declared Solidity type via `SolidityInfo.getFieldType` and
   returns `0` for int/uint/address fields, `false` for bool fields, and
   the empty struct `mtSt` for struct receivers. The field-type map is
   populated by `SolJSONParser.registerFieldConstant` at contract load,
   so no per-field axioms need to appear in problem `.key` files. The
   resolver taclet `defaultFViaDefaultOf` (in `memoryRules.key`) uses
   the `simplify_enlarging` heuristic so that list-walking rules
   (`headDefinition`, `lastConsNil`) reduce a path-extracting argument
   like `last<[Field]>(spPath)` to a bare Field constant *before* the
   resolver fires. Remaining gaps: array element-type detection
   (`storageIndexDelete` therefore hardcodes int `0` — sound for
   `uint[]` / `int[]`, not for `bool[]` / `struct[]`); dynamic-array
   length-reset semantics of `delete arr;`; memory delete.

3. Add Java/logic support for the paper rules that need generated aliases or
   heap helpers.

   The runnable `.key` subset does not yet cover array bounds/revert
   branches, push/pop length updates, non-primitive delete (struct/array
   root), memory heap read/write allocation rules, or storage-memory copy
   rules using `copySt`/`copyMem`-style helpers. Increment/decrement
   operators (++, --) are fully implemented at root, field, and index levels
   including unfold rules for complex paths. The `+=` compound assignment
   operator is implemented at root, field, and index levels. Other compound assignment operators (-=, *=, /=, %=, &=,
   |=, ^=, <<=, >>=) are parsed correctly but not yet implemented.
   Three alias decl-init rules are now in `solidityProgramRules.key` and
   fire correctly on `Person storage p = alice;`-shaped declarations, but
   standalone rebind (`lp = sp;` without declaration) and write-propagation
   through alias-rooted paths still need either path-as-List sorting for
   storage locals or an extra `cons(find<[Struct]>(s, p), q)` normalisation
   axiom — see the alias section above for the specific obstacles.

## Suggested Next Examples After Java Support

- `storage-{root,field,index}-delete.key` cover the primitive `delete`
  cases at root, simple-field, and simple-index receivers.
  `storage-root-delete-struct.key` covers `delete alice;` where `alice` is
  a struct root (saves `mtSt`, then nested reads collapse through
  `selectOnEmptyStorage` + the per-field int default to `0`). The
  receiver-unfold rules for complex receivers (e.g.
  `delete alice.account.balance;`) are implemented but not yet exercised
  by an example.
- `storage-field-read-write.key`: broader `storageFieldWriteSave` and
  `storageFieldReadFind` variants for additional source-level member paths
  beyond the `_decomposeBalance`/`_decomposeToken`/`_decomposeValue` siblings
  already present (each new terminal field still needs its own decompose
  sibling until a generic field-name match is supported).
- `storageFieldWriteCopySource` (struct-value copy along a member path),
  `storageFieldReadStoreRoot` for struct-typed roots, and the
  `…BindLocalRoot`/`…StoreRoot` mapping/array variants still need
  `find<[Struct]>`/`storeSt` plumbing for non-int payloads and array
  bounds branches.
- `memory-delete.key`: `memoryDeleteSimpleTarget` and
  `memoryDeleteComplexTarget`.
- `storage-index-read-write.key`: bounds-branching variants of the existing
  `_rootIndex` and `_decomposeArrayIndex` index rules, plus the paper's
  storage-alias copy cases (`tokens[i] = tokRef;`) once alias introduction is
  supported.
- `memory-field-index.key`: memory field/index write, read, and alias-root
  cases.

## Coverage Audit Against `docs/storage.md`

This section maps the §10 "Worked Examples" and the §11 "Statement →
Rule" table in `docs/storage.md` to the current example set, and
records, for every gap, whether new taclets/plumbing are required.

### §10 Worked examples (8 traces)

Covered:

- `alice.age = ageVal;` → `storage-field-global-age.key`,
  `storage-field-write-read.key`.
- `alice.account.balance = 10;` (depth-2 write) →
  `storage-field-deep-write-read.key`,
  `storage-field-decomposition.key`.
- `v = alice.account.balance;` (depth-2 read) →
  `storage-field-decomposition.key`.
- `alice.account.token.value = 5;` (depth-3 write) →
  `storage-field-deep-value.key`.
- `uint v = total;` (primitive root read) →
  `storage-root-read-write.key`.
- `alice = bob;` (root-to-root copy, primitive payload only) →
  `storage-root-copy-source.key`.

Recently added (struct-payload copy):

- `alice = bob;` (whole-struct root-to-root copy) →
  `storage-root-copy-struct.key`, via the new
  `storageRootWriteCopySource_struct` taclet (`find<[Struct]>`).
- `Account storage acc = bob.account; alice.account = acc;` →
  `storage-field-copy-struct.key`, via the new
  `storageFieldWriteCopySource` taclet.

Still missing:

- `alice = pVal;` (whole-struct write to a root from a struct *value*
  rather than another storage path) — requires step-1 unfolding for
  struct constructors / memory-struct sources, which is not yet
  modelled.

### §11 Statement → Rule coverage

Step-3 rules with an example today:

- `storageFieldWriteSave` (`sp.a = se`), `storageFieldReadFind`
  (`v = sp.a`).
- `storageRootWriteStore` (`gp = se`), `storageRootReadSelect`
  (`v = sp`), `storageRootWriteCopySource` (`gp = sp`, primitive only).
- `storageLocalRootRebind` (`lp = sp`),
  `storageFieldReadBindLocalRoot` (`lp = sp.b`),
  `storageFieldReadStoreRoot` (`gp = sp.b`).
- `storageIndexWriteMappingSave` / `storageIndexReadMappingFind` —
  currently shared with the array shape (no `array(sp)` /
  `mapping(sp)` split).

Step-3 rules added since the audit (struct-payload copy):

- `storageRootWriteCopySource_struct` — `gp = sp` where both roots are
  struct-typed. Same `\find` shape as the primitive
  `storageRootWriteCopySource`; emits
  `save(storage, lhsPath, find<[Struct]>(storage, rhsPath))`.
  Disambiguation: struct payload uses `find<[Struct]>` instead of the
  primitive rule's `find<[int]>`; both rules currently share the
  `\find` pattern, but the proof only closes when the struct variant
  fires for struct-typed roots (verified by `storage-root-copy-struct.key`).
- `storageFieldWriteCopySource` — `sp1.a = sp2;` where `sp2` is a
  simple storage path (root or storage-local). Emits
  `save(storage, consr(sp1Path, fieldA), find<[Struct]>(storage, sp2Path))`.
  To make this pairwise disjoint with the primitive
  `storageFieldWriteSave`, `SimpleExpressionSVSort` was tightened to
  exclude `DataLocation.Storage` program variables (storage-locals are
  `List`-typed paths, not values, so they never belonged in the
  primitive write rule).

Step-3 rules with **no example and no runnable taclet**:

| Missing rule | Statement shape | Blocking work |
|---|---|---|
| `storageDeleteSimpleTarget` | `delete sp;` | dedicated delete taclet that preserves storage-vs-memory and simple-vs-complex targets (see §"Work Needed" item 2) |
| `storageIndexWriteMappingCopySource` | `sp1[i] = sp2` (mapping) | alias/struct-copy support |
| `storageIndexReadMappingBindLocalRoot` | `lp = sp[i]` (mapping) | mapping-side taclet |
| `storageIndexReadMappingStoreRoot` | `gp = sp[i]` (mapping) | mapping-side taclet |
| All five array index variants (`storageIndexWriteArraySave`, `…CopySource`, `…ReadArrayFind`, `…BindLocalRoot`, `…StoreRoot`) | `sp[i] = …` / `… = sp[i]` (array) | bounds branch + `revert` machinery (impl doc §"Work Needed" item 3), plus the `array(sp)` / `mapping(sp)` predicate split |
| `storagePushValueSave` | `sp.push(se);` | full push/pop calculus |
| `storagePushValueCopySource` | `sp1.push(sp2);` | same |
| `storagePushLengthSave` | `sp.push();` | same |
| `storageLocalRootPushBind` | `lp = sp.push();` | same |
| `storagePushLhsToPushValue` (desugar) | `path.push() = se;` | same |
| `storagePopSave` | `sp.pop();` | bounds/revert + push/pop calculus |
| `revertDiamond` | `revert();` in `⟨·⟩` | `revert` taclet pair |
| `revertBox` | `revert();` in `[·]` | same |

Compound `+=` and ++/–– (storage.md §7 — desugared before Step-3) are
already exhaustively covered at root/field/index level (see the
"Increment and Decrement Operators" and "Compound assignment
operators" subsections above). Only `-=, *=, /=, %=, &=, |=, ^=, <<=,
>>=` remain.

### Take-away

No example is missing purely because nobody wrote the `.key` file:
every gap depends on a taclet or plumbing item already enumerated in
§"Work Needed For The Full Paper Rule Set" and §"Suggested Next
Examples After Java Support". The three coherent buckets of work are
(1) struct-payload `find`/`save`, (2) dynamic-array semantics
(bounds, push, pop, `revert`), and (3) mapping-specific
`lp = sp[i]` / `gp = sp[i]` / `sp1[i] = sp2` rules plus storage
`delete`.
