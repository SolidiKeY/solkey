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

The split follows the `solidity-key-taclets` skill: root rules use
`SimpleStoragePath`; field/index paths use `Path[...]` schema variables placed
directly in the resulting terms (the engine lowers the matched AST to logic);
complex member/index receivers stay as structural `.key` source patterns and are
captured into fresh storage aliases by `_unfold_leftFst` / `_unfold_rightFst`
rules before a terminal rule fires.

## Implemented

### Storage read / write / copy
- Root: `storageRootWriteStore`, `storageRootReadSelect`,
  `storageRootWriteCopySource` (primitive) and `…_struct` (`find<[Struct]>`).
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

### Increment / decrement (`++`/`--`, pre/post, plain and `result = …`)
Direct storage updates (no program-level desugaring), e.g. `++age;` ⇝
`{storage := save(storage, path, find<[int]>(storage, path) + 1)}`. Full matrix
at root / field / index level: `storage{Root,Field,Index}{Pre,Post}{in,de}crement`
and their `…Assignment` twins, plus `…_unfold_leftFst` for complex receivers.
Local-value twins `localDecl…` / `localAssign…` cover captured temporaries. All
24 focused examples close. Operator matching checks the operator enum (not just
the AST node class) so `+=` does not match `=`.

### Compound assignment
`+=`, `-=`, `*=`, `/=`, `%=` at root / field / index, each with a terminal and a
`_unfold_leftFst` for complex receivers: `storage{Root,Field,Index}{Add,Sub,Mul,Div,Mod}Assign`.
`/=` and `%=` guard with `\if(se != 0)\then(…)\else(revert)`; no overflow branch
(matches `+`). Bitwise `&= |= ^= <<= >>=` parse but are deferred (no bitwise LDT).

### Tier-1 expression operators
Mirror the `+`/`==` families: terminal assigns the logic-level result, non-simple
operands are captured by `_unfold_left/right` (arith) or `…CaptureLhs/Rhs`
(relational/logical). The former `<op>_unfold_result` rules (`nlhs = se1 OP se2`)
are gone — a non-simple write target is now served by the per-statement RHS
captures (§Capture partition below), which also cover nested RHS like
`total = x + y*z;`. Uses plain LDT ops (`sub`, `mul`, `div`, `mod`, `pow`,
`neg`), so no overflow branch; `/` and `%` revert on a zero denominator.
- Arithmetic: `-`, `*`, `**` (`pow`), `/`, `%`.
- Relational: `!=`, `<`, `>`, `<=`, `>=` (predicate map `lt/leq/gt/geq`).
- Logical / unary: `&&`, `||`, `!`, unary `-x`. A non-simple left operand is
  captured eagerly (Solidity always evaluates it); a non-simple *right* operand
  goes through `logicalAnd/OrShortCircuitRhs` — a two-goal split on the simple
  left operand, each branch guarded requireSimple-style by a `se = TRUE/FALSE`
  disjunct, so the right operand is only evaluated on the branch that reaches it
  (examples `logicalAndShortCircuitRhs` / `logicalOrShortCircuitRhs`).
- Deferred: bitwise (`& | ^ << >> ~`), unary `+` (removed in Solidity ≥0.5),
  checked-arith overflow.

### Storage aliases
`storageLocalDeclInitDrop` (decl-with-init decomposition) followed by
`storageLocalRootRebind` (standalone `lp = sp;`). The alias binds to the **path**,
not the value: `{lp := cons1(rhsField)}` or `{lp := pathFields}`. Two enablers:
`SolidityToKeyConverter#asStorageAliasType` re-sorts any storage-held reference
collection (`Struct`, array, mapping) to `List`; value-reads and path-rebinds
stay disjoint through the read side, not the target variable — the value-read
rules require a primitive source (`Path[storage,simple,primitive]` root,
`Field[primitive]` member, or `Path[...,primitiveElement]` indexed receiver),
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
`storagePopSave`'s nonempty branch clears the popped slot with the
mapping-preserving `delValue<[alpha]>(find<[alpha]>(storage, sp · at(ℓ-1)))`
marker (not eager `defaultValue`), reusing the `delNode` machinery of §Delete —
so a mapping nested in the popped element survives `pop()` (and a later bare
`push()`, which only bumps `length`), exactly like `storageRootDelete` /
`storageFieldDelete`. See the `testDeepPopDoesNotResetMappingMember` end-to-end
example.

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
`storageRootDelete` and `storageFieldDelete` save
`delValue<[alpha]>(find<[alpha]>(storage, path))`, binding `alpha` via `\hasSort` /
`\hasFieldSort`. (`storageIndexDelete` keeps the eager `defaultValue<[alpha]>`:
deleting a single collection entry/element resets it outright — the
`storage-index-delete-mapping-struct` starter asserts the whole entry `= mtSt`.)
`delValue` picks the reset value by sort: any
non-struct sort collapses to `defaultValue<[alpha]>` (`int→0`, `bool→FALSE`), while
a struct becomes a lazy `delNode` marker (structRules.key). This gives Solidity's
`delete` semantics on structs: value/reference members reset, but **mapping members
are preserved**. On read, `selectSt` on a `delNode` reads a mapping member
(`MapField`) through to the original struct, recurses into a reference member
(`IdField`, so nested structs' mappings also survive), and resets a primitive leaf.
Mapping vs. reference members are told apart by `Field` sub-sorts (`MapField` /
`IdField`) stamped on the field constants at parse time (`SolJSONParser`, keyed off
the member's Solidity type). The struct case is disambiguated from the default case
purely by the sorts of the `delValue` rewrite rules (no varcond): `delValueStruct`
matches the concrete `Struct` sort and, being in `simplify`, outranks the
`StValue`-generic `delValueDefault` (`simplify_enlarging`); the same pattern pairs
`selectStDelNodeMap`/`Ref` with `selectStDelNodeDefault`. Complex receivers unfold first
(`…_unfold_leftFst`). Storage and memory deletes stay separate by design.

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
  is restricted to a `Field[reference]` member so primitive members take this
  route instead.
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
`msgSender`, `msgValue`, and `self`, plus the uninterpreted contract-invariant
predicate `CInv(Struct, Struct)` over `(storage, net)` (solidiKeY-style: each
problem file gives it meaning via its own `insertCInv` rewrite taclet).
`msg.sender` / `msg.value` desugar to
`msgSender` / `msgValue` in `SolidityToKeyConverter.visitMemberAccess`
(shadowable by a local named `msg`). `transfer` and `send` are registered
builtins classified like `push`/`pop` (`MemberExp` + `FunctionCallExpression`,
no dedicated AST node). Transfer semantics is the taclet choice
`transferSemantics:{noCallback, withCallback}` (`optionsDeclarations.key`,
default `noCallback`; examples pin the other variant with
`\withOptions transferSemantics:withCallback;`). Under `noCallback`:
`transferNoCallback`
(`a.transfer(v);` ⇝ `{net := storeSt(net, at(a), selectSt<[int]>(net, at(a)) − v)}`).
Under `withCallback`: `transferWithCallback` splits into "invariant on exit"
(book `net(a) −= v`, prove `CInv(storage, net)`, drop the continuation) and
"resume after callback" (havoc `storage` and `net` with `\skolemTerm Struct`
constants, assume `CInv`, continue). Both share the
`transfer_unfold_leftFstReceiver` / `transfer_unfold_rightSndArgument`
captures. Not yet done: the `\getContractInvariant` varcond +
`ContractSpecification` plumbing (`docs/net.md` Step 4 phase 2),
`send`/`call{value:}` rules, and proof-obligation
plumbing (`docs/net.md` Step 5). **No example covers any of this any more**: the seven `net-*`
starters were `.key`-only (problem-local `\rules` for `CInv`, `\withOptions`, and inline
`msg.*` / `.transfer` that `SolJSONParser` does not parse) and were deleted with the rest of
the `.key` examples.

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
`SolidityToKeyConverter` (the ANTLR path used for programs written inline in a modality). The
two are not equally complete: `msg.sender`, `msg.value`, `.transfer` and `.send` work in the
ANTLR path but not in the JSON one. That is a **parser** gap, not a taclet gap, and it is why
the seven `net-*` examples still inline their programs.
