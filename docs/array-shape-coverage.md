# Array shape and copy coverage

The examples use a breadth-first shape queue. Start with `uint`, then wrap each dequeued
shape with `[2]` and `[]`. Enumerate both vectors and all four matrices before extending
selected mixed matrices to depth three. Vector cases are already in `TestSuite.sol` and
`ArrayCopyShapes.sol`; the new matrix/cube contracts contain 76 functions.

Names list dimensions from outermost to innermost: `F` means fixed and `D` means dynamic.
Solidity writes its outermost dimension last.

| Contract | Solidity type | Functions |
|---|---|---:|
| `ArrayMatrixFF` | `uint[2][2]` | 9 |
| `ArrayMatrixDF` | `uint[2][]` | 12 |
| `ArrayMatrixFD` | `uint[][2]` | 10 |
| `ArrayMatrixDD` | `uint[][]` | 13 |
| `ArrayCubeFDF` | `uint[2][][2]` | 11 |
| `ArrayCubeDFD` | `uint[][2][]` | 9 |
| `ArrayCubeDFDMemory` | `uint[][2][]` | 4 |
| `ArrayMatrixConversions` | fixed/dynamic source and destination combinations | 8 |

The memory functions of `ArrayCubeDFD` are a separate contract so each file compiles without
the EVM contract-size warning. Three-dimensional cases initialize only the leaves needed by their observations,
such as the first and last leaves; other leaves keep their default values. This exercises depth and identity without
making the proof spend its budget on redundant element writes.

## Edges checked for each matrix shape

| Edge | Observation |
|---|---|
| Storage to a local storage reference | Writes through the alias affect the original |
| Storage array assignment | Source and destination modifications are independent |
| Storage to memory | Modifying either domain leaves the other unchanged |
| Memory to storage | Modifying either domain leaves the other unchanged |
| Memory assignment | Writes are shared by the two references |
| Assignment of one storage row to another | The rows become independent copies |
| Assignment of one memory row to another | The rows share identity |
| Memory rows sharing identity, then copied to storage | The destination rows occupy independent storage locations |
| Delete one row | Outer length remains; fixed child lengths remain and dynamic children empty |
| Delete the root | Dynamic length becomes zero; fixed lengths remain; rebuilt elements read zero |
| Pop followed by empty push | The reused element has its declared shape and default ordinary values |
| Push with an indexed reference source | The appended element is an independent copy |
| Bind the reference returned by push | Writes through it affect the appended element |

Push/pop edges are generated at every dynamic dimension. Fixed arrays and memory arrays have
no resize edge: Solidity rejects push/pop there. Copies from a dynamic source to a fixed
destination are also excluded where Solidity rejects the type conversion.

The conversion contracts separately cover fixed outer to dynamic outer, fixed inner to
dynamic inner, both dimensions becoming dynamic, shorter fixed outer arrays clearing a
dynamic row, and dynamic outer copies shrinking before a row is pushed again. Existing
`ArrayCopyShapes` also checks shorter fixed inner and outer dimensions together.

`ArrayMappingShapes` adds four boundary cases for the mapping delete terminal: deleting
a key resets its fixed-array value and leaves other keys alone, while popping or deleting
the containing mapping array preserves its mapping entries and fixed value lengths.

`TestSuite.testPushIndexedPrimitiveSourceOnce` and
`TestSuite.testPushIndexedFixedArraySourceOnce` check that an indexed source with `i++`
is evaluated once before the push changes the destination length. The four former fixed-array
reset open cases now have `TestSuite.testFixedShape*` regressions and closing functions in
`FixedLengths`.

## Semantic boundary

The implementation retains `save`, `delAt`, `delNode`, and `delNodeFixed`. It removes the
fixed-element reset guards and the inequality in the reference reset selector. The statement
rules still enforce Solidity's array bounds. Copy selectors retain the source/target length
comparisons because those identify the copied prefix and cleared destination tail.

The default-read regressions also cover arbitrary entry storage. The formerly open
`emptyPushFixedElementReadsZero` now closes in `FixedLengths`; `TestSuite` checks fixed-array,
dynamic-array, and struct defaults through the reference returned by push. Nonmapping push
resets its appended slot with `delAt`, preserving fixed lengths and nested mappings.

`StorageDanglingOpen` records four current-compiler behaviors after out-of-range
dangling-reference writes. Data can survive deletion of an empty array, copying, or an empty
push; the canonical rules establish default ordinary values. Solidity does not guarantee
the behavior of dangling references, so these witnesses remain open with their EVM assertions
unchanged.

## Verification

For each contract above:

```bash
./run-key.sh keyext.solidity.examples/contracts/CONTRACT.sol -m 20000 -t 60000 --open-goals
./run-key.sh keyext.solidity.examples/contracts/CONTRACT.sol --solc
```

The existing `ContractExamplesTest` enumerates every function of these files. The compiler
warning gate also enumerates the files. `TestSuite` regressions run through the normal proof
and EVM test suites, and `OpenExamplesStayOpenTest` guards the compiler-specific dangling-reference witnesses.
This is bounded coverage through all depth-two shapes and two representative depth-three
shapes, not exhaustive coverage of arbitrary nesting or operation sequences.
