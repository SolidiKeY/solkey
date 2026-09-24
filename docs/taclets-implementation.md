# Taclet Implementation Status

What is runnable today vs. pending. Rules live in
`keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/solidityProgramRules.key`
(loaded via `standardSolidityRules.key`). Runnable examples are the functions of the single
contract `keyext.solidity.examples/TestSuite.sol` — the focused, one-rule-each ones run in
`TacletStarterExamplesTest`, the end-to-end `test*` ones in `PaperTestExamplesTest`. There are
no `.key` problem files: the loader synthesizes one obligation per function,

```
\programSource "<abs path>/TestSuite.sol";
\problem { \<{ storageFieldWriteRead()@TestSuite; }\>(true) }
```

(a function with parameters additionally gets a `\programVariables` block declaring one
unconstrained variable per parameter, passed as the call's arguments — such a function is
box-tagged and assumes its argument values with `require`)

so the specification lives in the body as `assert`, and every test program is real Solidity
that `solc` parses and type-checks. `TacletCoverageTest` checks that every taclet of the Solidity
rule files is applied by one of these functions or by a saved proof in
`keyext.solidity.examples/proofs/`. Authoring conventions, the `/// @custom:key` directives and
the `proofs/` directory are described in `keyext.solidity.examples/README.md`. See "Function-body inlining" below for the shape
constraints this imposes.

- Authoring syntax → `key-taclets.md`
- Calculus spec → `storage.md`, `memory.md`
- Open backlog (next constructs to implement) → `taclet-ideas.md`
- `require`/`assert` branch behavior → `require-assert.md`
- The `net` ledger, `msg`, `transfer` → `net.md`
- Rule names and section banners → `scripts/taclet.sh --list` / `--index`
- Commands to run any of this → `AGENTS.md`

Sort hierarchy (declared across the `*Header.key` files; `StValue`/`MemValue`/
`Prim` in `solidityDLHeader.key`): values storable in storage have sort
`StValue` (`Struct` + `Prim`), values storable in memory have sort `MemValue`
(`Identity` + `Prim`); `Prim` (`int`, `bool`, contract sorts) extends both.
Accordingly `storeSt`/`save` take `StValue`, `write` takes `MemValue`, and
`copySt` takes `Struct` as last argument. The array and mapping sorts
`SolJSONParser` creates per declared type (`uint256[]`, `uint256[3]`,
`mapping(bool => int256)`) extend **`Struct`**, not `StValue` directly: the value
at such a path *is* a struct node at run time — `mtSt` extended with `at(i)`
fields, which is why `memoryStorageCopy` reads it `find<[Struct]>` — so every
`selectSt`/`find`/`save`/`copySt` rule, all stated on `Struct`, applies to it.
Siblings of `Struct` under `StValue` would be incomparable with it, and a
generic bound at such a sort would produce terms no rule could consume.
`SolJsonParserTest#arrayAndMappingSortsExtendStruct` pins this down. Taclets
whose generic sort flows into a value position use the bounded generics
`alphaSt`/`alphaMem` (`solidityProgramRules.key`).

Field selectors are partitioned by what the member holds (`structHeader.key`),
stamped at parse time by `SolJSONParser#fieldSortFor`:

```
Field           value members, `size`, `at(i)`   delete resets them to their default
├── MapField    mapping members, `atMap(i)`      delete preserves their entries
├── RefField    struct/dynamic-array references  delete recurses into them
└── FixedField  fixed-size array members         delete resets their elements, keeps `size`
```

`atMap(i)` is the index of an array element whose type is a mapping. Only the rules that
reach such an element emit it: `storageIndexReadArrayBindLocalRootMappingElement`,
`storageLocalRootPushBindMappingElement` and `storagePopSaveMappingElement`, which take the
Path flag `mappingElement`. Solidity cannot read, write or copy a mapping, and mappings never
live in memory, so no other rule builds a path to one.

Only the two kinds `delete` must positively recognise get a sub-sort; value
members stay plain `Field` alongside `size` and `at(i)` (an index's element
sort comes from the container rather than from the index).
`selectStDelNodeDefault` therefore stays Field-generic on the *field* —
restricting it to a value-member sub-sort would leave `(delete w).arr.length`
stuck, which `SolcStructs.deleteStructResetsArrayLength` pins down. It is kept
off the struct-sorted reads by its *sort* generic instead: the read cast is
bound `alphaPrim \extends Prim`, so a `<[Struct]>` read is a failed
instantiation rather than a ranked race (the `simplify_enlarging` ranking is
performance-only). Struct-sorted element reads
`selectSt<[Struct]>(delNode(st), at(i))` — plain-`Field` index, so neither the
`MapField` nor the `RefField` rule matches — get their own
`selectStDelNodeIndexStruct`, which re-wraps the element exactly like the `RefField`
rule (`delNode(selectSt(st, at(i)))`), so `delete arr` keeps the mapping members of
struct elements. It re-wraps only the elements below the array's old length
(`i < selectSt(st, size)`): `delete arr` clears no further, so data a dangling reference left
past the length survives it (`testDeleteArrayLeavesDataPastLength`).

`MapField` is the sub-sort that earns its place: it is the only case that reads
*through* the delete marker (`selectSt(st, mf)`) instead of re-wrapping it
(`delNode(selectSt(st, rf))`) or resetting. Classifying a mapping member as a
reference would leave its entries under the marker and they would read as
defaults — losing exactly the "delete preserves mappings" rule.

The path-shape split: root rules use
`SimpleStoragePath`; field/index paths use `Path[...]` schema variables placed
directly in the resulting terms (the engine lowers the matched AST to logic);
complex member/index receivers stay as structural `.key` source patterns and are
captured into fresh storage aliases by `_unfold_leftFst` / `_unfold_rightFst`
rules before a terminal rule fires.

## Implemented

Operator families that differ only by operator token (`op(a,b) = c` instances —
compound assignment, inc/dec, tier-1 arithmetic) are listed under `// generalization:`
comments in `solidityProgramRules.key`, verified mechanically by
`RuleGeneralizationTest` — see `docs/rule-generalizations.md`.

### Storage read / write / copy
Root, field (member access, any depth) and index rules, each with a terminal
and a `_unfold_leftFst` / `_unfold_rightFst` receiver capture for complex paths
(`scripts/taclet.sh --list` for the names, `--index` for the section banners).
Notes that are not derivable from the rule names:

- Field constants are namespaced `Contract$Struct$field` and `\unique`;
  `Services.memberFieldTerm` reconstructs them by walking the member chain.
- Array index rules branch on `0 <= i < find(storage, sp·size)` with an
  `outOfBounds` goal that executes `revert();` (reads and writes alike); mapping
  rules do not. All index terminals take a simple receiver, so every target and
  source shape is reached through one alias step.
- A bare contract root on the right-hand side is a `FieldReference`, which
  `SimpleExpression` excludes but `Expression[primitive]` admits, so
  `nsp.a = gsp` / `nsp[i] = gsp` are ordinary receiver-capture members, after
  which the `…CopySource` terminals fire.
- The location keyword in a schematic declaration pattern is matched against the
  concrete variable's `DataLocation`, so there is one `…LocalDeclInitDrop` per
  location; `storageLocalDeclSkip` / `valueDeclSkip` consume bare declarations.
- Mapping-carrying copies are rejected at the front end, not by the rules: solc
  ≥ 0.7 refuses assignments whose target type transitively contains a mapping,
  so `ParserUtils.parseAssignmentMaybe` throws for them (storage-pointer rebinds
  `lsv = sp` stay legal), and both parsers reject `memory` declarations of
  mapping-carrying types (`StorageReferenceTypes.containsMapping`). The copy
  taclets themselves stay unconditional — the illegal shapes never reach them.

### Increment / decrement (`++`/`--`, pre/post, plain and `result = …`)
Direct storage updates (no program-level desugaring), e.g. `++age;` ⇝
`{storage := save(storage, path, find<[int]>(storage, path) + 1)}`. Full
root / field / index matrix, pre and post, plain and `…Assignment`, plus
`…_unfold_leftFst` for complex receivers. The indexed forms are split by the
receiver's sort like the plain index rules: the array form carries the
`inBounds` / `outOfBounds` (`revert();`) goal pair, the mapping form does not.
Operator matching checks the operator enum (not just the AST node class) so
`+=` does not match `=`. Annotated as the `storageIncDec` / `localIncDec` /
`memoryIncDec` families (`docs/rule-generalizations.md`).

The memory twins are the same rules with `find`/`save` replaced by
`read`/`write` — see "Memory arithmetic" for why that matrix has no root and no
mapping member.

### Compound assignment
`+=`, `-=`, `*=`, `/=`, `%=` at root / field / index, each with a terminal and a
`_unfold_leftFst` for complex receivers (the array form carrying the bounds goal
pair of `storageIndexWriteArraySave`). `/=` and `%=` guard with
`\if(se != 0)\then(…)\else(revert)`; integers are unbounded mathematical
integers, so there is no overflow guard. Bitwise `&= |= ^= <<= >>=` parse but
are deferred (no bitwise LDT). Listed under `// generalization:` comments.

### Memory arithmetic
Each memory arithmetic rule is its storage counterpart with
`find<[int]>(storage, path)` / `save(storage, path, v)` replaced by
`read<[int]>(memory, mv, sel)` / `write(memory, mv, sel, v)` — a single update,
no program-level desugaring, and no varcond, since an arithmetic context is
always `int`-carried (see "Stores can defer their sort; reads cannot").

Two axes of the storage matrix are absent by construction, not by omission:
there is **no root form**, because a memory root variable holds an `Identity`
and never an int cell, and **no mapping form**, because memory has no mappings
(`docs/memory.md`). The matrix is therefore `{field, indexArray}`.

The indexed terminals state their bounds the way `memoryIndexWriteArray` does —
`\sameUpdateLevel` plus `\add(0 <= i & i < read<[int]>(memory, mv, size) ==>)`
on the `inBounds` goal and the negation on `outOfBounds` — where the storage
twins use an implication inside `\replacewith`. That difference is real, so the
memory indexed rules have their own `// generalization:` comments rather than
rows in the storage ones.

No new capture rules were needed: `addAssignValueRhsCapture` and its siblings
take a plain `Expression` target and so already cover a memory left-hand side.
A non-simple index under a compound assignment has no capture rule on either
side and stays unsupported.

### Tier-1 expression operators
Mirror the `+`/`==` families: terminal assigns the logic-level result, non-simple
operands are captured by `_unfold_left/right` (arith) or `…CaptureLhs/Rhs`
(relational/logical), in the order given under §Operand evaluation order below.
The former `<op>_unfold_result` rules (`nlhs = se1 OP se2`)
are gone — a non-simple write target is now served by the per-statement RHS
captures (§Capture partition below), which also cover nested RHS like
`total = x + y*z;`. Uses plain LDT ops (`sub`, `mul`, `div`, `mod`, `pow`,
`neg`) over unbounded mathematical integers — overflow is not modeled
(`keyext.solidity.examples/unprovable/Unprovable.sol` documents the divergence
from the EVM's checked arithmetic); `/` and `%` revert on a zero denominator.
- Arithmetic: `-`, `*`, `**` (`pow`), `/`, `%` — listed under `// generalization:`
  comments (`docs/rule-generalizations.md`).
- Relational: `!=`, `<`, `>`, `<=`, `>=` (predicate map `lt/leq/gt/geq`).
- Logical / unary: `&&`, `||`, `!`, unary `-x`. A non-simple left operand is
  captured eagerly (Solidity always evaluates it); a non-simple *right* operand
  must NOT be captured — that would evaluate it unconditionally, breaking
  short-circuit semantics (e.g. `x != 0 && 10 / x > 1` would grow a spurious
  revert branch). Java KeY (`compound_assignment_3/5_nonsimple`) rewrites to an
  `if`-`else` statement instead; lacking `if` rules, `logicalAnd/OrShortCircuitRhs`
  perform that split directly at the sequent level (`\add(se = TRUE/FALSE ==>)`),
  continuing with `v = nse;` on the branch that reaches the right operand and
  `v = false;` / `v = true;` on the short-circuit branch
  (examples `logicalAndShortCircuitRhs` / `logicalOrShortCircuitRhs`).
- Operand evaluation order: solc compiles the **right** operand of a binary
  operation before the left (`libsolidity/codegen/ExpressionCompiler.cpp`,
  `visit(BinaryOperation)` — the left-first branch fires only when the right
  operand is a literal, so it never changes semantics). The capture partition
  reproduces that order: `_unfold_right` / `…CaptureRhs` take an arbitrary left
  operand and capture the non-simple right one first, so the both-non-simple
  case lands there too; `_unfold_left` / `…CaptureLhs` fire only once the right
  operand is simple, and snapshot it into a temporary *before* the left
  operand's side effects run, using two fresh variables (`pv1`, `pv2`). Without
  that snapshot the calculus proves `x == 3` for `uint i = 1; x = i++ + i;`
  where the EVM computes `2`. Examples `additionLeftImpureRightReadFirst`,
  `additionRightImpure`, `additionBothOperandsImpure`,
  `subtractionLeftImpureRightReadFirst`, `lessThanLeftImpureRightReadFirst`.
  `&&`, `||` and `?:` are excluded — they are left-/condition-first by language
  semantics, not by codegen. Note that via-IR evaluates left-to-right, so these
  examples pin the legacy pipeline the runtime cross-check compiles with.
- Conditional operator `?:`: `ternaryCaptureCond` hoists a non-simple
  condition (always evaluated); `ternaryToIf` (and `ternaryToIfStorage`, its
  twin for a storage-path target, which is not a `Variable`) rewrites the
  remaining `v = se ? e1 : e2;` to `if (se) v = e1; else v = e2;`, handing the
  split to the `if` rules below.
- `if` / `if`-`else` statements: `ifUnfold` / `ifElseUnfold` hoist a non-simple
  guard into a fresh variable; `ifSplit` / `ifElseSplit` are the Java KeY
  `ifElseSplit`-style sequent-level two-goal split (`\find( ==> \modality...)`
  with `\add(se = TRUE/FALSE ==>)`, the update context applied to the added
  guard). The sequent-level shape is deliberate: Java KeY's formula-level
  `if`/`ifElse` taclets have their heuristics commented out — automation there
  also runs on the sequent-level `ifElseSplit`, and a formula-level `\if`
  variant tried here made `additionStorageWrite` diverge past 10k nodes.
- Guard simplifiers, in `concrete_solidity` so they outrank the split and the
  unfold: `ifTrue` / `ifFalse` / `ifElseTrue` / `ifElseFalse` drop the dead
  branch of a literal guard outright, where `ifSplit` would leave a second,
  trivially closed goal; `ifElseNegated` rewrites
  `if (!se) thenStm else elseStm` to `if (se) elseStm else thenStm`, so the
  negation never has to be captured into a fresh
  variable by `ifElseUnfold`. `concrete_solidity` was a declared but unused
  rule set (`ruleSetDeclarations.key`, costed in `SymExStrategy`); these five
  are its first members. Matching a literal guard needs `BoolLiteral` to have
  value equality — `Literal#match` compares with `equals`, and the two parsers
  disagree on identity (the `.key` path builds a fresh instance, the `.sol`
  path returns the `TRUE`/`FALSE` singletons) — so `BoolLiteral` overrides
  `equals`/`hashCode` like `Uint256Literal` does.
- Deferred: bitwise (`& | ^ << >> ~`), unary `+` (removed in Solidity ≥0.5).
  Overflow is not modeled: integers are unbounded, and examples that depend on
  the EVM's checked-arithmetic revert live in
  `keyext.solidity.examples/unprovable/`.

### Storage aliases
`storageLocalDeclInitDrop` (decl-with-init decomposition) followed by
`storageLocalRootRebind` (standalone `lsv = sp;`). The alias binds to the **path**,
not the value: `{lsv := cons1(rhsField)}` or `{lsv := pathFields}`. Two enablers:
`SolidityToKeyConverter#asStorageAliasType` re-sorts any storage-held reference
collection (`Struct`, array, mapping) to `List`; value-reads and path-rebinds
stay disjoint through the read side, not the target variable — the value-read
rules require a primitive source (`Path[storage,simple,primitive]` root,
a member whose `\hasFieldSort(a, \sort(alphaPrim))` varcond binds — the
`alphaPrim \extends Prim` bound rejects reference-typed members — or a
`Path[...,primitiveElement]` indexed receiver),
while rebinds keep their `Variable[storage]` target.

### Mapping indices (nesting, aliases, memory keys)
Nothing beyond the index families above was needed for the weirder mapping shapes. A sweep over
nested mappings, arrays of mappings, mappings of structs that themselves carry a mapping,
mappings of arrays, storage pointers into a mapping or into one nested row, keys read out of
another mapping, and keys or values crossing the memory border found no rule missing: every one
closes. `TestSuite.sol`, section "Mapping indices: nesting, aliases and memory keys", keeps one
example per shape that the older sections do not already cover —
`mapping(k => mapping(k => v))`, `mapping(uint => uint)[]`, `mapping(uint => Ledger)`, a
nested row bound as a storage pointer, a ternary choosing between two mappings,
`balances[balances[1]]`, a memory field as the key, and a mapping entry copied to another
through memory. The shapes Solidity refuses outright are recorded in
`keyext.solidity.examples/illegal/IllegalMappings.sol`, and of the two gaps the sweep did find, the
non-integer key crash is in `bugs.md` and the other is in `taclet-ideas.md` ("Raised by the
mapping-index probe").

### Push / pop
Push-lvalue `sp.push() = se` is desugared to `sp.push(se)` at **parse time**
(`ParserUtils.parseAssignmentMaybe`), so it never reaches the prover. A no-arg
`arr.push()` is classified as a **complex storage path**, so the ordinary
complex-receiver unfold rules capture its return slot — no dedicated push-field
rules needed. Terminals: `storagePushValueSave`, `storagePushValueCopySource`
(sort-free `find<[StValue]>`), `storagePushLengthSave`, `storagePopSave` (nonempty + empty/
revert branch), `storagePushValue_unfold_rightSndArgument` (non-simple *primitive*
argument capture on a simple receiver — a path-shaped argument stays for
`storagePushValueCopySource`, which consumes `tokens.push(tok)` without an
intervening alias), `storage{Push,…}_unfold_leftFstReceiver` (a complex
receiver is aliased first, whatever the argument — `storagePushValue_unfold_leftFstReceiver`
takes any `Expression`). Array bounds/length are read
from **post-update** storage (bound emitted inside `\replacewith`, not via `\add`).
`storagePopSave`'s nonempty branch clears the popped slot with the mapping-preserving
`delAt(storage, sp · at(ℓ-1))` (not eager `defaultValue`), reusing the `delNode`
machinery of §Delete — so a mapping nested in the popped element survives `pop()` and a
later `push()`, exactly like `storageRootDelete` / `storageFieldDelete`.
`storagePushLengthSave` clears the appended slot the same way for a value-type element, which
is what makes `arr.push(); assert(arr[0] == 0);` provable on unknown storage. A push onto an
array of structs or arrays (`storagePushLengthSaveReferenceElement`, `storageLocalRootPushBind`)
does not clear: solc writes no zeroes on `push()`, and a write through a dangling reference to a
popped element survives it (`testDanglingReferenceSurvivesPush`,
`testDanglingInnerArrayReappearsAfterPush`). See the `testDeepPopDoesNotResetMappingMember` end-to-end
example. `delAt(st, p)` names `st` once where the equivalent `save`-of-deleted-value form
named it twice; reads commute through it with `selectOnDelAtCons`, and the reset still
resolves on read through `delField<[alpha]>`.

### Array length non-negativity (`sizeNotNegative`)
`sizeNotNegative` (`structRules.key`) is the SolKey twin of Java KeY's
`arrayLengthNotNegative`: `\find(selectSt<[int]>(st, size)) \sameUpdateLevel`
adds `0 <= selectSt<[int]>(st, size)` to the antecedent, in ruleset
`inReachableStateImplication` (declared in `ruleSetDeclarations.key`; cost +100
and `NonDuplicateAppModPositionFeature` bound in `SolidityDLStrategy`'s cost and
approval dispatchers, so the add-only rule fires once per length term and cannot
loop when formulas move). One static rule suffices because every length read the
program rules emit — `find<[int]>(storage, consr(sp, size))` — normalizes to a
`selectSt<[int]>(st, size)` leaf via the `consr` eliminators and
`findDefinitionCons`. Sound by the external invariant that `size` cells are only
written by push (`n+1`) and pop (`n-1` under `0 < n`); see `docs/storage.md`
§8b. It is what closes `storagePopUnknownLength`, `storagePushLengthPositive`
and `storagePushReadBack` without a `require` on the length. Landing it also
required order-guarding the `cnf_andComm`/`cnf_orComm` sorting rules in
`FOLStrategy` (`termSmallerThan`, mirroring `key.core`'s `JFOLStrategy`) —
unguarded, `commute_and` ping-pongs on a succedent conjunction and starves
every positive-cost rule.

### Memory
Source-level memory family covers heap field/index read & write, root aliasing,
fresh allocation (`memoryReferenceDeclFreshAlloc`, with a `new(memory, r)` skolem
branch), fixed-length array allocation (`memoryArrayFreshAlloc`, assignment form
`mv = new T(len);`), primitive-default vs. reference-slot
delete (`memoryRootDeleteFreshRebind` and field/index delete), and lazy
storage↔memory copies via `copySt` / `copyMem` (`memoryStorageCopy` for
`m = <simple storage path>;`, `memoryStorageCopyUnfold` captures a complex
storage RHS in a local storage pointer first; in the other direction
`memoryToStorage{StoreRoot,FieldCopyRoot,FieldCopyField}` and the indexed
terminals `memoryToStorageIndex{Mapping,Array}CopyRoot` — the array form
with the `inBounds`/`outOfBounds` goal pair — save
`copyMem(mtSt, memory, ·)` at the target path). Declarations with initializer
never reach these rules: `memoryLocalDeclInitDrop` (memory-only, the `memory`
keyword in the pattern is matched) rewrites `T memory m = x;`
to `m = x;` (registering `m` as a program variable), so all memory terminals
match plain assignments; only the bare `T memory m;` keeps its one-shot
fresh-allocation semantics. `memoryAssignForms` covers the assignment
forms directly. Memory references are
`Identity`-sorted, not copied `Struct` values; no `push`/`pop`/mapping.
Complex memory receivers are captured first by `memoryIndexRead_unfold_rightFst`
/ `memoryIndexWriteCaptureAll` / `memoryIndexDelete_unfold_leftFst`. These
take a plain `Path[memory,complex]` (no `array` flag): a receiver capture uses no
array structure, and in memory an indexable path is an array anyway — mappings
cannot be memory-located and `bytes`/`string` are not memory reference types. The
`array` flag stays on the rules that do consume it (`memoryIndexWriteArray`,
`memoryIndexReadArray*`, `memoryIndex*Delete`), which emit the `size` bound.
`memoryStructArrayIndex` covers the complex-receiver path.

### Delete
`storageRootDelete`, `storageFieldDelete`, `storageIndexDelete` (mappings) and
`storageIndexArrayDelete` (arrays, with an `outOfBounds` goal that executes `revert();`) save
the sort-free `delAt(storage, path)` marker, resolved on read (see "Sort-free clearing and
copying" below). A deleted collection entry/element therefore keeps its mapping members, like
any other deleted struct (`testDeleteArrayDoesNotResetElementMappingMember`).
The last field of the path decides what is reset: `delField<[alpha]>(st, f)` is field `f` of
`st` after the delete. A primitive sort (`alphaPrim \extends Prim`) collapses to
`defaultValue<[alphaPrim]>` (`int→0`, `bool→FALSE`). A `RefField` or an `at(i)` element becomes
a lazy `delNode` marker, a `FixedField` a `delNodeFixed` marker whose `size` reads through
(`testFixedArrayDeleteKeepsLength`), and a `MapField` stays as it is (structRules.key). A read
at `StValue` itself is routed by `delFieldStValueCast` (below). `pop()` on an array of mappings
only shortens it (`storagePopSaveMappingElement`, `testPopKeepsMappingElementEntries`).

An array whose elements are fixed-size arrays (`uint[3][]`) is out of reach: its elements are
read through `at(i)`, which carries no field kind. So `delete` and `pop()` refuse to reset one:
the Path flag `noFixedArrayElement` and the variable condition `\noFixedArrayElement(fld)`
(`StorageDeleteTypes`) leave such a proof open instead of proving it wrong. This gives Solidity's
`delete` semantics on structs: value/reference members reset, but **mapping members
are preserved**. On read, `selectSt` on a `delNode` reads a mapping member
(`MapField`) through to the original struct, recurses into a reference member
(`RefField`, so nested structs' mappings also survive), and resets a primitive leaf.
Member kinds are told apart by `Field` sub-sorts stamped on the field constants at
parse time (`SolJSONParser#fieldSortFor`, keyed off the member's Solidity type):
`MapField` for mappings and `RefField` for struct/array references. Every other
field constant — value members, `size`, `at(i)` — carries the base `Field` sort
and is reset by the Field-generic default rule.

`selectStDelNodeMap` and `selectStDelNodeRef` match their sub-sorts directly;
`selectStDelNodeDefault` stays Field-generic (because `size` and `at(i)` are
plain `Field`) but is Prim-bounded on the read sort (`alphaPrim`), which keeps
it disjoint from the `<[Struct]>` rules by sort rather than by ranking; the
struct-sorted `at(i)` element read has its own `selectStDelNodeIndexStruct`.
(`delField` uses the same discipline — its `Struct` rules match the concrete
`Struct` sort, and `delFieldDefault` is `alphaPrim`-bounded so a struct payload
is a failed instantiation. The former `StValue` bounds made the default rules
overlap the `Struct` rules, with only the `simplify`/`simplify_enlarging`
ranking — strategy guidance, not a soundness gate — keeping the
mapping-erasing rewrite away; both rules were applicable to the same term,
an inconsistency in the rule set.) Complex receivers unfold first (`…_unfold_leftFst`). Storage and memory
deletes stay separate by design.

### Sort-free clearing and copying

A rule that clears or copies a location needs no sort at all: the sort is resolved **on
read**, using the cast that `selectOnStore` (and `readOnWrite` in memory) already inserts.
The read rules are where a sort is genuinely needed, and they bind it at matching time with
the sort-binding varconds (`\hasSort` / `\hasFieldSort` / `\hasElementSort` and their
memory twins) — see "Stores can defer their sort; reads cannot" below.

Four sort-free shapes carry the deferred value:

| Symbol | Means | Resolved by |
|---|---|---|
| `delAt(Struct, List)` | the storage with a location reset — a struct keeps its mapping members | `delAtEmpty` / `selectOnDelAtCons` |
| `find<[StValue]>(Struct, List)` | the value at a path, for copies (`find` at the top storage sort) | `findStValueCast` |
| `delField<[StValue]>(Struct, Field)` | a reset field a sort-free copy carried out of a cleared location | `delFieldStValueCast` |
| `save(Struct, nil, StValue)` | the leaf of a write, never collapsed — a struct written over a location keeps the location's mapping members; an array element is copied below the source's length, reset below the old length and kept past both | `selectOnSaveEmpty{Map,Ref,IndexStruct,Default}` / `saveOnEmptyPrim` |
| `defVal` | a location reset outright, mapping members included | `defValResolve` |

`defVal` is declared `Prim`, so it is both an `StValue` and a `MemValue` and serves storage
and memory alike — the same arrangement `defaultValue<[alpha]>` already has (declared once in
`memoryRules.key`, resolved by `defaultValueInt`/`defaultValueBool` there and
`defaultValueStruct` in `structRules.key`). A separate memory twin is not needed.

`selectOnDelAtCons` defers to `delField<[alpha]>(st, f)` for the reset value, so it inherits
the `delField` split: the concrete `Struct` cases choose by the kind of `f` (`RefField` and
`at(i)` recurse via `delNode`, `FixedField` via `delNodeFixed`, `MapField` stays), the default
case is `alphaPrim`-bounded, and the two are disjoint by sort — their
`simplify`/`simplify_enlarging` ranking is performance-only. `defVal` is deliberately distinct
from `delAt`: it resets a location outright, mapping members included, and no delete rule
writes it.

Disjoint, but not exhaustive: `selectOnDelAtCons` instantiates its generic at the *reader's*
sort, and a sort-free copy reads at `StValue`, which is neither `Struct` nor `\extends Prim`.
`delete sp; gsp = sp;` therefore stops at `delField<[StValue]>(…)` until
`delFieldStValueCast` — the twin of `findStValueCast` for that shape — pushes the read's cast
inward: `cast<[alphaSt]>(delField<[StValue]>(st, f))` ⇝ `delField<[alphaSt]>(st, f)`.
The `alphaSt` it binds is the sort the read supplies, so one of the `delField` rules then
matches. Widening `delFieldDefault` to `StValue` would close the same gap by reintroducing the
overlap the paragraph above records; pushing the cast keeps the split. The three
`*DeleteThenCopy` examples all fail without it.

`save` never collapses its leaf. Every storage-to-storage copy (`storageRootWriteCopySource`,
`storageFieldWriteCopySource`, `storageIndexWrite{Array,Mapping}CopySource`,
`storagePushValueCopySource` and the three `…StoreRoot` reads) writes
`save(storage, target, find<[StValue]>(storage, source))`, and `find<[StValue]>` carries the
source's mapping members with it — but Solidity never copies a mapping, so there is no
`save(st, nil, v) ⇝ v`. `selectOnSaveCons` walks the target path like `selectOnDelAtCons` and,
at the location itself, leaves `(alpha) save(<old value>, nil, <written value>)` at the reader's
sort. The leaf is then resolved by what is read from it: a `MapField` member comes from the old
value (`selectOnSaveEmptyMap`), a `RefField` member is again a leaf one level down
(`selectOnSaveEmptyRef`, since a nested struct may carry a mapping), an element `at(i)` and every
primitive member come from the written value (`selectOnSaveEmptyIndexStruct`,
`selectOnSaveEmptyDefault`), and a leaf cast to a primitive sort *is* the written value
(`saveOnEmptyPrim`). At `Struct` or `StValue` the cast is `castDel`, a `save` being
`Struct`-sorted. `selectStValueCast` collapses `cast<[alphaSt]>(selectSt<[StValue]>(st, a))`,
which the `.key` shape below reaches when `findDefinitionCons` unfolds the written value before
the cast collapses. The same leaf sits under a memory-to-storage copy,
`save(storage, p, copyMem(mtSt, memory, mv))`, so `structMemoryRules.key` reads `copyMem` one
selector at a time (`selectOnCopyMemPrim`, `selectOnCopyMemRef`) beside the whole-path
`findOnCopy`. No `.sol` example can exercise the mapping half — both front ends reject a copy
whose type carries a mapping — so `keyext.solidity.examples/storage/copyKeepsMapping.key` pins it
and the `testCopy*` group of `TestSuite.sol` pins the mapping-free half of every leaf rule.

`delAt` names its storage argument once, where the `save(st, p, <deleted value at p>)` form it
replaced named it twice. That doubled the storage term at every `push`/`pop`, so a sequence of
them grew exponentially: `SolcArrays.pushThenPopRestoresLength` peaked at a 6183-node sequent,
93% of it under the deleted-value marker. It now peaks at 589 and proves in 1.1s instead of 4.1s.

**Stores can defer their sort; reads cannot.** A stored value is read later, and the read
supplies the sort through its cast. A read rule has to *produce* a value of the target
variable's sort, and nothing in the taclet language names that sort — so every primitive
read rule recovers it with a matching-time varcond: `\hasSort(sp, \sort(alphaPrim))` on
`storageRootReadSelect`, `\hasFieldSort` on `storageFieldReadFind`, `\hasElementSort` on
the `storageIndexRead{Array,Mapping}Find` variants, and the memory twins
(`\hasMemoryFieldSort` / `\hasMemoryElementSort`) on `memoryFieldRead` /
`memoryIndexReadArrayValue`. `alphaPrim \extends Prim` keeps a struct-typed match
inapplicable rather than mistyped. The store-position copies (`storageRootWriteCopySource`
and the `…StoreRoot` rules) carry the value as sort-free `find<[StValue]>` inside `save` instead; `size` reads and
all arithmetic contexts keep `find<[int]>`, since every numeric Solidity type shares the
`int` carrier sort (only `bool` has a distinct primitive sort).

### Capture partition (non-simple RHS / index, storage.md Steps 1–2)
Non-simple constituents are hoisted into fresh locals by a disjoint rule family,
partitioned by the RHS's *static type* so each capture picks the right variable
kind:
- **Value RHS** (`NonSimpleExpression[primitive]`: operator-shaped,
  primitive-typed, not path-shaped) → fresh plain `Variable`. The
  `…ValueRhsCapture` rules; the field and index ones are location-neutral.
- **Reference-path RHS** (`Path[…,complex,reference]`) → fresh storage/memory
  alias. The `…RefRhsCapture` / `…CaptureSrc` rules. Primitive-typed complex
  paths are excluded (they go through the value-read captures below).
- **Value path RHS into a non-simple target** (paper `unfold_rightSndResult`):
  `storage{Field,Index}Read_unfold_rightSndResult`,
  `memory{Field,Index}Read_unfold_rightSndResult` — `nlhs = sp.a;` /
  `nlhs = sp[se];` with `nlhs : Path[complex,primitive]` capture the read into a
  value temp, then the plain write rules fire. `memoryToStorageFieldCopyField`
  is restricted to reference-typed members by its
  `\hasMemoryFieldSort(b, \sort(alphaId))` varcond so primitive members take
  this route instead.
- **Non-simple index and non-simple receiver** (paper `unfold_leftSnd` /
  `unfold_leftFst` / `rightSndIndex`). A write `op(recv, i) = rhs` is decomposed
  by three rules partitioned on which constituent is not yet simple, each
  capturing in the EVM's order — **right-hand side, then receiver, then index**:
  - **Rule 1, field write with a complex receiver** (`Path[…,complex]`):
    captures the RHS and aliases the receiver. `nsp.a = e;` ⟹
    `rvType rv = e; aliasType storage sp = nsp; sp.a = rv;`. Five members, the
    `…Field…_unfold_leftFst` rules, one per location × primitive/reference RHS.
  - **Rule 2, index write with a non-simple receiver or index**: five
    `…CaptureAll` rules, one per RHS kind, which capture RHS, receiver and index
    in a single application. `p[ie] = e;` ⟹ `rvType rv = e;
    aliasType storage sp = p; pvType pv = ie; sp[pv] = rv;`. The receiver is
    snapshotted even when it is simple: a local storage pointer can be
    reassigned by the index expression, and the write must land where the
    receiver pointed before the index ran. Their receiver SV is a `Path` of any
    simplicity, so a disjunctive side condition keeps them off the fully simple
    `sp[se] = e` (which they would re-match forever): the varcond
    `\notAllSimple(p, ie)` (`parser/varcond/NotAllSimpleCondition.java`). On
    `nsp[se] = rhs` the index temporary is redundant, a few extra nodes.
  - **Rule 3, receiver and index simple, RHS non-simple**: the RHS capture
    rules of the two bullets above, plus the root-target
    `storageRootWriteValueRhsCapture`.

  Examples: `indexWriteBothImpure{StorageRef,MemToStorage,MemoryValue,MemRef}`
  cover the both-impure shape for every RHS kind.

  What differs between the members of a rule is only the *declaration* the
  capture emits, which follows the right-hand side's kind: `T rv = e;` for
  `Expression[primitive]`, `T storage rv = src;` for `Path[storage,reference]`,
  `T memory rv = src;` for `Path[memory,reference]`. `Expression[primitive]` is
  primitive-typed and not a *complex* path; a complex path such as `p.age` has
  its own receiver resolved by the read unfolds first, which also evaluates it
  ahead of the target.

  The value snapshot is what keeps the order: without it the calculus proves
  `a[0] == 1` for `a[i++] = i;` where the EVM writes `0`, and relaxing the
  receiver sort without it reproduces the Lean model's `fieldWrite_not_sound`
  evaluation-order bug (`lean/solidity/…/Counterexamples/EvaluationOrder.lean`).
  A reference source needs no snapshot for order — a reference is bound, not
  read — but is captured anyway, so one rule covers a source of any shape.

  Reads and deletes have no right-hand side and capture receiver then index
  (the `…_unfold_rightFst` / `…_unfold_rightSndIndex` rules).
  A non-simple index under a compound assignment (`a[i++] += x`) still has no
  capture rule; a compound assignment with an impure *receiver*
  (`persons[i++].age += i`) is handled, its RHS snapshotted like Rule 1's.

  **Nesting recurses; it is not enumerated.** `PathSVSort.classify` places no
  purity requirement on an index, so `matrix[i++]` is an ordinary
  `Path[storage,complex]` and the same rules apply at every level. The alias
  declaration a capture emits is stripped to a plain assignment by
  `*DeclInitDrop`, which re-enters the unfolds — so each step peels one level
  and the decomposition reaches any depth (`matrix[i++][i++] = k`,
  `testNestedIndexWriteImpureReceiverAndIndex`).
  **No depth-keyed rule belongs in this file**; `grep '\]\['` over
  `solidityProgramRules.key` must stay empty.

  Soundness rests on the capture order, not on the sort: no rule evaluates a
  receiver before its right-hand side, and no `Path` SV lacking the `simple`
  flag is ever lowered into a term or an update. The binary operand rules follow
  the same principle one level down — the right operand is captured before the
  left, matching solc (§Tier-1 expression operators, "Operand evaluation
  order"). The one rule that emits such a
  path twice is `ternaryToIfStorage`, whose two occurrences are in mutually
  exclusive `if`/`else` branches, so the path is resolved exactly once per trace.

  Their `// generalization:` comments are checked by `RuleGeneralizationTest`, so
  the storage and memory halves cannot drift apart again.
Also `storageIndexReadMappingStoreRoot` closes the paper's §11 table
(`gsp = sp[i]` for mappings, no bounds branch).

### Paths and lowering
Path SV sorts: `StoragePath`, `SimpleStoragePath`, `ComplexStoragePath`,
`MemoryPath`, `SimpleMemoryPath`, `ComplexMemoryPath`, plus precise
`Path[...]` with comma-separated flags (`storage`/`memory`, `simple`/`complex`,
`root`/`field`/`index`, `array`/`mapping`, `primitive`/`reference`,
`local`/`global`). Roots are simple; member/indexed paths and no-arg `arr.push()`
are complex. The sort places no purity requirement on an index, so
`matrix[i++]` is an ordinary complex path; what keeps "lowerable by
`convertToLogicElement`" intact is that only `simple`-flagged Path SVs ever
occur in a term or update position, and a simple path is a root. The
`primitive`/`reference` flags filter by the path's
static type; for index expressions rebuilt during taclet instantiation the type
is re-derived from the base's element type (`PathSVSort.typeOf`).
`NonSimpleExpression[primitive]` filters non-simple expressions to
operator-shaped, primitive-typed, non-path ones (`NonSimpleExpressionSVSort`);
`SimpleExpression[primitive]` analogously restricts literals/variables to
primitive static type (`SimpleExpressionSVSort`); `Expression[primitive]`
(`ExpressionSVSort`) is the union a capture rule needs — primitive-typed and not
a *complex* path, so literals, primitive variables, operator expressions and
bare contract roots, but not `p.age`. Path schema
variables used directly in `\replacewith`/`\add` term
positions lower to logic `List` terms automatically; indexed segments lower to
`at(index)` (sort `Field`); `arr.length` lowers to the `size` field. A `push()`
path is only ever captured via `\newTypeOf`, never lowered directly.

## End-to-end examples (the `test*` functions)

`TestSuite.sol` holds 64 end-to-end `test*` functions driven by `PaperTestExamplesTest.java`;
each is called with postcondition `true`, the obligations being carried by in-body `assert`s.
The other 223 functions are the focused starters run by `TacletStarterExamplesTest`.

**Passing (most close automatically):** storage write/read, nested + deep copy,
aliases, mapping read/write/delete, struct-`delete` preserving mapping members
(`testStorageStructDeleteSkipsMappingMember`), mapping-element struct deep copy
(`testStorageMapStructCopy`); the full push/pop family
(`testStoragePush*`, `testStorageComplexReceiverPush*`, `testStorageArrayReadWrite`,
`testStorageEvaluationOrder`, `testMemoryEvaluationOrder` — RHS-before-LHS index
order, and `test{Storage,Memory,Nested}IndexWriteImpureIndex*` for the value
snapshot an impure index forces); memory aliasing,
delete, and inc/dec in array indices (`testMemoryUintArray*`); cross-location
storage↔memory copies. Push examples assume a fresh-slot precondition
(`require(tokens.length == 0);` under `/// @custom:key box`) since execution starts from
unconstrained symbolic storage.

**Dropped — missing taclet support:**
- `testStorageArrayPushPop` — `tokens.push(Token(42));`, struct-literal `Token(42)`
  not parsed (no push-struct-value taclet). It had no `TestSuite.sol` function and was
  removed with the `.key` files.

## solc semantic-test ports (`keyext.solidity.examples/solc/`)

Six contracts porting the Solidity compiler's own semantic tests
(`ethereum/solidity`, `test/libsolidity/semanticTests/`) into the `require`/`assert` style, run
by `SolcSemanticsExamplesTest` — an external cross-check of the calculus against a description
of the language SolKey did not write. Provenance table, adaptation rules and the not-ported
families are in `keyext.solidity.examples/solc/README.md`.

All 72 functions close. The port found seven gaps no `TestSuite.sol` example reached; all were
fixed, and their examples are now regression tests. See "Rules added or corrected by the solc
port" below:

- `++`/`--` and `+=` **as a statement on a local variable**; and a **non-simple RHS in a
  compound assignment** at any location (`total += a * 16;` was open for storage too).
- **Indexing a storage alias of a primitive-element array** (`uint[] storage ref = arr;
  ref[1]`).
- **Whole-value copy into an array or mapping element** (`pairs2[0] = src;`,
  `arrayMap[0] = row;`), which raised a `TermCreationException`.
- **`?:` over memory references assigned into a storage target**, a `SolJSONParser` typing bug.
- **A freshly pushed slot was not known to be zero** — `push` bumped `size` without touching
  the appended slot.

Two more are worked around in the examples rather than fixed: `SolJSONParser` throws on a
**self-recursive struct type** (`struct s2 { mapping(k => s2) recursive; }`), and a **mapping
that is a struct member has to be bound to an alias before it can be indexed** (`map[4].z`
closes where `nested.recursive[4].z` does not).

### Rules added or corrected by the solc port

- `local{Pre,Post}{in,de}crement` — `++`/`--` on a local as a statement of its own, the twins
  of `storageRoot{Pre,Post}{in,de}crement`. The pre-existing `localDecl…`/`localAssign…` rules
  only cover the result forms (`uint p = a++;`).
- `local{Add,Sub,Mul,Div,Mod}Assign` — compound assignment on a local; `/=` and `%=` carry the
  same zero-denominator revert branch as their storage twins.
- `{add,sub,mul,div,mod}AssignValueRhsCapture` — hoist a non-simple RHS out of a compound
  assignment. Location-neutral (an `Expression` target, like `fieldWriteValueRhsCapture`), so
  one rule per operator covers local and storage root/field/index targets.
- `ternaryToIfStorage` — the `ternaryToIf` twin for a storage-path target, which is not a
  `Variable`.
- `storageIndex{Read,Write}{Array,Mapping}*` no longer require the `global` flag. They
  were the only members of the index family that did, which left a `simple`+`local` alias root
  matching no rule. (Their former `_decompose` twins for a complex receiver are gone: the
  receiver is now aliased by `storageIndex{Read,Write}_unfold_{rightFst,leftFst}`.)
- The index-write *save* rules now take `SimpleExpression[primitive]` for the value, as
  `storageRootWriteStore` already did. With an unrestricted `SimpleExpression` a storage-alias
  variable matched and `save(…, path)` was built ill-sorted.
- `storageIndexWrite{Array,Mapping}CopySource` carry the copied value sort-free, as
  `find<[StValue]>`, instead of hard-coding `int` / `Struct` — the sort arrives with the
  read (see "Sort-free clearing and copying").
- `storagePushLengthSave` clears the appended slot of a value-type array as well as bumping
  `size`, mirroring `storagePopSave` (arrays of structs or arrays use
  `storagePushLengthSaveReferenceElement`, which does not clear). It writes the lazy delete marker rather than an eager `defaultValue`, so the
  reset resolves by sort: a primitive element becomes 0 (which is what makes
  `arr.push(); assert(arr[0] == 0);` provable), while a struct element becomes a
  `delNode` whose mapping members still read through — so
  `testDeepPopDoesNotResetMappingMember`, where a bare `push` is asserted *not* to install an
  empty element, keeps closing. Both rules carry the marker as `delAt(storage, path)`, which names
  `storage` once rather than twice, so the term grows
  linearly in the number of push/pop operations instead of doubling at each one — peak term size
  for `SolcArrays.pushThenPopRestoresLength` fell from 6183 to 589, and its proof from 4.1s to
  1.1s.
- `SolJSONParser.parseConditional` types a `?:` from its branches instead of always `bool`,
  matching `SolidityToKeyConverter`. The wrong type made a reference-valued ternary look
  primitive, so it was captured into a `bool` temp and symbolic execution stalled.

## Function-body inlining

`functionBodyExpand` (in `solidityProgramRules.key`, so examples need not declare it) rewrites
a call statement to the callee's body via the `ExpandFunctionBody` transformer, which emits

```
T0 p0 = arg0; ... Tn pn = argn; Tr r0; { <body> } result = r0;
```

`blockEmpty` then discards the body block once its statements have run. Constraints that
follow from the current implementation — every example in `TestSuite.sol` respects them:

- **The call must be the whole modality program.** `functionBodyExpand`'s `\find` is a bare
  `s#fbs`, not a context block, so `\<{ f()@C; g()@C; }\>` does not match.
- **Exactly one named return.** Only `freshReturns.get(0)` is wired to the result variable; a
  second named return is silently dropped.
- **No `return e;`.** It parses, but no taclet consumes a `ReturnStatement`, so symbolic
  execution gets stuck. Assign to the named return instead.
- **No overloading.** `visitFunctionBodyStatement` takes the first function whose *name*
  matches, ignoring the signature, so function names must be unique.
- **Single-identifier left-hand side.** `(a, b) = f()@C;` is not parseable.

A test that observes more than one value therefore asserts in the body and uses postcondition
`true`; a test that observes only storage/memory has no return value at all.

Note that a `.sol` body is parsed by `SolJSONParser` (the solc-JSON path), not by
`SolidityToKeyConverter` (the ANTLR path used for programs written inline in a modality). Both
paths now handle `msg.sender`, `msg.value`, `.transfer` and `.send`, which is why the `net-*`
examples load their programs from the `.sol` beside them via `\programSource`.

## Synthesized obligations from `@custom:key` specifications

A `.sol` function whose contract carries `@custom:key invariant` clauses, or which carries
`@custom:key requires`/`ensures` clauses itself, is proved against them: `SolidityProblemSynthesizer`
compiles the clauses (`speclang/natspec/`: `KeyNatspec` splits the natspec text, `SpecParser`
parses an expression with the ANTLR grammar `SolSpec.g4`, `SpecCompiler` — a visitor over
that parse tree — emits the `.key` term text) into the same generated `.key`
problem the `net/*.key` files spell out by hand — an `insertCInv` rewrite taclet defining
`CInv(s, n)` as the conjoined invariants, and the ISoLA 2020 eq.-4 problem: `msg.value` bound,
requires, `CInv(storage, net)` in the antecedent; the ledger booking
`net := storeSt(net, at(msgSender), …)` as update; the call in the **box** modality; `CInv`
and the ensures as postcondition. `\old(e)` declares `Struct old, oldNet` and snapshots them in
the update; a named return declares `int result` and calls `result = f()@C;`. A function
without any clause keeps the plain `(true)` obligation byte for byte, so `TestSuite.sol` and
the `solc/` ports are unaffected. `./run-key.sh F.sol -f fn --print-problem` prints the text.

A parameter of type `uint`/`int`, `address`/`address payable` or an enum is declared `int`, a
`bool` one `bool`; `public` and `external` functions both get an obligation.

A quantified invariant keeps its bound variables as plain logic variables in the taclet's
`\replacewith` (`\forall int a; …`), guarded by `\varcond(\noFreeVarIn(s), \noFreeVarIn(n))`.
They used to be declared as `\schemaVar \variables`, but then the binder and its occurrences
were instantiated apart, `all_unused` dropped the quantifier, and every quantified invariant
was unprovable. Two calculus repairs from that attempt remain: `TacletPrefixBuilder` now gives
a `\noFreeVarIn` schema variable an empty prefix (its instantiation is closed, so it may sit
under any binder), and `TacletApp` instantiates a `\variables` schema variable that occurs only
in `\replacewith` with a fresh `BoundVariable` (it used to throw). `GenericSortCondition` also accepts a plain `InstantiationEntry` holding a
term, which is what `createSkolemConstant` records — before, every `exLeft`/`allRight` skolem
failed the generic-sort check, so no quantified problem could be proved. The surface grammar
and the emission table are in `keyext.solidity.examples/README.md`.

`trueNotFalse` / `falseNotTrue` (`formulaNormalizationRules.key`) rewrite `TRUE = FALSE` and
`FALSE = TRUE` to `false`. The bool literals had no distinctness axiom, so an infeasible branch
of `if (p != 0)` could end with `TRUE = FALSE` in the antecedent and stay open
(`real-world/SimpleAuction.sol`'s `bid`).
