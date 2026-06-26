# Memory Implementation in KeY

This is the implementation companion to `docs/memory.md`. It describes how to
turn the memory calculus into KeY logic rules, Java matcher support, Solidity
program taclets, and runnable examples.

The short version: the logic-level memory model is partly present already, but
the source-level Solidity memory program rules are not. Implement memory by
reusing the storage taclet shape, while keeping the central memory invariant:
reference-typed memory values are `Identity` terms, not copied `Struct` values.

## Current KeY State

Existing files:

- `keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/memoryHeader.key`
  declares `Memory`, `Identity`, `IdentityPrim`, `mtMem`, and the global
  program variable `memory`.
- `keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/memoryRules.key`
  declares `write`, `addM`, `read`, `readR`, `idC`, `idCC`, `default`,
  `defaultValue`, `new`, and `isPrimitive`, with basic read/write/default and
  freshness rewrites.
- `keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/structMemoryRules.key`
  declares `copySt` and `copyMem`, with the lazy storage-to-memory and
  memory-to-storage read rules.
- `keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/solidityProgramRules.key`
  contains storage program taclets but no memory program taclet family yet.
- `ProgramSVSort` already exposes `MemoryPath`, `SimpleMemoryPath`,
  `ComplexMemoryPath`, and parameterized `Path[name=memory...]` sorts.

Name mapping from `docs/memory.md` to current KeY rules:

| Spec name | Current KeY name |
|---|---|
| `emptyMemory` / `mtMem` | `mtMem` |
| `add(mem, r)` | `addM(mem, r)` |
| `read`, `readR`, `write` | same names |
| `idC(r, path)` | same name |
| `copySt`, `copyMem` | same names |
| `emptyStruct` | `mtSt` |
| `select(storage, path)` | usually `find<[Struct]>(storage, path)` |
| `store(storage, path, value)` | usually `save(storage, path, value)` |

Not present yet:

- Source-level memory declaration, read, write, alias, delete, array, and
  storage-memory taclets.
- A source-level fresh-memory-root mechanism that can create an `IdentityPrim`
  known to satisfy `new(memory, r)`.
- `erase(mem)` and a standalone `delete(mem, id)` helper. These may be useful
  if the Lean/Maude rules use them directly, but most Solidity `delete`
  statements can be implemented by fresh rebinding or `write` updates.
- A memory-specific payload-sort bridge: a Solidity struct field in memory has
  Solidity type `Struct`, but the memory heap stores an `Identity`.

## Implementation Order

### 1. Make Memory References Identity-Sorted

Before adding taclets, ensure every reference-typed memory local and parameter
is represented as an `Identity`-sorted program variable while preserving its
Solidity type metadata.

Current parser behavior keeps `Person memory alice;` as a `Struct`-sorted
program variable with `DataLocation.Memory`. That is not compatible with
updates such as:

```key
{alice := idC(r, nil)}
{memory := write(memory, alice, PaperStore$Person$age, 20)}
```

Add a helper similar to `asStorageAliasType`, but for memory references:

- If `dataLocation == Memory` and the Solidity type is a reference type
  (`StructDeclaration`, array type, dynamic array type, and later bytes/string
  if supported), return `new KeYSolidityType(original.getSolidityType(),
  Identity)`.
- Do not rewrite primitive/value locals; they remain `int`, `bool`, etc.
- Apply this in both parser paths:
  - `SolidityToKeyConverter#visitVariableDeclarationWithInitialValue`
  - `SolidityToKeyConverter#visitParameter`
  - `SolJSONParser#parseDeclaration`
  - `SolJSONParser#parseParam`

Keep the original Solidity type inside `KeYSolidityType`. The matcher still
needs it for member resolution, `\newTypeOf`, and field/index type conditions.

### 2. Add Memory-Local Fresh Variable Support

The existing fresh-variable plumbing has a special case for storage aliases:
`Variable[name=storage]` creates a `DataLocation.Storage` variable and re-sorts
structs to `List`. Memory unfolding needs the analogous behavior.

Extend `ProgramVariableSVSort` with a memory-local filter:

- Accept declarations such as `\program Variable[name=memory] mp;`.
- Match only `ProgramVariable` instances with `DataLocation.Memory`.
- In `TacletApp#getProgramElement`, create fresh variables for this filter with
  `DataLocation.Memory`.
- If the fresh variable's Solidity type is a reference type, give it the
  `Identity` logic sort described above.

This is needed for unfold rules that synthesize aliases such as:

```key
s#aliasType memory s#mp = s#nmp;
s#mp.s#a = s#se;
```

Do not generate memory aliases with `Variable[name=value]`. A memory reference
alias is a path/root identity, not an ordinary stack value.

### 3. Add Memory Payload Sort Conditions

Storage rules can use `\hasFieldSort(a, \sort(alpha))` because storage stores
the actual `Struct` value for struct-typed fields. Memory cannot do that:
reference-typed memory fields store `Identity`.

Add memory-aware varconds, or extend the existing ones with memory-specific
variants:

- `\hasMemoryFieldSort(a, \sort(alpha))`
  - primitive field `uint` maps to `int`
  - `bool` maps to `bool`
  - reference field `Account`, `Token[]`, etc. maps to `Identity`
- `\hasMemoryElementSort(mp, \sort(alpha))`
  - primitive array element maps to its primitive sort
  - reference array element maps to `Identity`
- Optionally `\isMemoryReferenceField(a)` and
  `\isMemoryReferenceElement(mp)` if separate primitive/reference taclets are
  clearer than one generic rule.

Use these varconds in terminal memory read/write/delete rules instead of
storage's `\hasFieldSort` when the heap cell is a memory payload.

Also revisit `defaultDef` and `readFromCopyToStorage`. They currently branch on
`isPrimitive(fld)`. That is fragile for `at(i)`, because `at(i)` is the same
field constructor for primitive arrays and arrays of structs. Prefer
sort-specialized default/copy rules:

```key
default<[int]>(idC(r, path), fld)      -> defaultValue<[int]>
default<[bool]>(idC(r, path), fld)     -> defaultValue<[bool]>
default<[Identity]>(idC(r, path), fld) -> idC(r, consr(path, fld))
```

If `isPrimitive` is retained, generate rewrite facts for source field constants
and add a separate plan for indexed array fields, whose primitive/reference
status depends on the receiver element type.

### 4. Add a Sound Fresh Identity Mechanism

Memory allocation and reference `delete` need a fresh `IdentityPrim r`.

Do not use an unconstrained term variable for `r`; that would allow aliasing
with an existing memory root. The rule application must either prove or
generate:

```key
new(memory, r)
```

Practical options:

- Add a Solidity taclet varcond that creates a fresh `IdentityPrim` skolem and
  records the freshness obligation.
- Use a `\schemaVar \skolemTerm IdentityPrim r;` plus an explicit side
  condition/branch containing `new(memory, r)`.
- For initial examples only, start from `mtMem`, where `new(mtMem, r)` reduces
  to `true`; still implement the general mechanism before claiming source-level
  memory allocation is complete.

The update emitted by allocation should combine allocation and initialization
so the fresh root has default reads:

```key
{mp := idC(r, nil)}
{memory := addM(memory, r)}
```

For storage-to-memory copies and arrays, use the allocated memory as the input
to the helper:

```key
{mp := idC(r, nil)}
{memory := copySt(addM(memory, r), r, find<[Struct]>(storage, spPath))}
```

## Program Taclet Families

Add these rules to `solidityProgramRules.key` under
`\rules(programRules:Solidity)`. Follow the storage rules' three-step shape:
unfold RHS, unfold LHS, then emit the update.

### Declarations and Allocation

Reference-typed declaration without initializer:

```solidity
Person memory carol;
```

Effect:

```key
{carol := idC(r, nil)}
{memory := addM(memory, r)}
```

Rule requirements:

- Match a reference-typed `T memory mp;`.
- Create or bind fresh `r` with `new(memory, r)`.
- Emit `mp := idC(r, nil)` and `memory := addM(memory, r)`.

Dynamic memory array allocation:

```solidity
uint[] memory xs = new uint[](n);
```

Effect shape:

```key
{xs := idC(r, nil)}
{memory := write(addM(memory, r), idC(r, nil), size, n)}
```

Use `size` as the synthetic length field, matching storage array rules. Memory
arrays are fixed-length after allocation, so do not add `push` or `pop` rules.

Root memory alias declaration or assignment:

```solidity
Person memory david;
Person memory carol = david;
carol = david;
```

Effect:

```key
{carol := david}
```

This is not a copy. Both names observe the same heap cells after the update.

### RHS Unfolding

Capture nonsimple right-hand expressions before touching the LHS, matching
Solidity evaluation order.

Reference field read:

```solidity
Account memory acc = carol.account;
```

Terminal effect:

```key
{acc := read<[Identity]>(memory, carol, PaperStore$Person$account)}
```

Primitive field read:

```solidity
uint v = carol.age;
```

Terminal effect:

```key
{v := read<[int]>(memory, carol, PaperStore$Person$age)}
```

Deep reads unfold the receiver first:

```solidity
v = carol.account.balance;
```

Rewrite to:

```solidity
Account memory acc = carol.account;
v = acc.balance;
```

Then emit:

```key
{acc := read<[Identity]>(memory, carol, PaperStore$Person$account)}
{v := read<[int]>(memory, acc, PaperStore$Account$balance)}
```

Use `ComplexMemoryPath` or `Path[name=memory.complex]` for the first pattern,
and a fresh `Variable[name=memory]` for the alias.

### LHS Unfolding

Capture nonsimple memory receivers before writing:

```solidity
carol.account.balance = 10;
```

Rewrite to:

```solidity
Account memory acc = carol.account;
acc.balance = 10;
```

Then emit:

```key
{acc := read<[Identity]>(memory, carol, PaperStore$Person$account)}
{memory := write(memory, acc, PaperStore$Account$balance, 10)}
```

Use the same shape for index receivers:

```solidity
tokens[i].value = 9;
```

Capture `tokens[i]` into a memory alias before writing `.value`.

### Terminal Field Rules

Simple receiver field write:

```solidity
mp.a = se;
```

Effect:

```key
{memory := write(memory, mp, fieldA, se)}
```

where `mp` is `Path[name=memory.simple]` or `SimpleMemoryPath`, `a` is
`\program Field`, and `fieldA` is tied with `\sameAsTerm(a, fieldA)`.

Simple receiver field read:

```solidity
lhs = mp.a;
```

Effect:

```key
{lhs := read<[alpha]>(memory, mp, fieldA)}
```

Bind `alpha` with `\hasMemoryFieldSort(a, \sort(alpha))`. For primitive
fields this gives the primitive sort; for reference fields it gives `Identity`.

For field assignment between memory references:

```solidity
carol.account = david.account;
```

The RHS should be unfolded first:

```solidity
Account memory pv = david.account;
carol.account = pv;
```

The terminal update is still a single heap write:

```key
{memory := write(memory, carol, PaperStore$Person$account, pv)}
```

### Terminal Index Rules

Memory array index read/write uses `at(i)` as the heap field:

```solidity
xs[i] = se;
v = xs[i];
Token memory tok = tokens[i];
```

Effects:

```key
{memory := write(memory, xs, at(i), se)}
{v := read<[alpha]>(memory, xs, at(i))}
{tok := read<[Identity]>(memory, tokens, at(i))}
```

Add bounds branches for arrays:

```key
0 <= i & i < read<[int]>(memory, xs, size)
```

Memory has no mappings, so do not copy storage's mapping variants into the
memory rule family.

### Delete Rules

Keep memory delete separate from storage delete. Do not implement one generic
`delete path` rule.

Primitive memory field or primitive array element:

```solidity
delete carol.age;
delete xs[i];
```

Effect:

```key
{memory := write(memory, receiver, fieldOrAtIndex, defaultValue<[alpha]>)}
```

Reference-typed memory field or array element:

```solidity
delete carol.account;
delete tokens[i];
```

Effect shape:

```key
{memory := write(addM(memory, r), receiver, fieldOrAtIndex, idC(r, nil))}
```

This freshens the selected slot. Old aliases still point to the old identity.

Root memory delete:

```solidity
delete carol;
```

Effect:

```key
{carol := idC(r, nil)}
{memory := addM(memory, r)}
```

This rebinds only the root variable. Existing aliases keep observing the old
identity.

Complex delete targets unfold first:

```solidity
delete carol.account.balance;
```

Rewrite to:

```solidity
Account memory acc = carol.account;
delete acc.balance;
```

Then apply the terminal primitive/reference delete rule.

### Storage to Memory

Storage-to-memory declaration copies allocate a fresh memory identity and
install a lazy storage snapshot:

```solidity
Person memory carol = alice;
```

Effect shape in current KeY names:

```key
{carol := idC(r, nil)}
{memory := copySt(addM(memory, r), r, find<[Struct]>(storage, alicePath))}
```

where `alicePath` is the `List` produced by `\sameAsTerm(alice, alicePath)`.
Use `find<[Struct]>` for struct-valued storage paths, not an identity alias.

Reads through the copied memory root are already supported by
`structMemoryRules.key`:

```key
read(copySt(mem, r, st), idC(r, path), primitiveField)
  -> find(st, consr(path, primitiveField))

read(copySt(mem, r, st), idC(r, path), referenceField)
  -> idC(r, consr(path, referenceField))
```

If `isPrimitive` remains in that rule, make sure source fields and array
indices are classified correctly as described above.

### Memory to Storage

Memory-to-storage assignment stores a lazy struct view of the memory identity:

```solidity
alice = carol;
alice.account = carol.account;
```

Effect shapes:

```key
{storage := save(storage, alicePath, copyMem(mtSt, memory, carol))}

{storage := save(storage, accountPath,
    copyMem(mtSt, memory, read<[Identity]>(memory, carol, accountField)))}
```

`structMemoryRules.key` already rewrites primitive reads from `copyMem`:

```key
find(copyMem(st, memory, id), path)
  -> readR(memory, id, path)
```

Only use eager materialization if a later proof obligation genuinely needs a
concrete `Struct`; otherwise keep the lazy `copyMem` view.

## Logic Rule Follow-Ups

The existing data model is enough for simple aliasing examples, but full memory
support should add or adjust these logic rules:

1. Add `defaultValue` rules for every Solidity primitive sort that can appear
   in memory (`uint*`, `int*`, `address`, fixed bytes, etc.), or normalize those
   types to the existing `int`/`bool` sorts before rules fire.
2. Add sort-specialized defaults for `Identity` so fresh reference fields read
   as compound identities.
3. Decide whether `erase(mem)` is required. If Lean/Maude uses it for lazy
   identity-copy normalization, add `Memory erase(Memory)` plus read-over-erase
   rules. If source taclets always emit direct aliasing writes, leave it out.
4. Decide whether a standalone `delete(mem, id)` helper is required. Prefer
   source-level fresh rebinding/writes first, because they naturally preserve
   old aliases.
5. Add copy rules for reference-valued paths if `copyMem` or `copySt` proofs
   get stuck on non-primitive leaves.

## Examples and Tests

Start with small examples under `keyext.solidity.examples/taclets/`. Suggested
files:

- `memory-decl-fresh.key`: implemented. `Person memory carol;` allocates a
  fresh root with an explicit `new(memory, r)` branch.
- `memory-root-delete-fresh.key`: implemented. `delete carol;` rebinds the
  memory root to a fresh identity with the same freshness branch.
- `memory-decl-default.key`: `Person memory carol; result = carol.age;`
  proves default primitive read.
- `memory-root-alias.key`: `carol = david; carol.age = 41;` proves
  `david.age == 41`.
- `memory-field-alias.key`: `Account memory acc = carol.account; acc.balance =
  100;` proves `carol.account.balance == 100`.
- `memory-deep-field.key`: exercises receiver unfolding for
  `carol.account.balance`.
- `memory-field-reference-assign.key`: `carol.account = david.account;` proves
  later writes alias.
- `memory-delete.key`: separate primitive field, reference field, index, and
  complex receiver delete cases. Root delete is covered by
  `memory-root-delete-fresh.key`.
- `memory-array-index.key`: fixed-length memory array read/write with bounds.
- `storage-to-memory.key`: `Person memory carol = alice;` then mutate `alice`
  and prove the memory snapshot is unchanged.
- `memory-to-storage.key`: `alice = carol;` proves storage reads through
  `copyMem`.

Existing logic-level examples in
`keyext.solidity.core/src/test/resources/org/key_project/solidity/examples/memoryExample*.key`
are useful regression tests for the heap model, but they do not exercise
Solidity source statements inside modalities.

When examples should close automatically, add them to
`TacletStarterExamplesTest.examples()`.

Validation commands:

```bash
./gradlew :keyext.solidity.core:solidityCli --args="--no-replay -m 20000 keyext.solidity.examples/taclets/memory-field-alias.key"
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.taclets.TacletStarterExamplesTest"
./gradlew :keyext.solidity.core:test
```

Use `--no-prove` first when debugging parser or matcher failures:

```bash
./gradlew :keyext.solidity.core:solidityCli --args="--no-prove --no-replay keyext.solidity.examples/taclets/memory-field-alias.key"
```

## Pitfalls

- Do not lower memory references to `Struct` payloads. Memory aliases are
  `Identity` aliases.
- Do not deep-copy on root memory assignment. `carol = david;` is a direct
  identity rebind.
- Do not use storage delete rules for memory paths. Memory delete must preserve
  old aliases.
- Do not use full-path `\sameAsTerm(nmp, path)` for terminal memory heap
  updates. Terminal memory updates need `(receiver identity, field)` rather
  than a storage-style `List` path.
- Do not add memory `push`, `pop`, or mapping rules. Memory arrays have fixed
  length after allocation.
- Be careful with `at(i)`: whether it is primitive or reference-valued depends
  on the array element type, not on the `Field` constructor itself.
