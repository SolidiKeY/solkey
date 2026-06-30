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
  write and read (`values[1]`) via the bounded
  `storageIndexWriteArraySave_root` and `storageIndexReadArrayFind_root`
  rules. The example assumes `values.size > 1`, so the in-bounds branch closes
  and the out-of-bounds/revert branch is contradictory.
- `storage-index-root-mapping.key` demonstrates root-level mapping index write
  and read (`balances[1]`) via the unbounded mapping rules; mappings do not
  generate size checks.
- `storage-index-array-out-of-bounds-box.key` demonstrates the out-of-bounds
  array branch in box modality, where `revert();` closes to `true`.
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

The arithmetic compound assignments `-=`, `*=`, `/=`, `%=` (`docs/taclet-ideas.md`
Tier 2) are twins of the `+=` family — each clones the same five rules with the
infix operator swapped (`-`, `*`, `/`, `%`):

- `storageRoot{Sub,Mul,Div,Mod}Assign` - terminal rule on storage roots
- `storageField{Sub,Mul,Div,Mod}Assign` - terminal rule on nested fields
- `storageIndex{Sub,Mul,Div,Mod}Assign` - terminal rule on indexed (array) paths
- `storageField{Sub,Mul,Div,Mod}Assign_unfold_leftFst` - unfolds complex field
  receivers through a fresh storage alias before the terminal field rule fires
- `storageIndex{Sub,Mul,Div,Mod}Assign_unfold_leftFst` - unfolds complex indexed
  receivers before the terminal index rule fires

Like the Tier-1 `/` and `%` expression rules, the `/=` and `%=` terminals guard
the update with `\if(se != 0)\then(…)\else(revert)`, so dividing/modding a storage
target by zero reverts (the unfold rules carry no guard — they only rewrite the
program). There is no overflow branch, matching `+=` and Tier-1 arithmetic.

Example files (root / field / array-index / deep-field per operator):

- `storage-{root,field,index,field-deep}-sub-assign.key` - `-=`
- `storage-{root,field,index,field-deep}-mul-assign.key` - `*=`
- `storage-{root,field,index,field-deep}-div-assign.key` - `/=`
- `storage-{root,field,index,field-deep}-mod-assign.key` - `%=`

The `…-field-deep-…` examples (e.g. `alice.account.balance /= 4;`) exercise the
`…Field…_unfold_leftFst` rule before the terminal fires.

The remaining compound assignment operators (`&=`, `|=`, `^=`, `<<=`, `>>=`) are
parsed correctly but not yet implemented — they are gated on bitwise LDT support.

### Tier 1 expression operators

`docs/taclet-ideas.md` Tier 1 (pure expression evaluation) is implemented by
mirroring the existing `+` and `==` families, and staying faithful to KeY's Java
calculus (`key.core/.../integer/integerAssignment2UpdateRules.key`,
`.../rules/javaRules.key`). The terminal rules assign the logic-level result of
the operator to a value variable; non-simple operands are first captured into
fresh value temporaries (the `_unfold_*` / `*Capture*` rules), exactly like
`addition_unfold_left/right/result` and `boolEqualityCaptureLhs`.

Solidity adaptations vs. Java: plain LDT operators (`sub`, `mul`, `div`, `mod`,
`pow`, `neg`) instead of Java's overflow-wrapper `javaXxx` functions, so — like
the existing `+` — there is **no overflow branch**; and division-by-zero
**reverts** (`revert();`) where Java throws `ArithmeticException`.

**Arithmetic** (`solidityProgramRules.key`), each with `_unfold_left`,
`_unfold_right`, `_unfold_result`, and a terminal:

- `subtractionAssignment` — `v = a - b;` → `{v := a - b}`
- `multiplicationAssignment` — `v = a * b;` → `{v := a * b}`
- `powerAssignment` — `v = a ** b;` → `{v := pow(a, b)}` (no infix `**` in the
  term parser; `pow` on concrete literals is folded by `intSimplificationRules`)
- `divisionAssignment` — `v = a / b;` → `\if(b != 0)\then({v := a / b}…)\else(revert)`
- `moduloAssignment` — `v = a % b;` → `\if(b != 0)\then({v := a % b}…)\else(revert)`

**Relational** (bool result), each with `…CaptureLhs`, `…CaptureRhs`, and a
terminal twin of `boolEqualityAssignment` (predicate map `< → lt`, `<= → leq`,
`> → gt`, `>= → geq`; `!=` negates equality):

- `boolInequalityAssignment` (`!=`), `lessThanAssignment` (`<`),
  `greaterThanAssignment` (`>`), `lessEqualAssignment` (`<=`),
  `greaterEqualAssignment` (`>=`).

**Logical / unary** (mirroring Java `compound_assignment_*_simple`/`_mixed`,
`compound_assignment_1_new`/`_2`, `unaryMinusInt`):

- `logicalAndAssignment` (`&&`), `logicalOrAssignment` (`||`) with a
  `…CaptureLhs` left-capture rule each.
- `logicalNotAssignment` (`!`) and `unaryMinusAssignment` (`-x`), each with a
  capture rule for a non-simple operand.

Example files: `subtraction-simple.key`, `subtraction-storage-read.key`,
`multiplication-simple.key`, `power-simple.key`, `division-simple.key`,
`modulo-simple.key`, `less-than-simple.key`, `greater-than-simple.key`,
`less-equal-simple.key`, `greater-equal-simple.key`, `not-equal-simple.key`,
`logical-and-simple.key`, `logical-or-simple.key`, `logical-not-simple.key`,
`unary-minus-simple.key` (all in `TacletStarterExamplesTest`).

Deferred from Tier 1, with rationale:

- **Bitwise `&`, `|`, `^`, `<<`, `>>` and `~x`** — `intHeader.key` has no bitwise
  LDT functions (Java uses `javaBitwiseAndInt`, `shiftleft`, …). Deferred until
  int bit-ops are modelled (matching `docs/taclet-ideas.md`).
- **Unary plus `+x`** — Java has no unary-plus rule (parse-time no-op) and
  Solidity ≥0.5 removed the operator.
- **`&&`/`||` right-operand short-circuit** — Java's `_nonsimple` variant expands
  to an `if`-statement, which depends on the Tier-3 `if` rule. Only the
  both-operands-simple terminal and the left-operand capture are implemented now.
- **Checked-arithmetic overflow revert (Solidity ≥0.8)** — consistent with `+`
  having none; future work alongside the Tier-3 `unchecked { }` rule.

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
  and indexed paths such as `alice.account` are complex, as is a no-arg
  `arr.push()` (the freshly appended slot, typed as the array's element type) —
  this lets the complex-receiver unfold rules capture a `push()` return value
  the same way they capture any other complex path. Storage paths may be
  `local` or `global`; memory paths are always local. Field/index paths lower
  to the `List` sort consumed by `save` and `find` in `structRules.key`. A
  `push()` path is only ever captured (via `\newTypeOf`), never lowered with
  `\sameAsTerm`, since `convertToLogicElement` cannot turn a call into a term.
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

  Storage `delete` at root and field levels (`storageRootDelete`,
  `storageFieldDelete`) saves the typed default operator
  `defaultValue<[alpha]>`. `storageRootDelete` binds `alpha` from the
  matched root path with `\hasSort(sp, \sort(alpha))`. `storageFieldDelete`
  binds `alpha` from the matched field with
  `\hasFieldSort(a, \sort(alpha))`, which reads the field's AST type metadata.
  Per-shape unfold rules (`storageFieldDelete_unfold_leftFst`,
  `storageIndexDelete_unfold_leftFst`) alias only the receiver when it is
  complex — mirroring the per-shape `++`/`+=` unfold structure to avoid
  clashing with the simple-receiver terminal rules.

  The currently supported default rewrites are `defaultValue<[int]> = 0`,
  `defaultValue<[bool]> = FALSE`, and `defaultValue<[Struct]> = mtSt`,
  shared by memory defaults, empty storage reads, and storage delete.
  `storageIndexDelete` binds `alpha` from indexed receivers with
  `\hasElementSort(sp, \sort(alpha))`, using a mapping's value type or an
  array's element type. Index keys still lower through the current `at(idx)`
  int-index encoding, so non-int key precision remains out of scope.
  Memory root allocation is implemented for bare reference declarations
  (`memoryReferenceDeclFreshAlloc`) and root memory delete/rebinding
  (`memoryRootDeleteFreshRebind`). Both rules introduce a fresh
  `IdentityPrim` skolem and split off an explicit `new(memory, r)` proof
  branch; the semantic branch assumes that freshness fact before emitting
  `{mp := idC(r, nil)} {memory := addM(memory, r)}`. Memory field/index
  delete is now split between primitive default writes and reference-slot
  freshening, with complex receivers unfolded through fresh memory aliases.
  Dynamic-array length-reset semantics of `delete arr;` remains out of scope
  for the current memory rules.

3. Add Java/logic support for the paper rules that need generated aliases or
   heap helpers.

  The runnable `.key` subset covers bounded int array index reads/writes,
  dynamic-array push/pop length updates, push-return assignment desugaring, pop
  nonempty/defaulting, and the `revert();` modality rules. Source-level memory
  rules now cover heap field/index reads and writes, root aliasing, memory
  allocation with selected initializers, fixed-length memory array allocation,
  and lazy storage-memory copies through `copySt`/`copyMem`.
  Increment/decrement
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
- `memory-decl-fresh.key` and `memory-root-delete-fresh.key` cover the
  implemented fresh root allocation/rebinding mechanism from `mtMem`.
  `memory-decl-default.key`, `memory-root-alias.key`,
  `memory-field-alias.key`, `memory-deep-field.key`,
  `memory-field-reference-assign.key`, `memory-delete.key`,
  `memory-array-index.key`, `storage-to-memory.key`, and
  `memory-to-storage.key` cover the source-level memory rule family.
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
- `storageIndexWriteMappingSave` / `storageIndexReadMappingFind` for mappings
  and `storageIndexWriteArraySave` / `storageIndexReadArrayFind` for arrays.
  `Path[name=...array]` and `Path[name=...mapping]` keep the rules disjoint;
  array rules branch on `0 <= i < find(storage, sp · size)`, mapping rules do
  not.
- `revertDiamond` / `revertBox`.

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
- `storageIndexWriteMappingCopySource` — `sp1[i] = sp2;` (mapping, `sp2`
  a simple storage path) emits
  `save(storage, consr(sp1Path, at(idx)), find<[Struct]>(storage, sp2Path))`.
  Unbounded twin of `storageIndexWriteArrayCopySource`.
- `storageIndexReadMappingBindLocalRoot` — `lp = sp[i];` (mapping) emits
  `{lp := consr(spPath, at(idx))}`. Unbounded twin of
  `storageIndexReadArrayBindLocalRoot`; also discharges the captured
  `src = sp[i];` declaration left by `storageFieldWrite_unfold_leftFst` on
  mapping-element field writes.
- `storageIndexWriteMapRefRhsCapture` — Step-1 RHS capture, storage twin of
  `memoryIndexWriteMemRefRhsCapture`. A non-simple storage read on the RHS of a
  mapping index write (`sp[i] = nse;`) is captured into a fresh storage alias
  (`T storage src = nse; sp[i] = src;`) so the
  split/`storageIndexReadMappingBindLocalRoot`/`storageIndexWriteMappingCopySource`
  chain can finish. (Latent limitation, shared with the memory clone: a
  non-simple *value* RHS — e.g. an int `valuesMap[2] = valuesMap[1]` — would be
  captured into an ill-sorted `uint storage` alias; no current example exercises
  that shape, which needs the value-capture twin → `…MappingSave` instead.)

These three close `mainFeatures/testStorageMapStructCopy.key`
(`accountMap[2] = accountMap[1];`, a `mapping(uint => Account)` deep struct copy
verified by mutating the source after the copy).

Step-3 rules with **no example and no runnable taclet**:

| Missing rule | Statement shape | Blocking work |
|---|---|---|
| `storageDeleteSimpleTarget` | `delete sp;` | dedicated delete taclet that preserves storage-vs-memory and simple-vs-complex targets (see §"Work Needed" item 2) |
| `storageIndexReadMappingStoreRoot` | `gp = sp[i]` (mapping) | mapping-side taclet |
| `storageIndexWriteArrayCopySource` | `sp1[i] = sp2` (array) | runnable taclet exists for int payloads, but no focused example yet |
| `storageIndexReadArrayStoreRoot` | `gp = sp[i]` (array) | runnable taclet exists, but no focused example yet |
| `storageLocalRootPushBind` | `lp = sp.push();` | runnable taclet exists, but the focused example is not yet part of the verified starter suite |

Recently covered push/pop examples:

| Rule | Example |
|---|---|
| `storagePushValueSave` | `storage-push-value.key` |
| `storagePushValue_unfold_rightSndArgument` + `storagePushValueSave` | `storage-push-nonsimple-arg.key` |
| `storagePushLengthSave` | `storage-push-empty.key` |
| (parse-time push-lvalue desugar) + `storagePushValueSave` | `storage-push-return-assign.key` |
| (parse-time push-lvalue desugar) + `storagePushValueCopySource_unfold_leftFstReceiver` + `storagePushValueCopySource` | `mainFeatures/testStorageComplexReceiverPushLvalueCopy.key` |
| (parse-time push-lvalue desugar, simple receiver) + `storagePushValueCopySource` | `mainFeatures/testStoragePushLvalueCopiesStorageSource.key` |
| `storagePush_unfold_leftFstReceiver` + `storagePushLengthSave` | `mainFeatures/testStorageComplexReceiverEmptyPush.key` |
| `storageFieldWrite_unfold_leftFst` (+ split/push-bind/`storageFieldWriteSave`) | `mainFeatures/testStoragePushFieldLvalue.key` |
| `storageFieldWrite_unfold_leftFst` (+ `storageLocalRootPush_unfold_leftFstReceiver` …) | `mainFeatures/testStorageComplexReceiverPushFieldLvalue.key` |
| `storageFieldRead_unfold_rightFst` (+ split/push-bind/`storageFieldReadBindLocalRoot`) | `mainFeatures/testStorageNestedPushReturnAlias.key` |
| `storagePopSave` nonempty branch | `storage-pop-nonempty.key` |
| `storagePopSave` empty/revert branch | `storage-pop-empty-box.key` |
| `storagePopSave` nonempty branch, bound read post-push | `storage-pop-after-push.key` |
| `storageIndexWriteArraySave_decompose` inBounds branch, bound read post-push | `storage-index-decompose-after-push.key` |
| `storageIndexWriteArrayCopySource` inBounds branch, bound read post-push | `storage-index-copysource-after-push.key` |

The `sp.push().a` shape (a field access on the slot returned by `push()`) needs **no
dedicated push-field rules**: a no-arg `arr.push()` is classified by `PathSVSort` as a
**complex storage path** (rooted at the array receiver, with the array's element type),
so the ordinary complex-path receiver-unfold rules
`storageFieldWrite_unfold_leftFst` (`nsp.a = se;`) and
`storageFieldRead_unfold_rightFst` (`lhs = nsp.a;`) already match it and capture the
push value into a fresh element-typed storage alias (`\newTypeOf` reads the push
call's element type). The capture `T storage t = arr.push();` (also a `StoragePath`
RHS) is split by `storageLocalDeclInitSplit` and push-bound by the existing
`storageLocalRootPushBind` / `storageLocalRootPush_unfold_leftFstReceiver`; the
residual `t.a` field op is finished by the existing `storageFieldWriteSave` /
`storageFieldReadFind` / `storageFieldReadBindLocalRoot`. (Because `push()` is now a
`StoragePath`, the former `storageLocalDeclInitPushSplit` is also redundant and removed.)
The push-lvalue desugar `sp.push() = se ⇝ sp.push(se)` (both the value and
copy-source forms, simple or complex receiver) is performed at **parse time** by
`ParserUtils.parseAssignmentMaybe`: an assignment whose lhs is a zero-arg `.push()`
call is rewritten into a one-arg `.push(value)` `FunctionCallExpression`. Therefore the
`push() = …` shape never reaches the prover. The former taclets
`storagePushLhsToPushValue` / `storagePushLhsToPushValueCopySource` had identical `\find`
and `\replacewith` (both `sp.push(value)`) — they were no-ops the strategy could pick
repeatedly, producing an infinite loop (`testStorageEvaluationOrder`, 3rd push). **They
have been removed**; the parse-time desugar feeds `storagePushValueSave` /
`storagePushValueCopySource` directly, keeping a single applicable rule per statement.

The complex-receiver push-lvalue copy chain (`bucket.tokens.push() = tokRef;`)
parse-desugars to `nsp.push(sp2);`, then
captures the complex receiver into a fresh storage alias
(`storagePushValueCopySource_unfold_leftFstReceiver`, mirroring
`storagePushValue_unfold_leftFstReceiver` but for a simple storage-path
argument), and then appends the copied struct via the terminal
`storagePushValueCopySource`. The terminal now reads the copied payload with
`find<[Struct]>` directly (matching `storageFieldWriteCopySource` /
`storageRootWriteCopySource_struct`) rather than a generic `find<[alpha]>`. The
value-push rules (`storagePushValueSave`,
`storagePushValue_unfold_leftFstReceiver`) were
tightened from a bare `SimpleExpression` to `SimpleExpression[name=value]`, so a
storage-local copy source (`tokRef`) is no longer matched as a value push;
this also fixes a latent over-match that would `save` the path term itself.

`storagePushValue_unfold_rightSndArgument` is `storage.md` §4's Step-1
"unfold the push argument" rule: `sp.push(nse)` with a non-simple argument
`nse` is rewritten to `T pv = nse; sp.push(pv);`, evaluating the argument into
a fresh value local so the terminal `storagePushValueSave` (which requires a
`SimpleExpression`) can fire. It mirrors the existing
`storageIndexWriteNonSimpleIndexCapture` / `addition_unfold_*` capture rules and
stays disjoint from `storagePushValueSave` (literal/value-local arguments) and
`storagePushValueCopySource` (simple storage-path arguments), since
`NonSimpleExpression` matches neither literals nor program variables.

Landing this rule also exposed and fixed a long-standing infrastructure bug:
`FunctionDeclaration.visit(Visitor)` was empty, so `CreatingASTVisitor`'s
per-node `ExtList` was never popped when a method call (e.g. `push`) was
reconstructed inside a statement *sequence*, corrupting the rewrite stack
(`DeclarationStatement cannot be cast to ContextStatementBlock`). It now
dispatches to `performActionOnFunctionDeclaration` like `FieldDeclaration` does.
(This is the "void-call statement-suffix reconstruction bug" the
`testStorageEvaluationOrder` `@Disabled` comment once blamed; it fixed the crash but
was *not* the cause of that example's loop — see the no-op push-lvalue rules above.)

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

## mainFeatures Examples (PaperTest.sol)

The `keyext.solidity.examples/mainFeatures/` directory contains 29 end-to-end
proof problems from `PaperTest.sol` exercising the full storage/memory rule set.
Each `.key` file uses `\programSource "PaperTest.sol"` and inlines the function
body directly in the modality. The JUnit driver is
`PaperTestExamplesTest.java`.

**Passing (19 proofs close automatically):**
- Storage: write/read, nested writes, field deep copy, root deep copy, aliases,
  map read/write/delete, delete paper case, mapping-element struct deep copy
  (`testStorageMapStructCopy`: `accountMap[2] = accountMap[1];`).
- Storage push (lvalue/alias): `testStoragePushLvaluePrimitive`
  (`values.push() = 77; values[0] == 77`) and `testStoragePushReturnAlias`
  (`Token storage t = tokens.push(); t.value = 99; tokens[0].value == 99`).
  Both assume a fresh contract slot via a
  `find<[int]>(storage, cons2(Contract$arr, size)) = 0` precondition (the
  `mainFeatures` problems otherwise start from an unconstrained symbolic
  `storage`, so an array's initial length is unknown).
- Complex-receiver push-lvalue copy: `testStorageComplexReceiverPushLvalueCopy`
  (`bucket.tokens.push() = tokRef;` where `bucket.tokens` is a struct-member
  array and `tokRef` is a `Token storage` reference). Assumes a fresh nested
  slot via `find<[int]>(storage, cons3(PaperTest$bucket,
  PaperTest$TokenBucket$tokens, size)) = 0`. Reads the result back through a
  simple-array alias (`Token[] storage bt = bucket.tokens; bt[0].value`) because
  indexing the complex path `bucket.tokens[0]` directly is not yet supported.
- Remaining push family (all fresh-array preconditioned as above):
  - `testStoragePushLvalueCopiesStorageSource` (`tokens.push() = storageRef;`,
    simple receiver) — the parse-time push-lvalue desugar rewrites this to
    `tokens.push(storageRef);`, feeding the terminal `storagePushValueCopySource`
    directly.
  - `testStorageComplexReceiverEmptyPush` (`bucket.tokens.push();`) — the existing
    `storagePush_unfold_leftFstReceiver` captures the complex receiver; closes now
    that array storage aliases re-sort to `List`.
  - `testStoragePushFieldLvalue` (`tokens.push().value = 11;`) and
    `testStorageComplexReceiverPushFieldLvalue` (`bucket.tokens.push().value = 5;`):
    `push()` classifies as a complex storage path, so `storageFieldWrite_unfold_leftFst`
    captures the `push()` receiver, then the split/push-bind/`storageFieldWriteSave` chain.
  - `testStorageNestedPushReturnAlias`
    (`Account storage acc = people.push().account;`): `storageFieldRead_unfold_rightFst`
    captures the `push()` receiver, then the split/push-bind/`storageFieldReadBindLocalRoot`
    chain.
  - `testStorageArrayReadWrite` (`tokens.push(); tokens[0].value = 100; …`) — now
    closes with the fresh-array precondition (the field-write-on-index path uses the
    post-update-bounds `storageIndexReadArrayBindLocalRoot`).
  - `testStorageEvaluationOrder` (`a.push(100)×3; a[++i] = ++i; assert(a[2] == 1);`,
    fresh-array preconditioned) — exercises Solidity's RHS-before-LHS-index evaluation
    order on a storage array. The new `storageIndexWriteNonSimpleRhsCapture` captures
    the non-simple RHS *first* (`uint _rhs = ++i; a[++i] = _rhs;`), then the existing
    `storageIndexWriteNonSimpleIndexCapture` captures the non-simple index
    (`uint _idx = ++i; a[_idx] = _rhs;`), so `_rhs` binds before `_idx` (giving
    `a[2] == 1`). Ordering is self-enforcing by schema-variable kind: while the RHS is
    non-simple only the RHS-capture matches (the index-capture requires a
    `SimpleExpression` RHS), and once the RHS is the simple temp only the index-capture
    matches — one applicable rule per statement.
- Memory: aliasing, field shallow copy, root alias, delete alias,
  root-delete-rebinds-only-local, delete-identity-field-freshens-slot,
  delete-primitive-field, uint-array auxiliary cases
  (`testMemoryUintArrayAuxiliaryCases`, `carolValues[++i] = 77`), and the full
  inc/dec family in a memory array index — `carolValues[--i]`
  (`testMemoryUintArrayPredecrement`), `carolValues[i++]`
  (`testMemoryUintArrayPostincrement`), `carolValues[i--]`
  (`testMemoryUintArrayPostdecrement`).
- Cross-location: storage→memory copy (root, field, complex path),
  memory→storage copy (root, field, complex source, complex target).

**Disabled — missing taclet support:**
- `testStorageArrayPushPop`: `tokens.push(Token(42));` — the `Token(42)` struct
  literal is not supported by the parser (and there is no push-struct-value taclet).
- `testStorageStructDeleteSkipsMappingMember`: `delete` on a struct must
  preserve mapping members — not modeled in `storageRootDelete`.

**Bugs fixed to reach the passing set:**
- `storageRoot{Pre,Post}{in,de}crementAssignment` (`result = ++age;` etc.) declared their
  receiver as an **unconstrained** `\program Path sp`, so they over-matched *any*
  `vp = ++lv;` — including `vp = ++localValueVar;` where `lv` is a plain local. The
  `\varcond(\sameAsTerm(sp, spPath))` then tried to bind the `\term List spPath` to the
  local's `int`-sorted term, throwing `SortException` and aborting the whole proof. This
  is why `testMemoryUintArrayAuxiliaryCases` (`carolValues[++i] = 77`) could not close: the
  index-capture rule `memoryIndexWriteNonSimpleIndexCapture` emits `uint pv = ++i;`, which
  `valueDeclInitSplit` splits into the assignment form `pv = ++i;` (the storage path keeps
  the decl-init form and is consumed by `localDeclPreincrement`, so it never triggered the
  bug — hence the earlier misdiagnosis of a "missing `++i` memory-index desugaring rule").
  Fixed by constraining the receiver to `\program Path[name=storage.simple.global] sp`,
  matching the non-assignment siblings (`storageRootPreincrement` etc.); the generic
  `localAssignPreincrement` now handles `pv = ++i;`.
- The other three inc/dec operators in a memory array index (`carolValues[--i]`,
  `carolValues[i++]`, `carolValues[i--]`) closed the `SortException` once the rule above was
  constrained, but then stalled with an open goal: only `localDeclPreincrement` /
  `localAssignPreincrement` existed for local value variables, so the captured
  `pv = --i;` / `pv = i++;` / `pv = i--;` had no applicable rule. Added the missing six —
  `localDecl{Pre,Post}decrement` / `localDeclPostincrement` and their `localAssign…` assignment
  twins — mirroring the pre-increment pair (pre forms read the new value, post forms read the
  old value before updating). Covered by `testMemoryUintArray{Predecrement,Postincrement,Postdecrement}`.
- `storageIndexWriteArraySave_root` checked its bounds against the **pre-update**
  storage: the `0 <= idx & idx < find(storage, path·size)` guards were emitted via
  `\add`, which adds them *outside* the pending update `{U}` in front of the modality,
  so `find(storage, …)` read the array length *before* any preceding push/write. Any
  array index write after a mutation wrongly reverted (e.g. `a.push(100); a[0] = 7;`
  checked `0 < len(empty array)`). Fixed by moving the bound *inside* the `\replacewith`
  as the antecedent of an implication, so `{U}` distributes onto it and the length comes
  from the post-mutation storage. The two branches keep the modality-generic revert
  split — `inBounds` is `bound -> {save}…post`, `outOfBounds` is `!bound -> ⟨/[revert]post`
  (vacuously true in `[·]`, forcing in-bounds in `⟨·⟩`) — so the box out-of-bounds case
  (`storage-index-array-out-of-bounds-box.key`) still closes.
- The same `\add`-outside-update bug was then fixed in the three remaining
  array write/pop rules that emitted their bound (or emptiness check) via `\add`:
  `storageIndexWriteArraySave_decompose`, `storageIndexWriteArrayCopySource`, and
  `storagePopSave`. Each now uses the implication-inside-`\replacewith` split of
  `storageIndexWriteArraySave_root`, so the length is read from the post-update
  storage. New diamond regression examples exercise each rule after a preceding
  mutation — `storage-pop-after-push.key` (`values.push(); values.pop();`),
  `storage-index-decompose-after-push.key` (`matrix[0].push(100); matrix[0][0] = 7;`),
  and `storage-index-copysource-after-push.key`
  (`uint[][] storage m = matrix; m.push(); m[0] = values;`, the only construction
  that reaches `storageIndexWriteArrayCopySource`: a local-alias LHS dodges the
  `storage.simple.global.array` competitors `storageIndexWriteArraySave_root` and
  `storageIndexWriteNonSimpleRhsCapture`). All three close only with the fix and
  fail under the reverted `\add` form. The pre-update box revert paths
  (`storage-index-array-out-of-bounds-box.key`, `storage-pop-empty-box.key`) are
  unchanged and still close.
- `Services.getOverlay` was ignoring its `localNamespaces` argument; fixed to
  use the supplied namespace so second skolem constant gets a distinct name.
- `TacletApp.createSkolemConstant` was constructing `SFunction` with
  `unique=false`; fixed to use `(Name, Sort, isRigid=true, unique=true)` so
  `equalUnique` can close `freshIdp_0 = freshIdp_1`.
- `IndexExpression.getType()` was returning the container type
  (`mapping(uint=>uint)`) instead of the element type (`uint`); fixed by
  extracting the element type in the constructor.
- `TypeReference(Type)` constructor was setting `typeName=null`; fixed to
  derive `typeName` from `referencedType.name()`.
- `at(int)` is now declared `\unique Field` in `structHeader.key` so
  `equalUnique` can simplify `at(1) = at(2)` to `false` and close map
  index-disjointness goals.
- Because `at(int)` and `size` are both `\unique Field`, `equalUnique`
  discharges `at(n) = size` to `false` automatically — `at` is *not* special
  among fields. The leftover `& !(at(n) = size)` precondition (a workaround from
  before `at` was unique) was therefore dropped from all eight push/pop/index
  example problems (`storage-push-value.key`, `storage-push-return-assign.key`,
  `storage-push-local-bind.key`, `storage-push-nonsimple-arg.key`,
  `storage-pop-nonempty.key`, `storage-pop-after-push.key`,
  `storage-index-decompose-after-push.key`,
  `storage-index-copysource-after-push.key`); they all still close.
- Array `.length` was unreadable: `SolidityToKeyConverter.visitMemberAccess`
  built a synthetic `length` field with no logic constant. It now lowers
  `arr.length` (on `ArrayType`/`DynamicArrayType` receivers) to the existing
  `size` Field constant with `uint256` type, so the ordinary
  `storageFieldReadFind` rule reads it as `find<[int]>(storage, arr·size)`.
- The array index-**read** rules (`storageIndexReadArrayFind_root`/`_decompose`,
  `storageIndexReadArrayBindLocalRoot`, `storageIndexReadArrayStoreRoot`)
  emitted their bounds via `\add`, which sees the *pre-update* storage. They now
  emit `bounds & {read}⟨rest⟩post` inside the replacement (under the ambient
  update), so bounds are checked in the post-update state. For diamond this is
  exactly `⟨arr[idx];rest⟩post`; for box the conjunction soundly implies
  `bounds -> {read}[rest]post`. (The array index-*write* rules have the same
  latent `\add` issue but are not yet exercised — left as follow-up.)
- Storage aliases of **array/mapping** reference types were not re-sorted to
  `List`. `asStorageAliasType` (in both `SolidityToKeyConverter` and
  `TacletApp#getProgramElement`) only re-sorted types whose sort name was
  `Struct`, but dynamic/fixed arrays and mappings have their own sorts
  (`DynamicArraySort`, `ArraySort`, `MappingSort`). A storage-local array alias
  such as the fresh receiver capture `Token[] storage sp = bucket.tokens;` (or a
  source-level `Token[] storage bt = bucket.tokens;`) therefore kept its array
  sort, and `\sameAsTerm(sp, spPath)` against a `\term List` schema variable
  threw `SortException` ("Sort of SV is not compatible with its instantiation's
  sort"), killing auto mode. Both copies now re-sort any storage-held reference
  collection (struct, array, mapping) to `List` via a shared `isStoragePathType`
  check.
- `storageLocalRootPushBind` (`lp = sp.push();`) emitted its storage bump and
  alias bind as a *sequential* `{storage := …}{lp := …}`, so `lp`'s index
  `find(storage, sp·size)` saw the bumped storage and bound `lp` to
  `at(post-push length)` instead of the new element's `at(pre-push length)`.
  Fixed to a parallel update `{ storage := … || lp := sp·at(n) }`, matching
  `storage.md`. Reading the pushed element back through a literal index
  (`tokens[0]`) now matches.
