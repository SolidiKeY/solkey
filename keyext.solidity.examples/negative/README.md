# Negative examples

## `unprovable/`

`unprovable/` holds valid solc ≥ 0.8 examples whose EVM behavior the calculus does not model,
so their obligations cannot close — currently `Unprovable.sol`, whose functions revert on the
EVM's checked arithmetic (Panic 0x11) before their `assert(false)`, while SolKey's unbounded
mathematical integers give the overflowing operation a non-reverting path. No test suite scans
the directory; moving an example here is how it is retired from the suites while staying
compilable.

## `no-clean-tail/`

`no-clean-tail/` holds claims that are **true on the EVM and must stay unproved**. Each reads a
default value from a slot an empty `push()` exposed: zero on a chain where solc cleared
everything beyond the array's length, but symbolic execution starts from an arbitrary storage
and assumes no such clean tail (`docs/limitations.md`, "Storage starts arbitrary"), since
assembly or a dangling storage reference can leave data there. An example that needs the zero
states it itself with `delete arr.push();`.

`NoCleanTailExamplesStayOpenTest` (common `test` task) enumerates the directory and fails when
a function closes, so an example joins by being written and a rule that starts assuming a clean
tail turns the test red. `SolidityRuntimeExecutionTest` runs every function on the EVM, which
keeps the other half of the claim: none hits a failing `assert`. Each file is named after the
file its functions came from, `<origin>Tail.sol`:

| File | From | Functions |
|---|---|---|
| `StorageArraysTail.sol` | `taclets/StorageArrays.sol` | `testEmptyPushReadsDefaultValue`, `testFixedCopyThenEmptyPushReadsZero`, `testFixedShapeEmptyPushFixedElementReadsZero`, `testEmptyPushFixedArrayReturnReadsZero`, `testEmptyPushDynamicArrayReturnReadsZero`, `testEmptyPushStructReturnReadsZero` |
| `SolcArithmeticTail.sol` | `semantics/SolcArithmetic.sol` | `unaryAddOnPush` |
| `SolcArraysTail.sol` | `semantics/SolcArrays.sol` | `pushedSlotIsZeroed` |
| `SolcLoopsTail.sol` | `semantics/SolcLoops.sol` | `boundaryTestPushedSlotsAreZero`, `pushChainedAssign` |
| `SolcStructsMappingsTail.sol` | `semantics/SolcStructsMappings.sol` | `copyStructArrayIntoNestedArray`, `pushedStructIsZero` |
| `SolcArrayMembersTail.sol` | `semantics/SolcArrayMembers.sol`, `semantics/open/SolcArrayMembers.sol` | `deleteArrayIndex2d`, `pushLhsRhs2d`, `pushLhsRhs2dAnyState` (from `open/`), `copyNestedStorageToMemory`, `length2dStorageToStorage`, `pushLhs2dLast`, `pushLhs3dLast`, `pushLhsAndRhs1d`, `pushLhsCompound`, `pushNested`, `pushNoArgs1d`, `pushNoArgs1dLvalue`, `pushZero2d`, `pushZeroReadsZero` |

The solc ports keep their `/// solc:` line, which names the upstream test.

## `illegal/`

`illegal/` holds one **minimal contract per construct** Solidity itself rejects, so each file
really does fail to compile — mostly mapping shapes: a mapping lives only in storage, has no
memory or calldata representation, cannot be copied, assigned or deleted as a whole, and its key
must be elementary. The record says which rules the calculus never has to have; its legal
counterparts are the "Mapping indices" section of `taclets/Storage.sol`.
`IllegalExamplesCompileTest` enumerates the directory and asserts solc rejects every contract with
the diagnostic the file names on its single

```solidity
/// solc: <substring of the diagnostic>
```

line, so a new case joins the suite by being written, and a solc release that started accepting one
of them turns the test red instead of going unnoticed. The directory also holds the one non-mapping
case, `NestedArrayCalldataToStorage.sol`.

The test compiles through `SolcWrapper.getBinJson` (bytecode), not `getJsonSolidity` (AST only),
because the two stages reject different things. Every mapping case is an analysis-stage
`TypeError`/`ParserError` and shows up either way, but `NestedArrayCalldataToStorage` is rejected
during **code generation** — so it passes an AST-only request, and therefore passes
`./run-key.sh … --no-prove`, while still being uncompilable. Checking a `.sol` file with
`--no-prove` proves it parses and type-checks, not that solc can compile it.
