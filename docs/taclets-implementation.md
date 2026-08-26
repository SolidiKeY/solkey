# Taclet Implementation Status

What is runnable today vs. pending. Rules live in
`keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/solidityProgramRules.key`
(loaded via `standardSolidityRules.key`). Runnable examples live in
`keyext.solidity.examples/taclets/` (focused, one rule each) and
`keyext.solidity.examples/mainFeatures/` (end-to-end, from `PaperTest.sol`).

- Authoring syntax → `key-taclets.md`
- Calculus spec → `storage.md`, `memory.md`
- Open backlog (next constructs to implement) → `taclet-ideas.md`

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
- Local-storage declarations: `storageLocalDeclSkip`, `storageLocalDeclInitSplit`.

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
operands are captured by `_unfold_left/right/result` (arith) or `…CaptureLhs/Rhs`
(relational/logical). Uses plain LDT ops (`sub`, `mul`, `div`, `mod`, `pow`,
`neg`), so no overflow branch; `/` and `%` revert on a zero denominator.
- Arithmetic: `-`, `*`, `**` (`pow`), `/`, `%`.
- Relational: `!=`, `<`, `>`, `<=`, `>=` (predicate map `lt/leq/gt/geq`).
- Logical / unary: `&&`, `||` (left-capture only), `!`, unary `-x`.
- Deferred: bitwise (`& | ^ << >> ~`), unary `+` (removed in Solidity ≥0.5),
  `&&`/`||` right short-circuit (needs the Tier-3 `if`), checked-arith overflow.

### Storage aliases
`storageLocalDeclInitSplit_{rootRebind,globalField,localField}` and
`storageLocalRootRebind` (standalone `lp = sp;`). The alias binds to the **path**,
not the value: `{lp := cons1(rhsField)}` or `{lp := pathFields}`. Two enablers:
`SolidityToKeyConverter#asStorageAliasType` re-sorts any storage-held reference
collection (`Struct`, array, mapping) to `List`; `\program Variable[storage|value]`
filters by `DataLocation` to keep value-reads and path-rebinds disjoint.

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
`storageFieldDelete`. See the `testDeepPopDoesNotResetMappingMember` mainFeatures
example.

### Memory
Source-level memory family covers heap field/index read & write, root aliasing,
fresh allocation (`memoryReferenceDeclFreshAlloc`, with a `new(memory, r)` skolem
branch), fixed-length array allocation, primitive-default vs. reference-slot
delete (`memoryRootDeleteFreshRebind` and field/index delete), and lazy
storage↔memory copies via `copySt` / `copyMem`. Memory references are
`Identity`-sorted, not copied `Struct` values; no `push`/`pop`/mapping.
Complex memory receivers are captured first by `memoryIndexRead_unfold_rightFst`
/ `memoryIndexWrite_unfold_leftFst` / `memoryIndexDelete_unfold_leftFst`. These
take a plain `Path[memory,complex]` (no `array` flag): a receiver capture uses no
array structure, and in memory an indexable path is an array anyway — mappings
cannot be memory-located and `bytes`/`string` are not memory reference types. The
`array` flag stays on the rules that do consume it (`memoryIndexWriteArray`,
`memoryIndexReadArray*`, `memoryIndex*Delete`), which emit the `size` bound.
`memory-struct-array-index.key` covers the complex-receiver path.

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
purely by the concrete-vs-generic sort of the `delValue` rewrite rules (no varcond),
mirroring `selectIntOnStore` / `selectOnStore`. Complex receivers unfold first
(`…_unfold_leftFst`). Storage and memory deletes stay separate by design.

### Payments (`net` ledger, `msg`, `transfer`)
First slice of `docs/net.md` (Steps 1–3). `netHeader.key` declares the
program variables `Struct net` (per-address ledger: read
`selectSt<[int]>(net, at(a))`, empty ledger `mtSt` ⇒ `net(a) = 0`),
`msgSender`, `msgValue`, and `self`. `msg.sender` / `msg.value` desugar to
`msgSender` / `msgValue` in `SolidityToKeyConverter.visitMemberAccess`
(shadowable by a local named `msg`). `transfer` and `send` are registered
builtins classified like `push`/`pop` (`MemberExp` + `FunctionCallExpression`,
no dedicated AST node). Rules (no-callback semantics only): `transferNoCallback`
(`a.transfer(v);` ⇝ `{net := storeSt(net, at(a), selectSt<[int]>(net, at(a)) − v)}`),
plus `transfer_unfold_leftFstReceiver` / `transfer_unfold_rightSndArgument`
captures. Not yet done: the with-callback rule (contract-invariant varcond +
havoc, `docs/net.md` Step 4), `send`/`call{value:}` rules, and proof-obligation
plumbing (`docs/net.md` Step 5). Five `net-*` starter examples close.

### Paths and lowering
Path SV sorts: `StoragePath`, `SimpleStoragePath`, `ComplexStoragePath`,
`MemoryPath`, `SimpleMemoryPath`, `ComplexMemoryPath`, plus precise
`Path[...]` with comma-separated flags (`storage`/`memory`, `simple`/`complex`,
`root`/`field`/`index`, `array`/`mapping`, `primitive`/`reference`,
`local`/`global`). Roots are simple; member/indexed paths and no-arg `arr.push()`
are complex. Path schema variables used directly in `\replacewith`/`\add` term
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

## mainFeatures examples (PaperTest.sol)

`keyext.solidity.examples/mainFeatures/` holds 38 end-to-end problems driven by
`PaperTestExamplesTest.java`; each inlines a function body in the modality over
`\programSource "PaperTest.sol"`.

**Passing (most close automatically):** storage write/read, nested + deep copy,
aliases, mapping read/write/delete, struct-`delete` preserving mapping members
(`testStorageStructDeleteSkipsMappingMember`), mapping-element struct deep copy
(`testStorageMapStructCopy`); the full push/pop family
(`testStoragePush*`, `testStorageComplexReceiverPush*`, `testStorageArrayReadWrite`,
`testStorageEvaluationOrder` — RHS-before-LHS index order); memory aliasing,
delete, and inc/dec in array indices (`testMemoryUintArray*`); cross-location
storage↔memory copies. Push examples assume a fresh-slot precondition
(`find<[int]>(storage, …·size) = 0`) since `mainFeatures` starts from
unconstrained symbolic storage.

**Disabled — missing taclet support:**
- `testStorageArrayPushPop` — `tokens.push(Token(42));`, struct-literal `Token(42)`
  not parsed (no push-struct-value taclet).

## Verification

```bash
# One focused example (absolute path — the CLI resolves relative paths against
# test-resources, not the repo root):
./gradlew :keyext.solidity.core:solidityCli --args="-m 10000 /abs/path/keyext.solidity.examples/taclets/<file>.key"

# Focused taclet suites:
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.taclets.TacletStarterExamplesTest"
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.taclets.PaperTestExamplesTest"

# Use --no-prove first when debugging a parser/matcher failure (load only).
```
