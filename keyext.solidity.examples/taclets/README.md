# Pre-licenciate-paper Taclet Starters

This directory contains the runnable taclet subset inspired by the
Pre-licenciate-paper storage rules. `storageRules.key` is the authoritative
rule file; the other `.key` files in this directory are examples that include
it, declare only example variables, and define closing problems.

## Implemented Here

- `storageRules.key` collects the currently runnable subset of the
  Pre-licenciate-paper storage taclets. It is the only rule-bearing `.key` file
  in this directory. The split follows the `solidity-key-taclets` skill
  guidance: root storage rules use `SimpleStoragePath`, field/index paths use
  `Path[...]` plus `\sameAsTerm`, and complex member/index cases stay as
  structural `.key` source patterns.
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

### Storage-alias examples

`storageRules.key` includes four local-storage alias taclets:

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
   and indexed-path decomposition.

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

3. Add Java/logic support for the paper rules that need generated aliases or
   heap helpers.

   The runnable `.key` subset does not yet cover array bounds/revert
   branches, push/pop length updates, delete, compound-update desugaring,
   memory heap read/write allocation rules, or storage-memory copy rules
   using `copySt`/`copyMem`-style helpers. Three alias decl-init rules are
   now in `storageRules.key` and fire correctly on
   `Person storage p = alice;`-shaped declarations, but standalone rebind
   (`lp = sp;` without declaration) and write-propagation through
   alias-rooted paths still need either path-as-List sorting for storage
   locals or an extra `cons(find<[Struct]>(s, p), q)` normalisation axiom
   — see the alias section above for the specific obstacles.

## Suggested Next Examples After Java Support

- `storage-delete.key`: `storageDeleteSimpleTarget` and
  `storageDeleteComplexTarget`.
- `storage-field-read-write.key`: broader `storageFieldWriteSave` and
  `storageFieldReadFind` variants for additional source-level member paths
  beyond the `_decomposeBalance`/`_decomposeToken`/`_decomposeValue` siblings
  already present (each new terminal field still needs its own decompose
  sibling until a generic field-name match is supported).
- `memory-delete.key`: `memoryDeleteSimpleTarget` and
  `memoryDeleteComplexTarget`.
- `storage-index-read-write.key`: bounds-branching variants of the existing
  `_rootIndex` and `_decomposeArrayIndex` index rules, plus the paper's
  storage-alias copy cases (`tokens[i] = tokRef;`) once alias introduction is
  supported.
- `memory-field-index.key`: memory field/index write, read, and alias-root
  cases.
