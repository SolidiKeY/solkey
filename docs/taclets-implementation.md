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
that `solc` parses and type-checks. Authoring conventions and the `/// @custom:key` directives
are in `keyext.solidity.examples/README.md`. See "Function-body inlining" below for the shape
constraints this imposes.

- Authoring syntax → `key-taclets.md`
- Calculus spec → `storage.md`, `memory.md`
- Open backlog (next constructs to implement) → `taclet-ideas.md`

Sort hierarchy (declared across the `*Header.key` files; `StValue`/`MemValue`/
`Prim` in `solidityDLHeader.key`): values storable in storage have sort
`StValue` (`Struct` + `Prim`), values storable in memory have sort `MemValue`
(`Identity` + `Prim`); `Prim` (`int`, `bool`, contract sorts) extends both.
Accordingly `storeSt`/`save` take `StValue`, `write` takes `MemValue`, and
`copySt` takes `Struct` as last argument. Array/mapping sorts created by
`SolJSONParser` extend `StValue`; taclets whose generic sort flows into a value
position use the bounded generics `alphaSt`/`alphaMem`
(`solidityProgramRules.key`).

Field selectors are partitioned by what the member holds (`structHeader.key`),
stamped at parse time by `SolJSONParser#fieldSortFor`:

```
Field           value members, `size`, `at(i)`   delete resets them to their default
├── MapField    mapping members                  delete preserves their entries
└── RefField     struct/array references          delete recurses into them
```

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
`selectStDelNodeIndexStruct`.

`MapField` is the sub-sort that earns its place: it is the only case that reads
*through* the delete marker (`selectSt(st, mf)`) instead of re-wrapping it
(`delNode(selectSt(st, rf))`) or resetting. Classifying a mapping member as a
reference would leave its entries under the marker and they would read as
defaults — losing exactly the "delete preserves mappings" rule.

The split follows the `solidity-key-taclets` skill: root rules use
`SimpleStoragePath`; field/index paths use `Path[...]` schema variables placed
directly in the resulting terms (the engine lowers the matched AST to logic);
complex member/index receivers stay as structural `.key` source patterns and are
captured into fresh storage aliases by `_unfold_leftFst` / `_unfold_rightFst`
rules before a terminal rule fires.

## Implemented

Operator families that differ only by operator token (`op(a,b) = c` instances —
compound assignment, inc/dec, tier-1 arithmetic) carry `// generalized by:`
annotations in `solidityProgramRules.key`, verified mechanically by
`RuleGeneralizationTest` — see `docs/rule-generalizations.md`.

### Storage read / write / copy
- Root: `storageRootWriteStore`, `storageRootReadSelect`,
  `storageRootWriteCopySource` (sort-free `find<[StValue]>`, primitives and structs alike).
- Field (member access, any depth): `storageFieldWriteSave`,
  `storageFieldReadFind`, `storageFieldWriteCopySource`,
  `storageFieldReadBindLocalRoot`, `storageFieldReadStoreRoot`, plus
  `_unfold_leftFst` / `_unfold_rightFst` receiver capture for complex paths.
  Field constants are namespaced `Contract$Struct$field` and `\unique`;
  `Services.memberFieldTerm` reconstructs them by walking the member chain.
- Index: array rules branch on `0 <= i < find(storage, sp·size)`, mapping rules
  do not. `storageIndexWrite{Array,Mapping}Save`,
  `storageIndexRead{Array,Mapping}Find`, plus `…CopySource`, `…BindLocalRoot`,
  `…StoreRoot`, and `_decompose` variants for nested indexed paths.
- Local declarations: the location keyword in a schematic declaration pattern
  is matched against the concrete variable's `DataLocation`, so
  `localValueDeclInitDrop` / `storageLocalDeclInitDrop` /
  `memoryLocalDeclInitDrop` (one per location) drop a declaration-with-
  initializer to the plain assignment and register the variable;
  `storageLocalDeclSkip` / `valueDeclSkip` consume bare declarations.
- Mapping-carrying copies are rejected at the front end, not by the rules:
  solc ≥ 0.7 refuses assignments whose target type transitively contains a
  mapping, so `ParserUtils.parseAssignmentMaybe` throws for them (both parse
  paths; storage-pointer rebinds `lp = sp` stay legal), and both parsers
  reject `memory` declarations of mapping-carrying types
  (`StorageReferenceTypes.containsMapping`). The copy taclets themselves stay
  unconditional — the illegal program shapes never reach them.

### Increment / decrement (`++`/`--`, pre/post, plain and `result = …`)
Direct storage updates (no program-level desugaring), e.g. `++age;` ⇝
`{storage := save(storage, path, find<[int]>(storage, path) + 1)}`. Full matrix
at root / field / index level: `storage{Root,Field,Index}{Pre,Post}{in,de}crement`
and their `…Assignment` twins, plus `…_unfold_leftFst` for complex receivers.
Local-value twins `localDecl…` / `localAssign…` cover captured temporaries. All
24 focused examples close. Operator matching checks the operator enum (not just
the AST node class) so `+=` does not match `=`. Annotated as the
`storageIncDec` / `localIncDec` families (`docs/rule-generalizations.md`).

### Compound assignment
`+=`, `-=`, `*=`, `/=`, `%=` at root / field / index, each with a terminal and a
`_unfold_leftFst` for complex receivers: `storage{Root,Field,Index}{Add,Sub,Mul,Div,Mod}Assign`.
`/=` and `%=` guard with `\if(se != 0)\then(…)\else(revert)`; integers are
unbounded mathematical integers, so there is no overflow guard. Bitwise
`&= |= ^= <<= >>=` parse but are deferred (no bitwise LDT). Annotated as the
`storageCompoundAssign` / `localCompoundAssign` / `compoundAssignRhsCapture`
families (`docs/rule-generalizations.md`).

### Tier-1 expression operators
Mirror the `+`/`==` families: terminal assigns the logic-level result, non-simple
operands are captured by `_unfold_left/right` (arith) or `…CaptureLhs/Rhs`
(relational/logical). The former `<op>_unfold_result` rules (`nlhs = se1 OP se2`)
are gone — a non-simple write target is now served by the per-statement RHS
captures (§Capture partition below), which also cover nested RHS like
`total = x + y*z;`. Uses plain LDT ops (`sub`, `mul`, `div`, `mod`, `pow`,
`neg`) over unbounded mathematical integers — overflow is not modeled
(`keyext.solidity.examples/unprovable/Unprovable.sol` documents the divergence
from the EVM's checked arithmetic); `/` and `%` revert on a zero denominator.
- Arithmetic: `-`, `*`, `**` (`pow`), `/`, `%` — annotated as the `binaryOp`
  family (`docs/rule-generalizations.md`).
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
- Conditional operator `?:`: `ternaryCaptureCond` hoists a non-simple
  condition (always evaluated); `ternarySplit` is the Java KeY
  `ifElseSplit`-style sequent-level two-goal split
  (`\find( ==> \modality...)` with `\add(se = TRUE/FALSE ==>)`, the update
  context is applied to the added guard) continuing with `v = e1;` / `v = e2;`.
  The sequent-level shape is deliberate: Java KeY's formula-level `if`/`ifElse`
  taclets have their heuristics commented out — automation there also runs on
  the sequent-level `ifElseSplit`, and a formula-level `\if` variant tried here
  made `additionStorageWrite` diverge past 10k nodes
  (examples `ternaryCaptureCond` / `ternarySplit`).
- Deferred: bitwise (`& | ^ << >> ~`), unary `+` (removed in Solidity ≥0.5).
  Overflow is not modeled: integers are unbounded, and examples that depend on
  the EVM's checked-arithmetic revert live in
  `keyext.solidity.examples/unprovable/`.

### Storage aliases
`storageLocalDeclInitDrop` (decl-with-init decomposition) followed by
`storageLocalRootRebind` (standalone `lp = sp;`). The alias binds to the **path**,
not the value: `{lp := cons1(rhsField)}` or `{lp := pathFields}`. Two enablers:
`SolidityToKeyConverter#asStorageAliasType` re-sorts any storage-held reference
collection (`Struct`, array, mapping) to `List`; value-reads and path-rebinds
stay disjoint through the read side, not the target variable — the value-read
rules require a primitive source (`Path[storage,simple,primitive]` root,
a member whose `\hasFieldSort(a, \sort(alphaPrim))` varcond binds — the
`alphaPrim \extends Prim` bound rejects reference-typed members — or a
`Path[...,primitiveElement]` indexed receiver),
while rebinds keep their `Variable[storage]` target.

### Push / pop
Push-lvalue `sp.push() = se` is desugared to `sp.push(se)` at **parse time**
(`ParserUtils.parseAssignmentMaybe`), so it never reaches the prover. A no-arg
`arr.push()` is classified as a **complex storage path**, so the ordinary
complex-receiver unfold rules capture its return slot — no dedicated push-field
rules needed. Terminals: `storagePushValueSave`, `storagePushValueCopySource`
(`find<[Struct]>`), `storagePushLengthSave`, `storagePopSave` (nonempty + empty/
revert branch), `storagePushValue_unfold_rightSndArgument` (non-simple argument
capture), `storage{Push,…}_unfold_leftFstReceiver`. Array bounds/length are read
from **post-update** storage (bound emitted inside `\replacewith`, not via `\add`).
`storagePopSave`'s nonempty branch clears the popped slot with the mapping-preserving
`delAt(storage, sp · at(ℓ-1))` (not eager `defaultValue`), reusing the `delNode`
machinery of §Delete — so a mapping nested in the popped element survives `pop()` and a
later `push()`, exactly like `storageRootDelete` / `storageFieldDelete`.
`storagePushLengthSave` clears the appended slot the same way, which is what makes
`arr.push(); assert(arr[0] == 0);` provable for a primitive element while a struct
element keeps its mapping members. See the `testDeepPopDoesNotResetMappingMember` end-to-end
example. `delAt(st, p)` names `st` once where the equivalent `save`-of-deleted-value form
named it twice; reads commute through it with `selectOnDelAtCons`, and the reset still
resolves by sort on read through `delValue<[alpha]>`.

### Storage wellformedness (`wellFormed`)
`wellFormed(Struct)` (`structHeader.key`) is the assumption that storage matches the
contract's declared layout — the calculus twin of the Lean formalization's
`wellTypedStorageB L storage`, sound to assume once because
`TypeSoundness.execBlock_preserves_wellTyped` proves it inductive. It is uninterpreted,
like `CInv`; `WellFormedTacletGenerator` (`proof/init/`) generates the per-contract
`wellFormedExpand` rewrite taclet from the state variables, emitting
`0 <= find<[int]>(storage, p)` for each `uintN` cell and
`0 <= find<[int]>(storage, p · size)` for each array length cell, recursing into struct
members and quantifying over mapping keys. Obligations opt in: `/// @custom:key wellformed`
on a `.sol` function (both directives share one tag: `/// @custom:key box wellformed`), or
`wellFormed(storage) -> …` plus a hand-written expansion in a `.key` problem. Without it
nothing bounds a cell the proof never wrote, which is why the array examples pin their
length with `require`. The `wellFormed*` functions of `TestSuite.sol` are exactly the
examples that close only with it — `values.push(); values.pop();` (`storagePopSave`'s
`"empty"` branch needs `0 <= n` under a diamond), `values.push(); assert(values.length > 0)`,
the push/read-back round trip, and `uint`-typed root, struct-member and mapping reads.
Limits: array *elements* are not descended into (would need the index quantified under the
length bound; no example needs it), no upper bound is stated (no checked arithmetic to use
it), and the callback rules havoc `storage` without re-assuming it. See `docs/storage.md`
§8b.

### Memory
Source-level memory family covers heap field/index read & write, root aliasing,
fresh allocation (`memoryReferenceDeclFreshAlloc`, with a `new(memory, r)` skolem
branch), fixed-length array allocation (`memoryArrayFreshAlloc`, assignment form
`mp = new T(len);`), primitive-default vs. reference-slot
delete (`memoryRootDeleteFreshRebind` and field/index delete), and lazy
storage↔memory copies via `copySt` / `copyMem` (`memoryStorageCopy` for
`m = <simple storage path>;`, `memoryStorageCopyUnfold` captures a complex
storage RHS in a local storage pointer first). Declarations with initializer
never reach these rules: `memoryLocalDeclInitDrop` (memory-only, the `memory`
keyword in the pattern is matched) rewrites `T memory m = x;`
to `m = x;` (registering `m` as a program variable), so all memory terminals
match plain assignments; only the bare `T memory m;` keeps its one-shot
fresh-allocation semantics. `memoryAssignForms` covers the assignment
forms directly. Memory references are
`Identity`-sorted, not copied `Struct` values; no `push`/`pop`/mapping.
Complex memory receivers are captured first by `memoryIndexRead_unfold_rightFst`
/ `memoryIndexWrite_unfold_leftFst` / `memoryIndexDelete_unfold_leftFst`. These
take a plain `Path[memory,complex]` (no `array` flag): a receiver capture uses no
array structure, and in memory an indexable path is an array anyway — mappings
cannot be memory-located and `bytes`/`string` are not memory reference types. The
`array` flag stays on the rules that do consume it (`memoryIndexWriteArray`,
`memoryIndexReadArray*`, `memoryIndex*Delete`), which emit the `size` bound.
`memoryStructArrayIndex` covers the complex-receiver path.

### Delete
`storageRootDelete` and `storageFieldDelete` save the sort-free `delAt(storage, path)`
marker, resolved on read (see "Sort-free clearing and copying" below).
(`storageIndexDelete` saves `defVal` instead: deleting a single collection
entry/element resets it outright, mapping members included — the
`storage-index-delete-mapping-struct` starter asserts the whole entry `= mtSt`.)
`delValue` picks the reset value by sort: any
non-struct sort collapses to `defaultValue<[alpha]>` (`int→0`, `bool→FALSE`), while
a struct becomes a lazy `delNode` marker (structRules.key). This gives Solidity's
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
(`delValue` uses the same discipline — `delValueStruct` matches the concrete
`Struct` sort, and `delValueDefault` is `alphaPrim`-bounded so a struct payload
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

Three sort-free symbols carry the deferred value:

| Symbol | Means | Resolved by |
|---|---|---|
| `delAt(Struct, List)` | the storage with a location reset — a struct keeps its mapping members | `delAtEmpty` / `selectOnDelAtCons` |
| `find<[StValue]>(Struct, List)` | the value at a path, for copies (`find` at the top storage sort) | `findStValueCast` |
| `defVal` | a location reset outright, mapping members included | `defValResolve` |

`defVal` is declared `Prim`, so it is both an `StValue` and a `MemValue` and serves storage
and memory alike — the same arrangement `defaultValue<[alpha]>` already has (declared once in
`memoryRules.key`, resolved by `defaultValueInt`/`defaultValueBool` there and
`defaultValueStruct` in `structRules.key`). A separate memory twin is not needed.

`selectOnDelAtCons` defers to `delValue<[alpha]>` for the reset value, so it inherits the
`delValueStruct`/`delValueDefault` split: the concrete `Struct` case recurses via `delNode`,
the default case is `alphaPrim`-bounded, and the two are disjoint by sort — their
`simplify`/`simplify_enlarging` ranking is performance-only. `defVal` is deliberately distinct
from `delAt` — it is what `storageIndexDelete` writes, which resets a collection element
outright rather than preserving its mapping members.

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
and the `…StoreRoot` rules) carry the value as sort-free `find<[StValue]>` instead; `size` reads and
all arithmetic contexts keep `find<[int]>`, since every numeric Solidity type shares the
`int` carrier sort (only `bool` has a distinct primitive sort).

### Capture partition (non-simple RHS / index, storage.md Steps 1–2)
Non-simple constituents are hoisted into fresh locals by a disjoint rule family,
partitioned by the RHS's *static type* so each capture picks the right variable
kind:
- **Value RHS** (`NonSimpleExpression[primitive]`: operator-shaped,
  primitive-typed, not path-shaped): `storageRootWriteValueRhsCapture`
  (`gp = nse;`), `fieldWriteValueRhsCapture` (`e.a = nse;`, location-neutral),
  `indexWriteValueRhsCapture` (`e1[e2] = nse;`, location-neutral) → fresh plain
  `Variable`.
- **Reference-path RHS** (`Path[…,complex,reference]`):
  `storageIndexWriteStorageRefRhsCapture`, `storageFieldWriteCaptureSrc`,
  `memoryIndexWriteMemRefRhsCapture`, `memoryFieldWriteCaptureSrc` → fresh
  storage/memory alias. Primitive-typed complex paths are excluded (they go
  through the value-read captures below).
- **Value path RHS into a non-simple target** (paper `unfold_rightSndResult`):
  `storage{Field,Index}Read_unfold_rightSndResult`,
  `memory{Field,Index}Read_unfold_rightSndResult` — `nlhs = sp.a;` /
  `nlhs = sp[se];` with `nlhs : Path[complex,primitive]` capture the read into a
  value temp, then the plain write rules fire. `memoryToStorageFieldCopyField`
  is restricted to reference-typed members by its
  `\hasMemoryFieldSort(b, \sort(alphaId))` varcond so primitive members take
  this route instead.
- **Non-simple index** (paper `unfold_leftSnd` / `rightSndIndex`), all with
  `Path[storage]` / `Path[memory]` bases (any simplicity/origin/kind):
  `storageIndexWriteNonSimpleIndexCapture`, `memoryIndexWriteNonSimpleIndexCapture`,
  `storageIndexWriteRootRhsNonSimpleIndexCapture` (root-reference RHS),
  `storageIndexRead_unfold_rightSndIndex`, `memoryIndexRead_unfold_rightSndIndex`,
  `storageIndexDeleteNonSimpleIndexCapture`, `memoryIndexDeleteNonSimpleIndexCapture`.
  Depth-2 inner indices (`e1[nse][e2]`) have dedicated location-neutral captures
  `indexWriteInnerNonSimpleIndexCapture` / `indexReadInnerNonSimpleIndexCapture`;
  NSE indices at depth ≥ 3 or under member bases (`people[k+1].age`) are still
  unsupported.
Also `storageIndexReadMappingStoreRoot` closes the paper's §11 table
(`gp = sp[i]` for mappings, no bounds branch).

### Require / assert
Per `require-assert.md`: `requireConditionCapture` / `requireSimple` and
`assertConditionCapture` / `assertSimple`. `requireSimple` branches
`pv = FALSE | ⟨ω⟩φ` ("Holds") and `pv = TRUE | ⟨revert();⟩φ` ("Reverts") inside
`\replacewith` so the update context binding `pv` is preserved; combined with
`revertDiamond`/`revertBox` this yields `c ∧ φ` (diamond) / `c → φ` (box), while
`assert` keeps `c ∧ φ` in both modalities. `FunctionReference.match` compares
callee names, so `assert`/`require`/`revert` patterns are disjoint (previously
any zero-child `FunctionReference` matched any other). A literal operand
(`require(true)`) still matches neither rule (pre-existing `assert` gap).

### Payments (`net` ledger, `msg`, `transfer`)
`docs/net.md` Steps 1–4. `netHeader.key` declares the
program variables `Struct net` (per-address ledger: read
`selectSt<[int]>(net, at(a))`, empty ledger `mtSt` ⇒ `net(a) = 0`),
`msgSender`, `msgValue`, `self`, and `selfBalance` (the contract's own funds),
plus the uninterpreted contract-invariant
predicate `CInv(Struct, Struct)` over `(storage, net)` (solidiKeY-style: each
problem file gives it meaning via its own `insertCInv` rewrite taclet).
`CInv` deliberately stays binary: the callback havoc leaves `selfBalance`
unconstrained rather than carrying it in the invariant.
`msg.sender` / `msg.value` desugar to
`msgSender` / `msgValue` in `SolidityToKeyConverter.visitMemberAccess`
(shadowable by a local named `msg`). `transfer` and `send` are registered
builtins classified like `push`/`pop` (`MemberExp` + `FunctionCallExpression`,
no dedicated AST node). Transfer semantics is the taclet choice
`transferSemantics:{noCallback, withCallback}` (`optionsDeclarations.key`,
default `noCallback`; examples pin the other variant with
`\withOptions transferSemantics:withCallback;`). Each semantics is split by
modality: the box rules book the debit unconditionally (a reverting run is
trivially correct under partial correctness, so no funds check), while the
diamond rules owe the EVM balance check `0 <= v & v <= selfBalance` as a
"sufficient funds" goal — an unfunded diamond transfer is unprovable. Under
`noCallback`: `transferNoCallbackBox`
(`a.transfer(v);` ⇝ `{selfBalance := selfBalance − v ||
net := storeSt(net, at(a), selectSt<[int]>(net, at(a)) − v)} …`) and
`transferNoCallbackDiamond` (the same booking plus the "sufficient funds"
goal). Under `withCallback`: `transferWithCallbackBox` splits into
"invariant on exit" (book the debit, prove `CInv(storage, net)`, drop the
continuation) and "resume after callback" (havoc `storage`, `net`, and
`selfBalance` with skolem constants, assume `CInv`, continue);
`transferWithCallbackDiamond` adds the leading "sufficient funds" goal and
keeps the guard as an assumption on the other two branches. All share the
`transfer_unfold_leftFstReceiver` / `transfer_unfold_rightSndArgument`
captures. The PO pattern credits the incoming payment to the balance
alongside the ledger:
`{net := storeSt(net, at(msgSender), … + msgValue) || selfBalance :=
selfBalance + msgValue}`. Not yet done: the `\getContractInvariant` varcond +
`ContractSpecification` plumbing (`docs/net.md` Step 4 phase 2),
`send`/`call{value:}` rules, `address(this).balance` reading `selfBalance`,
and proof-obligation plumbing (`docs/net.md` Step 5). Examples: the `net-*`
starters and the per-contract `*-invariant.key` / `*-withcallback.key` POs in
`keyext.solidity.examples/net/`, driven by `NetExamplesTest`; the starters
run in box with no funding premises (`net-transfer-unfunded.key` pins the
unconditional booking), the `net-transfer-*diamond-funded.key` starters
discharge the diamond funding obligation, and
`NetExamplesTest#unfundedDiamondStaysOpen` asserts the unfunded-diamond
problems under the core test resources `examples/open/` stay open.

### Paths and lowering
Path SV sorts: `StoragePath`, `SimpleStoragePath`, `ComplexStoragePath`,
`MemoryPath`, `SimpleMemoryPath`, `ComplexMemoryPath`, plus precise
`Path[...]` with comma-separated flags (`storage`/`memory`, `simple`/`complex`,
`root`/`field`/`index`, `array`/`mapping`, `primitive`/`reference`,
`local`/`global`). Roots are simple; member/indexed paths and no-arg `arr.push()`
are complex. An `IndexExpression` whose index is not simple (variable/literal) is
**not** a path at all — it must be normalized by the index-capture rules first,
which keeps "matches a `Path[...]` SV" aligned with "lowerable by
`convertToLogicElement`". The `primitive`/`reference` flags filter by the path's
static type; for index expressions rebuilt during taclet instantiation the type
is re-derived from the base's element type (`PathSVSort.typeOf`).
`NonSimpleExpression[primitive]` filters non-simple expressions to
operator-shaped, primitive-typed, non-path ones (`NonSimpleExpressionSVSort`);
`SimpleExpression[primitive]` analogously restricts literals/variables to
primitive static type (`SimpleExpressionSVSort`). Path schema
variables used directly in `\replacewith`/`\add` term
positions lower to logic `List` terms automatically; indexed segments lower to
`at(index)` (sort `Field`); `arr.length` lowers to the `size` field. A `push()`
path is only ever captured via `\newTypeOf`, never lowered directly.

## Not yet implemented

See `taclet-ideas.md` for the full backlog. Headline gaps:
- Bitwise operators and bitwise compound assignments (need bitwise LDTs).
- Whole-struct write from a struct **value** (`alice = pVal;`) and struct
  literals (`Token(42)`) — need step-1 unfolding for struct constructors.
- Dynamic-array `delete arr;` length reset (whole-array delete).
- Control flow (`if`, loops, `return`), calls beyond `ExpandFunctionBody`,
  events, casts — see `taclet-ideas.md` Tiers 3–5.

## End-to-end examples (the `test*` functions)

`TestSuite.sol` holds 40 end-to-end `test*` functions driven by `PaperTestExamplesTest.java`;
each is called with postcondition `true`, the obligations being carried by in-body `assert`s.
The other 127 functions are the focused starters run by `TacletStarterExamplesTest`.

**Passing (most close automatically):** storage write/read, nested + deep copy,
aliases, mapping read/write/delete, struct-`delete` preserving mapping members
(`testStorageStructDeleteSkipsMappingMember`), mapping-element struct deep copy
(`testStorageMapStructCopy`); the full push/pop family
(`testStoragePush*`, `testStorageComplexReceiverPush*`, `testStorageArrayReadWrite`,
`testStorageEvaluationOrder` — RHS-before-LHS index order); memory aliasing,
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
- `storageIndex{Read,Write}{Array,Mapping}*_root` no longer require the `global` flag. They
  were the only members of the index family that did, which left a `simple`+`local` alias root
  matching neither the `_root` nor the `_decompose` rule.
- The index-write *save* rules now take `SimpleExpression[primitive]` for the value, as
  `storageRootWriteStore` already did. With an unrestricted `SimpleExpression` a storage-alias
  variable matched and `save(…, path)` was built ill-sorted.
- `storageIndexWrite{Array,Mapping}CopySource` are sort-generic (`find<[alphaSt]>` with
  `\hasSort`, the `storageRootDelete` pattern) instead of hard-coding `int` / `Struct`.
- `storagePushLengthSave` clears the appended slot as well as bumping `size`, mirroring
  `storagePopSave`. It writes the lazy delete marker rather than an eager `defaultValue`, so the
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

## Verifying a function directly from a `.sol` file (no `.key` file)

A single Solidity function can be verified without writing a `.key` problem file. The
proof obligation `\[{ body }\] true` is synthesized in memory
(`FunctionVerificationPO`): leading `require` statements act as preconditions (their
false branch reverts and closes trivially in box) and every `assert` is an obligation.
Function parameters and named return parameters are registered as free program
variables (symbolic inputs).

```bash
# CLI: --function is required, --contract optional when the name is unambiguous
./gradlew :keyext.solidity.core:solidityCli --args="--contract MyContract --function deposit /abs/path/MyContract.sol"
```

```java
// Tests: KeYEnvironment.loadFunction / SolidityExampleTests.loadAndProveFunction
KeYEnvironment env = KeYEnvironment.loadFunction(solPath, "MyContract", "deposit");
Proof proof = SolidityExampleTests.prove(env, 10000, KEEP_TIMEOUT);
```

Example: `keyext.solidity.examples/taclets/FunctionVerification.sol`, exercised by
`FunctionVerificationTest`. Scope: the body must stay within the implemented rule set
above; a value-carrying top-level `return` has no consuming rule yet (assign to named
return parameters instead).

## Verification

```bash
# Type-check the contract first — ~150ms, and it catches a Solidity error before a
# multi-minute test run:
solc --ast-compact-json keyext.solidity.examples/TestSuite.sol > /dev/null

# One example, or every function of the contract (absolute path — the CLI resolves relative
# paths against test-resources, not the repo root):
./run-key.sh keyext.solidity.examples/TestSuite.sol <function>
./run-key.sh keyext.solidity.examples/TestSuite.sol

# Focused taclet suites:
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.taclets.TacletStarterExamplesTest"
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.taclets.PaperTestExamplesTest"

# Check the `// generalized by:` annotations (docs/rule-generalizations.md):
./gradlew :keyext.solidity.core:testRuleGeneralization

# The .key/net/solc example suites (RulesTest, NetExamplesTest, SolcSemanticsExamplesTest)
# are a CI-only group:
./gradlew :keyext.solidity.core:testSolidityExamples

# Use --no-prove first when debugging a parser/matcher failure (load only).
```

Both example suites enumerate the contract, so a new function is picked up without editing the
test class.

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
