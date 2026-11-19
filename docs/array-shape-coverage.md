# Array shape and copy coverage

Nested array shapes are named from the outermost dimension to the innermost: `F` means fixed
and `D` means dynamic, so `DF` is `uint[2][]` (Solidity writes its outermost dimension last).
The four matrices are `FF`, `DF`, `FD` and `DD`. The cubes `FDF` (`uint[2][][2]`) and `DFD`
(`uint[][2][]`) between them contain both mixed transitions, fixed over dynamic and dynamic
over fixed, at both the outer and the inner level.

`taclets/StorageArrays.sol` and `taclets/Memory.sol` cover the vectors and most matrix edges.
`taclets/NestedArrays.sol` adds only the shape and edge pairs they leave out: nested arrays
crossing between memory and storage, cubes, and copies that change a dimension from fixed to
dynamic. An edge whose rule ignores the shape, such as taking a storage reference, has a single
example.

## Edges and the examples that cover them

| Edge | Observation | Examples |
|---|---|---|
| Local storage reference | Writes through the alias reach the original | `Storage.testStorageAliases`, `StorageArrays.testFixedShapeFixedElementStorageAlias`, `StorageArrays.testShapeCopyNestedSelfAliasPreservesValues` |
| Storage assignment | Target takes the source's values, then the two are independent | `StorageArrays.fixedRowsCopy*` (DF), `listTripleCopyResizesRows` (FD), `storageMatrixCopy*` (DD), `NestedArrays.nestedFixedCopyClearsInnerAndOuterTails` (FF), `NestedArrays.cubeStorageCopyIsDeep` (FDF) |
| Storage to memory | Modifying either side leaves the other unchanged | `Memory.storageMatrixCopyToMemory` (DD); `NestedArrays.fixedMatrixStorageToMemoryIsDeep`, `cubeStorageToMemoryIsDeep`, `dynamicCubeStorageToMemoryIsDeep` |
| Memory to storage | Modifying either side leaves the other unchanged | `NestedArrays.fixedMatrixMemoryToStorageIsDeep`, `dynamicMatrixMemoryToStorageIsDeep`, `cubeMemoryToStorageIsDeep`, `dynamicCubeMemoryToStorageIsDeep` |
| Memory reference assignment | Both names share writes | `Memory.memoryRootAlias`, `Memory.testMemoryAliasing` |
| One storage row assigned to another | The rows become independent copies | `NestedArrays.cubeRowCopyIsDeep`, `StorageArrays.testPushIndexedFixedArraySourceOnce` |
| Memory rows sharing identity, copied to storage | The storage rows are independent | `NestedArrays.fixedMatrixSharedMemoryRowsCopyApart`, `dynamicMatrixSharedMemoryRowsCopyApart`, `dynamicCubeSharedMemoryRowsCopyApart` |
| Delete one element | Lengths of the parent and of fixed children remain | `StorageArrays.testFixedShapeDeleteFixedElementKeepsLength`, `structArrayElementDeleteReadsZero` |
| Delete the root | A dynamic length becomes zero, fixed lengths remain, old valid elements are reset | `StorageArrays.testFixedShapeDeleteDynamicThenPushFixedElement`, `basketArrayDeleteThenPushReadsZero`, `NestedArrays.dynamicCubeDeleteResetsEveryLevel` |
| Pop, then empty push | The removed element reads as its default | `StorageArrays.fixedRowsPopPushFreshRow`, `listTriplePopPushFreshElement`, `mappingMatrixPopPushFreshRow`, `storageMatrixPopPushDanglingRowReappears` |
| Push of a storage element | The appended element is an independent copy | `StorageArrays.testPushIndexedFixedArraySourceOnce`, `NestedArrays.dynamicCubePushCopiesNestedElement` |
| Reference returned by push | Writes through it reach the appended element | `StorageArrays.testStoragePushReturnAlias`, `testStorageNestedPushReturnAlias` |
| Fixed to dynamic dimension | The target takes the source lengths | `StorageArrays.storageStaticToDynamicCopyKeepsLength`; `NestedArrays.fixedMatrixIntoDynamicMatrix`, `fixedOuterIntoDynamicOuter`, `fixedMemoryMatrixIntoDynamicRows` |
| Shorter source | Rows past the source length are cleared | `StorageArrays.fixedRowsCopyShorterIntoLonger`, `storageMatrixCopyShorterIntoLonger`, `NestedArrays.shorterFixedOuterEmptiesDynamicRow` |
| Mapping values in arrays | Deleting a key resets only its value; pop or delete of the array keeps the entries | `StorageArrays.testDeleteEntryOfMappingInArrayKeepsOtherKeys`, `testDeleteArrayOfMappingsOfFixedArraysKeepsEntries`, `testPushMappingOfFixedArrayPreservesEntries`, `storageDeletedFixedByKeyCopy` |

Push and pop exist only at a dynamic storage dimension. Solidity rejects a copy from a dynamic
source to a fixed destination. A default read from a slot that only an empty `push()` exposed
belongs in `negative/no-clean-tail/`, not here.

## Semantic boundary

The implementation retains `save`, `delAt`, `delNode`, and `delNodeFixed`. Statement
rules enforce array bounds. Array reset selectors clear the old valid range and preserve
out-of-range data. Copy selectors distinguish the copied source prefix, the cleared old
destination suffix, and the untouched tail beyond both lengths.

Empty push changes only the array length. Appended primitive, struct, and array values retain
their previous symbolic contents; returned references name those existing locations. No
clean-tail invariant is assumed. Assertions requiring an unknown appended value to be zero
or an unknown nested array to be empty stay open.

`StorageDangling` records four compiler-specific behaviors after writes through popped
aliases. Data survives deletion of an empty array, copying, and empty push under these rules.
The four assertions close and pass on the bundled EVM runner, although Solidity leaves
dangling-reference executions undefined.

The coverage is bounded: every depth-two shape and two depth-three shapes, not arbitrary
nesting or operation sequences.
