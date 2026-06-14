# Taclet Implementation Plan

Analysis of what's needed to implement the paper's taclets (from `Pre-licenciate-paper/main.tex`) in KeY.

## Current State vs Paper

The paper defines **~80 program rules** (taclets) organized into 4 sections:
- **Storage Rules** (~45 rules)
- **Memory Rules** (~25 rules)
- **Storage-to-Memory Rules** (3 rules)
- **Memory-to-Storage Rules** (6 rules)

### What's already implemented

- The **data model** (logic-level) rules in `structRules.key`, `memoryRules.key`, and `structMemoryRules.key` — these define `save`, `find`, `selectSt`, `storeSt`, `read`, `write`, `addM`, `copySt`, `copyMem` and their rewrite rules. These are the functions that the paper's program rules *generate* in their `\replacewith` clauses.
- Only **one** program rule: `emptyModality` (removes an empty modality).
- The `schemaVarExample.key` proves that `\schemaVar \program Variable pv` works for matching a program variable inside a modality.

### What's NOT implemented

Everything needed to go from Solidity statements to logic updates.

---

## Blocker 1: Missing Program Schema Variable Sorts

In `ProgramSVSort.java`, only `Variable` is implemented. The paper's rules need:

| Schema Variable Sort | Paper Usage | Status |
|---|---|---|
| `Variable` | `v`, `pv`, `lhs` (stack variables) | **Implemented** |
| `Expression` | `e` (arbitrary expression) | `null` — not implemented |
| `SimpleExpression` | `se`, `i` (variable or constant) | `null` — not implemented |
| `NonSimpleExpression` | `nse` (complex expression) | `null` — not implemented |
| `Statement` | for prefix/suffix matching | `null` — not implemented |
| `Type` | `T` in declarations | `null` — not implemented |
| `StoragePath` | `path` (any storage path) | **Does not exist** |
| `SimpleStoragePath` | `sp` (simple storage path) | **Does not exist** |
| `NonSimpleStoragePath` | `nsp` (nonsimple storage path) | **Does not exist** |
| `LocalPath` | `lp` (local storage reference) | **Does not exist** |
| `GlobalPath` | `gp` (global storage root) | **Does not exist** |
| `MemoryPath` | `mp` (any memory path) | **Does not exist** |
| `NonSimpleMemoryPath` | `nmp` (nonsimple memory path) | **Does not exist** |
| `Field` | `a`, `b` (field name) | **Does not exist** as program SV |

Each needs a concrete `ProgramSVSort` subclass with a `canStandFor()` method that checks if a program element matches the category.

## Blocker 2: Solidity Parser Doesn't Support Key Constructs in Modalities

Testing revealed:
- **Member access** (`st.age = v;`) — fails with "Variable st out of the scope" even though `st` is a declared program variable of type `Struct`. The modality parser can't resolve field access on KeY logical types.
- **`revert;`** — fails with `NullPointerException` during parsing. The Solidity parser recognizes the `revert` keyword in the grammar but the converter crashes.
- **Schema variable member access** (`s#sp.age`) — the grammar supports `expression.identifier` syntax but the converter crashes.

## Blocker 3: Taclet Side Conditions

Many paper rules have side conditions:
- `isMapping(sp)` / `isArray(sp)` — to distinguish array vs mapping index rules
- `isComplex(path)` / `isSimple(path)` — to distinguish simple vs nonsimple paths
- `new(mem, r)` — freshness condition for memory allocation
- `kindof(pv)` — to determine the type/data-location of a program variable

These need `\varcond` implementations or custom taclet condition classes.

## Blocker 4: Program Context Matching (pi/omega)

The paper writes `{pi} stmt {omega}` to match a statement as the first active statement with arbitrary prefix/suffix. In KeY taclets, this is typically:
```
\find(\modality{#mod}{ ... #s; ... }\endmodality(post))
```
This requires `\program [list] Statement` support for prefix/suffix, which is infrastructure that exists in the parser but hasn't been tested for Solidity.

---

## Recommended Implementation Order

### Phase 1: Foundation (enables simplest rules)
1. **Fix `revert` statement parsing** in the Solidity converter
2. **Fix member access** in modality code (make `st.age = v;` work)
3. **Implement `Expression` SV sort** in `ProgramSVSort`
4. **Implement `SimpleExpression` SV sort**
5. **Implement `Statement` SV sort** (for prefix/suffix matching)
6. Implement `revertDiamond` and `revertBox` taclets (simplest program rules)

### Phase 2: Storage Field Rules (enables basic storage symbolic execution)
7. **Implement storage path schema variable sorts** (`StoragePath`, `SimpleStoragePath`, `NonSimpleStoragePath`, `GlobalPath`, `LocalPath`)
8. Implement `storageLocalDeclSkip` and `storageLocalDeclInitSplit`
9. Implement `storageFieldWriteSave`, `storageFieldReadFind` (simplest field rules)
10. Implement `storageLocalRootRebind`
11. Implement the remaining Step 1/2 unfolding rules
12. Implement remaining Step 3 field rules (`storageFieldWriteCopySource`, `storageRootWriteStore`, etc.)

### Phase 3: Storage Array/Mapping Rules
13. Implement `isMapping`/`isArray` side conditions
14. Implement mapping index rules (no bounds check)
15. Implement array index rules (with bounds branching)
16. Implement push/pop rules

### Phase 4: Memory Rules
17. Implement `MemoryPath` / `NonSimpleMemoryPath` SV sorts
18. Implement memory Step 1-3 rules (follow same pattern as storage)
19. Implement `memoryDeclFreshAlloc` with `new(mem, r)` freshness condition

### Phase 5: Cross-Domain Rules
20. Implement storage-to-memory rules (3 rules)
21. Implement memory-to-storage rules (6 rules)

---

## Key Files

- Schema variable sorts: `keyext.solidity.core/src/main/java/org/key_project/solidity/rule/sv/sort/`
- Schema variable classes: `keyext.solidity.core/src/main/java/org/key_project/solidity/rule/sv/`
- Taclet parser: `keyext.solidity.core/src/main/java/org/key_project/solidity/parser/builder/TacletPBuilder.java`
- Solidity converter: `keyext.solidity.core/src/main/java/org/key_project/solidity/parser/SolidityToKeyConverter.java`
- Schema reader: `keyext.solidity.core/src/main/java/org/key_project/solidity/program/SoliditySchemaReader.java`
- Solidity grammar: `keyext.solidity.core/src/main/antlr/Solidity.g4`
- Schema lexer: `keyext.solidity.core/src/main/antlr/SchemaLexer.g4`
- Program rules: `keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/solidityProgramRules.key`
- Data model rules: `structRules.key`, `memoryRules.key`, `structMemoryRules.key` (same directory)
- Test examples: `keyext.solidity.core/src/test/resources/org/key_project/solidity/examples/`

## Notes

- The critical bottleneck is **Phase 1** — without the program schema variable sorts and parser fixes, none of the paper's taclets can be expressed.
- The data model rules (save/find/read/write) are already solid, so once the program-level infrastructure is in place, the taclet definitions themselves should be relatively straightforward translations from the paper.
- Existing tests (`storageExample*.key`, `memoryExample*.key`) only test the data model at the logic level — they manually construct update sequences. There are no tests that exercise program rules with Solidity statements inside modalities.
