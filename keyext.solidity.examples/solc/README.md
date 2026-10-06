# solc test ports

Ports of the Solidity compiler's own tests ([`ethereum/solidity`](https://github.com/ethereum/solidity),
`test/libsolidity/semanticTests/` and `test/libsolidity/smtCheckerTests/`) into SolKey's
proof-obligation style. Upstream a semantic test is a small contract with expected return values,
and an SMTChecker test a contract whose `assert`s the checker proves or refutes; both were written
by the language implementers to pin down a corner case. Here each becomes a function whose
specification lives in the body as `assert`.

The point is a cross-check against a description of Solidity semantics that SolKey did not
write. `TestSuite.sol` exercises one taclet each; these exercise what the language says. What the
ports could not prove, and why, is summarised in `docs/limitations.md`.

Seventeen contracts, one per upstream theme. Every function of a file here closes and runs on the
EVM without a failing `assert`:

| File | Upstream | Functions |
|---|---|---|
| `SolcExpressions.sol` | `expressions/`, `exponentiation/` | 20 |
| `SolcStructs.sol` | `structs/` | 11 |
| `SolcArrays.sol` | `array/`, `array/push`, `array/pop`, `array/delete` | 12 |
| `SolcMemory.sol` | `structs/memory_*`, `array/copying`, `array/delete` | 11 |
| `SolcMappings.sol` | `structs/*mapping*`, `array/copying/array_elements_to_mapping.sol` | 9 |
| `SolcControlFlow.sol` | `statements/`, `various/`, `expressions/conditional_expression_storage_memory_*` | 10 |
| `SolcLoops.sol` | `statements/`, `viaYul/loops/`, loop-heavy `array/`; smt `loops/` | 49 |
| `SolcArrayMembers.sol` | `array/` push, pop, delete, copying, allocation; `storage/`; smt `array_members/` | 61 |
| `SolcFunctionCalls.sol` | `functionCall/`, `freeFunctions/`; smt `functions/` | 29 |
| `SolcStructsMappings.sol` | remaining `structs/`, mapping tests of `functionCall/`, `variables/`, `types/`, `storage/`, `viaYul/storage/`; smt `types/struct*`, `mapping*` | 50 |
| `SolcConstructors.sol` | `constructor/`, `immutable/`, `constants/`, `scoping/`; smt constructor, `inheritance/constructor_*`, `file_level/` | 35 |
| `SolcPayments.sol` | `payable/`, `receive/`, `fallback/`, `reverts/`, `errors/`, `tryCatch/`; smt `special/`, `blockchain_state/`, `try_catch/` | 35 |
| `SolcModifiers.sol` | `modifiers/`; smt `modifiers/` | 34 |
| `SolcSmtControlFlow.sol` | smt `control_flow/`, `bmc_coverage/`, `verification_target/`, `invariants/`, `complex/slither/` | 85 |
| `SolcArithmetic.sol` | `arithmetics/`, `exponentiation/`, `integer/`, `operators/`; smt `operators/`, `overflow/` | 46 |
| `SolcHigherLevel.sol` | `inheritance/`, `virtualFunctions/`, `getters/`; smt `inheritance/` | 19 |
| `SolcTypes.sol` | `enums/`, `literals/`, `types/`; smt `types/`, `typecast/` | 39 |

The function counts are obligations (helpers included). `solc/open/` holds what did not close
(see "The `open/` directory").

## Running

```bash
./run-key.sh keyext.solidity.examples/solc/SolcArrays.sol                    # every function
./run-key.sh keyext.solidity.examples/solc/SolcArrays.sol pushWithArgument   # one function
./run-key.sh keyext.solidity.examples/solc/SolcModifiers.sol -c SolcModifiers  # file with base contracts
./run-key.sh keyext.solidity.examples/solc/SolcArrays.sol --solc             # the EVM cross-check
./gradlew :keyext.solidity.core:test --tests "*SolcSemanticsExamplesTest"
```

`SolcSemanticsExamplesTest` and the `solc/*.sol` half of `SolidityRuntimeExecutionTest` enumerate
the directory (not `open/`) and derive the contract name from the file name, so a new example
joins both by being written. `SolcModifiers.sol` and `SolcHigherLevel.sol` also declare base
contracts and an interface, so the CLI needs `-c`; the tests pass the name. Load a file first —
that compiles and type-checks the contract without proving, catching a Solidity error before a
multi-minute run:

```bash
./run-key.sh keyext.solidity.examples/solc/SolcArrays.sol --no-prove
```

`--no-prove` still prints `PASS … N/N closed`; that line then only means "loaded".

## Adaptation rules

Applied uniformly; every function names its upstream file in a `/// solc:` line.

- The upstream `// f() -> 42` expectation, or the SMTChecker's proved `assert`, becomes an
  in-body `assert`. An SMTChecker `assert` carrying an "Assertion violation" warning is dropped,
  or replaced by its negation or by the counterexample value when that holds on every run.
  Upstream asserts marked as aliasing false positives are kept: they are true.
- `return e;`, tuples, internal calls, loops, modifiers and constructor obligations are
  supported now. Ports written before that (the first six files) turn `return e;` into an
  `assert` over a bound local, tuple assignments into sequential assignments, and unroll loops.
- No constructor or non-constant state-variable initializer in a closing file:
  `SolidityRuntimeCheck` runs from all-zero storage and skips any contract that has one, which
  would hide every other function from the EVM check. Constructor bodies become internal
  helpers or assignments at the top of the body. A `constant` is allowed: the prover inlines
  its initializer and the runtime check does not skip it (older ports inline constants by
  hand). The faithful constructor port, which closes, is in `open/SolcConstructorsOpen.sol`.
- Symbolic execution starts from unconstrained storage, so wherever upstream relies on storage
  being zero-initialized (fresh deployment, a constructor-built state, a freshly pushed inner
  array) the premise is stated with `require` and the function is tagged `/// @custom:key box`.
  A box `require` that reverts on the EVM's fresh storage is skipped by the runtime check.
- Parameters: a `bool` becomes a `uint` with `require(p <= 1)` and the flag derived in the body;
  a symbolic parameter gets a range pin (`require(x >= 0 && x < N)`), which keeps the proof
  universal and gives `PinnedArguments` a witness for the EVM run. Only number literals pin (a
  `!=` conjunct pins nothing); enum and `bool` parameters cannot be pinned.
- Widths: older ports widen `uint8`/`uint16`/`bytesN` to `uint`. `SolcTypes` and
  `SolcArithmetic` keep widths where the width is the subject, and port only the
  value-preserving cases; wrapping and truncating ones are in `open/`. `address` stays
  `address` (equality and `address(n)` literals now discharge).
- Workarounds for the defects in `docs/bugs.md`, each named at the function: an uninitialised
  local gets an explicit `= 0`; an indexed parameter is aliased to a local first; a storage read
  on the right of a compound assignment is bound to a local; free and library functions,
  file-level declarations and abstract members move into the contract (with a `{ _; }` body);
  `this` as a call target becomes an `address p` pinned to the harness address
  `0x00000000000000000000000000000000DeaDBeef`; `type(T).max` becomes its literal; custom errors
  and string messages are dropped; inheritance chains are flattened into internal helpers,
  except in `SolcModifiers` and `SolcHigherLevel`, whose hierarchies are the subject.
- Fixed-size arrays (`S[5] data`) in the first ports become dynamic arrays with their length
  assumed.

## Deviations forced by the loader or calculus (first port)

- **Mapping members must be aliased before being indexed.** `nested.recursive[4].z` leaves an
  open goal; binding `mapping(uint => s2) storage map = nested.recursive;` first and using
  `map[4].z` closes. At depth two each member mapping needs its own alias. Upstream
  `struct_reference.sol` happens to be written that way already.
- **A `push` argument that reads storage must be bound first**, per the general convention in
  `../README.md`.

## The `open/` directory

`open/` holds, for each of the eleven newer themes, the upstream claims that do **not** close:
the faithful form of a port whose closing twin needed a workaround, and tests stopped by a
missing construct or a defect. Every file loads (`--no-prove`) and states something true on the
EVM, except the proves-false witnesses noted below. No test enumerates the directory, so nothing
here is red in CI; each function has a `// open:` line with the reason, and moving a function
into the closing file is how a fix is recorded. Four functions here do close:
`SolcConstructorsOpen.constructor` (true, kept out only because the runtime check skips
constructors), and `SolcPaymentsOpen.trySuccessKeepsStorageUnsound`/`callKeepsStorageUnsound`
and `SolcTypesOpen.ternaryLiteralOverflow` (false on the EVM: soundness witnesses, see
`docs/bugs.md`).

```bash
./run-key.sh keyext.solidity.examples/solc/open/SolcLoopsOpen.sol -f forBreakOrIncrement --open-goals
```

## Provenance

Generated from the `/// solc:` lines. The first six files name their upstream file in place.

### `SolcLoops.sol`

| Upstream (`test/libsolidity/`) | Functions |
|---|---|
| `semanticTests/statements/empty_for_loop.sol` | `emptyForLoopBreaksAtTen` |
| `semanticTests/viaYul/loops/simple.sol` | `forLoopDoubles`, `whileLoopDoubles`, `doWhileRunsUntilConditionFails` |
| `semanticTests/viaYul/loops/break.sol` | `forBreakAfterFirstIteration`, `whileBreakSkipsRestOfBody`, `doWhileBreakSkipsCondition` |
| `semanticTests/viaYul/loops/continue.sol` | `forContinueRunsUpdate`, `whileContinueSkipsRestOfBody`, `doWhileContinueEvaluatesCondition` |
| `semanticTests/viaYul/loops/return.sol` | `returnLeavesLoop` |
| `semanticTests/viaYul/if.sol` | `breakOutOfDoWhileFalseSmallSums`, `breakOutOfDoWhileFalseLargeSums` |
| `semanticTests/functionCall/calling_other_functions.sol` | `collatzReachesOne` |
| `semanticTests/array/array_storage_index_boundary_test.sol` | `boundaryTestPushedSlotsAreZero` |
| `semanticTests/array/dynamic_array_cleanup.sol` | `fillThenHalfClear` |
| `semanticTests/array/array_memory_index_access.sol` | `memoryArrayLoopWriteRead` |
| `semanticTests/array/array_2d_new.sol` | `newInnerArraysInLoop` |
| `smtCheckerTests/loops/do_while_1.sol` | `doWhileLeavesPositive` |
| `smtCheckerTests/loops/do_while_break_2.sol` | `innerDoWhileBreakLeavesOuterBody` |
| `smtCheckerTests/loops/for_1_continue.sol` | `forContinueRunsIncrement` |
| `smtCheckerTests/loops/for_break_direct.sol` | `forBreakDirectKeepsInit` |
| `smtCheckerTests/loops/for_loop_2.sol` | `forConditionHoldsInBody` |
| `smtCheckerTests/loops/while_loop_simple_2.sol` | `whileConditionHoldsInBody` |
| `smtCheckerTests/loops/while_loop_simple_4.sol` | `whileExitNegatesCondition` |
| `smtCheckerTests/loops/while_1.sol` | `whileResetOrIncrement` |
| `smtCheckerTests/loops/while_1_continue.sol` | `whileContinueOrIncrement` |
| `smtCheckerTests/loops/while_2.sol` | `whileCountsDownToOne` |
| `smtCheckerTests/loops/while_2_break.sol` | `whileBreakAfterIncrement` |
| `smtCheckerTests/loops/while_break_direct.sol` | `whileBreakDirect` |
| `smtCheckerTests/loops/do_while_bmc_iterations_nested_break.sol` | `nestedDoWhileBreak` |
| `smtCheckerTests/loops/do_while_bmc_iterations_nested_continue.sol` | `nestedDoWhileContinue` |
| `smtCheckerTests/loops/for_loop_bmc_iterations_8.sol` | `forCallsInConditionAndUpdate` |
| `smtCheckerTests/loops/while_bmc_iterations_6.sol` | `whileToggleConditionRunsOnce` |
| `smtCheckerTests/loops/do_while_bmc_iterations_6.sol` | `doWhileToggleConditionRunsTwice` |
| `smtCheckerTests/loops/for_loop_bmc_iterations_break_continue_3.sol` | `forContinueThenBreak` |
| `semanticTests/array/array_3d_new.sol` | `nestedLoopsAllocate3d` |
| `semanticTests/array/array_3d_assignment.sol` | `nestedLoopsAllocate3dThenAlias` |
| `semanticTests/array/array_memory_as_parameter.sol` | `loopFillsMemoryParameter` |
| `smtCheckerTests/loops/do_while_break.sol` | `doWhileBreakSkipsAssignment` |
| `smtCheckerTests/loops/while_bmc_iterations_nested_continue.sol` | `nestedWhileContinueSkipsCount` |
| `semanticTests/array/memory_arrays_of_various_sizes.sol` | `binomialRowsSmall` |
| `semanticTests/array/dynamic_arrays_in_storage.sol` | `setLengthsByPushLoops` |
| `smtCheckerTests/loops/for_loop_bmc_iterations_7.sol` | `forConditionCallRunsThrice` |
| `smtCheckerTests/loops/do_while_bmc_iterations_5.sol` | `doWhileConditionCallRunsThrice` |
| `smtCheckerTests/loops/do_while_bmc_iterations_break_continue_2.sol` | `doWhileContinueThenBreak` |
| `smtCheckerTests/loops/for_loop_bmc_iterations_break_5.sol` | `forSecondBreakUnreachable` |
| `smtCheckerTests/loops/for_loop_bmc_iterations_continue_4.sol` | `forContinueRunsUpdateAfterReassign` |
| `smtCheckerTests/loops/for_loop_bmc_iterations_shallow_unroll.sol` | `forShallowUnrollAssertIsFalse` |

### `SolcArrayMembers.sol`

| Upstream (`test/libsolidity/`) | Functions |
|---|---|
| `semanticTests/array/arrayMemoryAllocation/array_zeroed_memory_index_access.sol` | `memoryArrayZeroed` |
| `semanticTests/array/arrayMemoryAllocation/array_2d_zeroed_memory_index_access.sol` | `memory2dZeroed` |
| `semanticTests/array/arrayMemoryAllocation/array_static_zeroed_memory_index_access.sol` | `memoryStaticZeroed` |
| `semanticTests/array/arrayMemoryAllocation/array_array_static.sol` | `memoryArrayOfStaticZeroed` |
| `semanticTests/array/array_memory_create.sol` | `memoryCreateLength` |
| `semanticTests/array/create_dynamic_array_with_zero_length.sol` | `memoryZeroLength2d` |
| `semanticTests/array/create_multiple_dynamic_arrays.sol` | `memoryMultipleDynamic` |
| `semanticTests/array/create_memory_array.sol` | `memoryArrayOfFixed` |
| `semanticTests/array/delete/delete_memory_array.sol` | `deleteMemoryArray` |
| `semanticTests/array/pop/array_pop.sol` | `popTracksLength` |
| `semanticTests/array/push/array_push.sol` | `pushReadsBack` |
| `semanticTests/array/pop/array_pop_storage_empty.sol` | `pushPopLeavesEmpty` |
| `semanticTests/array/pop/parenthesized.sol` | `parenthesizedPop` |
| `semanticTests/array/push/push_no_args_1d.sol` | `pushNoArgs1dLvalue` |
| `semanticTests/array/push/push_no_args_struct.sol` | `pushNoArgsStructLvalue`, `pushNoArgsStruct` |
| `semanticTests/array/delete/delete_storage_array.sol` | `deleteStorageArray` |
| `semanticTests/array/copying/array_copy_clear_storage.sol` | `copyShrinksStorage` |
| `semanticTests/array/copying/array_copy_memory_to_storage.sol` | `copyMemoryToStorage`, `copyMemoryFixedToStorage` |
| `semanticTests/array/copying/nested_array_memory_to_storage.sol` | `copyNestedMemoryToStorage` |
| `semanticTests/array/copying/array_copy_storage_storage_dyn_dyn.sol` | `copyStorageStorageDynDyn` |
| `semanticTests/array/copying/array_copy_storage_to_memory_nested.sol` | `copyNestedStorageToMemory` |
| `semanticTests/array/copying/storage_memory_nested.sol` | `storageMemoryNestedFixed` |
| `semanticTests/array/copying/array_to_mapping.sol` | `arrayToMappingFromStorage` |
| `semanticTests/array/copying/nested_array_element_storage_to_storage.sol` | `nestedElementStorageToStorage` |
| `semanticTests/array/copying/nested_array_element_memory_to_memory.sol` | `nestedMemoryElementToMemory` |
| `semanticTests/array/fixed_arrays_in_storage.sol` | `fixedArraysInStorage` |
| `semanticTests/array/array_storage_push_empty_length_address.sol` | `setGetLength` |
| `semanticTests/storage/array_accessor.sol` | `arrayAccessor` |
| `semanticTests/storage/mappings_array2d_pop_delete.sol` | `mappingArray2dDelete`, `mappingArray2dPopRepush` |
| `smtCheckerTests/array_members/push_arg_1.sol` | `pushArgReadsBackAtEnd` |
| `smtCheckerTests/array_members/push_zero_safe.sol` | `pushZeroReadsZero` |
| `smtCheckerTests/array_members/push_zero_2d_safe.sol` | `pushZero2d` |
| `smtCheckerTests/array_members/push_push_no_args_1.sol` | `pushPushNoArgs2d` |
| `smtCheckerTests/array_members/push_push_no_args_2.sol` | `pushPushNoArgs3d` |
| `smtCheckerTests/array_members/push_as_lhs_1d.sol` | `pushLhs1dEmpty`, `pushLhs1dLast` |
| `smtCheckerTests/array_members/push_struct_member_2.sol` | `pushStructMember2` |
| `smtCheckerTests/array_members/push_storage_ref_safe_aliasing.sol` | `pushRefAliasing` |
| `smtCheckerTests/array_members/push_storage_ref_unsafe_aliasing.sol` | `pushRefAliasingWrite` |
| `smtCheckerTests/array_members/storage_pointer_push_1_safe.sol` | `storagePointerPushSafe` |
| `smtCheckerTests/array_members/pop_2d_safe.sol` | `pop2dSafe` |
| `smtCheckerTests/array_members/pop_loop_safe.sol` | `popLoopSafe` |
| `smtCheckerTests/array_members/push_overflow_1_safe_no_overflow_assumption.sol` | `pushKeepsFirst` |
| `smtCheckerTests/array_members/push_overflow_2_safe_no_overflow_assumption.sol` | `pushLoopKeepsFirst` |
| `smtCheckerTests/array_members/length_basic.sol` | `lengthBasic` |
| `smtCheckerTests/array_members/length_assignment_storage_to_storage.sol` | `lengthStorageToStorage` |
| `smtCheckerTests/array_members/length_copy_storage_to_memory.sol` | `lengthStorageToMemory` |
| `smtCheckerTests/array_members/length_copy_memory_to_storage.sol` | `lengthMemoryToStorage` |
| `smtCheckerTests/array_members/length_1d_assignment_2d_memory_to_memory.sol` | `length2dMemoryToMemory` |
| `smtCheckerTests/array_members/length_1d_copy_2d_storage_to_memory.sol` | `length2dStorageToMemory` |
| `smtCheckerTests/array_members/length_1d_mapping_array_2.sol` | `lengthMappingArray` |
| `smtCheckerTests/array_members/length_1d_mapping_array_2d_1.sol` | `lengthMappingArray2d` |
| `smtCheckerTests/array_members/length_1d_struct_array_2d_1.sol` | `lengthStructArray2d` |
| `smtCheckerTests/array_members/length_function_call.sol` | `lengthFunctionCall` |
| `smtCheckerTests/array_members/length_same_after_assignment.sol` | `lengthSameAfterMemoryCopy` |
| `smtCheckerTests/array_members/length_same_after_assignment_2.sol` | `lengthSameAfterElementWrite` |
| `smtCheckerTests/array_members/length_same_after_assignment_3.sol` | `lengthSameAfterRowCopy` |
| `semanticTests/array/push/array_push_nested.sol` | `pushNested` |

### `SolcFunctionCalls.sol`

| Upstream (`test/libsolidity/`) | Functions |
|---|---|
| `semanticTests/functionCall/named_args.sol` | `namedArgsOrdered` |
| `semanticTests/functionCall/named_args_overload.sol` | `namedArgsOverload` |
| `semanticTests/functionCall/multiple_return_values.sol` | `multipleReturnValues` |
| `semanticTests/functionCall/multiple_functions.sol` | `multipleFunctions` |
| `semanticTests/functionCall/array_multiple_local_vars.sol` | `arrayMultipleLocalVars`, `arrayMultipleLocalVarsBreak` |
| `semanticTests/freeFunctions/recursion.sol` | `freeFunctionRecursion` |
| `semanticTests/freeFunctions/easy.sol` | `freeFunctionEasy` |
| `smtCheckerTests/functions/free_function_multiple_contracts.sol` | `freeFunctionMultipleContracts` |
| `semanticTests/freeFunctions/storage_calldata_refs.sol` | `storageCalldataRefs` |
| `semanticTests/functionCall/calldata_argument_internal_dynamic_array_v2.sol` | `calldataArgumentInternalDynamicArray` |
| `semanticTests/functionCall/calldata_argument_internal_struct_v2.sol` | `calldataArgumentInternalStruct` |
| `semanticTests/functionCall/calldata_argument_internal_multi_array_v2.sol` | `calldataArgumentInternalMultiArray` |
| `semanticTests/functionCall/call_internal_function_via_expression.sol` | `callInternalFunctionViaExpression` |
| `smtCheckerTests/functions/function_inline_chain.sol` | `chainF` |
| `smtCheckerTests/functions/function_inside_branch_modify_state_var_2.sol` | `branchModifyStateVarBothBranches` |
| `smtCheckerTests/functions/function_inside_branch_modify_state_var.sol` | `branchModifyStateVarOneBranch` |
| `smtCheckerTests/functions/functions_identifier_nested_tuple_1.sol` | `identifierNestedTuple` |
| `smtCheckerTests/functions/functions_identity_1.sol` | `functionsIdentity1` |
| `smtCheckerTests/functions/functions_identity_2.sol` | `functionsIdentity2` |
| `smtCheckerTests/functions/functions_identity_as_tuple.sol` | `functionsIdentityAsTuple` |
| `smtCheckerTests/functions/functions_recursive.sol` | `functionsRecursive` |
| `smtCheckerTests/functions/functions_recursive_indirect.sol` | `functionsRecursiveIndirect` |
| `smtCheckerTests/functions/functions_trivial_condition_require_only_call.sol` | `trivialConditionRequireOnlyCall` |
| `smtCheckerTests/functions/internal_call_state_var_init.sol` | `internalCallStateVarInit` |
| `smtCheckerTests/functions/internal_call_state_var_init_2.sol` | `internalCallStateVarInit2` |
| `smtCheckerTests/functions/internal_call_with_assertion_1.sol` | `internalCallWithAssertion` |
| `smtCheckerTests/functions/internal_multiple_calls_with_assertion_1.sol` | `internalMultipleCallsWithAssertion` |
| `smtCheckerTests/functions/functions_library_1.sol` | `functionsLibrary1` |

### `SolcStructsMappings.sol`

| Upstream (`test/libsolidity/`) | Functions |
|---|---|
| `semanticTests/structs/delete_struct.sol` | `deleteStructKeepsNestedMappings` |
| `semanticTests/structs/struct_assign_reference_to_struct.sol` | `assignReferenceToStruct` |
| `semanticTests/structs/struct_copy.sol` | `structCopyBetweenMappingEntries`, `structCopyFromZeroEntry` |
| `semanticTests/structs/struct_delete_storage_small.sol` | `deleteSmallStruct` |
| `semanticTests/structs/struct_delete_storage_nested_small.sol` | `deleteRecursiveStruct` |
| `semanticTests/structs/struct_delete_storage_with_arrays_small.sol` | `deleteStructWithFixedAndDynamicArrays` |
| `semanticTests/structs/recursive_structs.sol` | `deleteRecursiveMemoryAndStorage` |
| `semanticTests/structs/memory_structs_nested_load.sol` | `loadNestedStructToMemory`, `storeNestedStructFromMemory` |
| `semanticTests/structs/copy_struct_with_nested_array_from_storage_to_storage.sol` | `copyStructWithNestedArrays` |
| `smtCheckerTests/types/struct/struct_return.sol` | `structReturnedFromInternal` |
| `smtCheckerTests/types/struct/struct_unary_add.sol` | `memoryStructIncrementAfterDelete` |
| `smtCheckerTests/types/struct/struct_unary_sub.sol` | `memoryStructDecrementAfterDelete` |
| `smtCheckerTests/types/struct/struct_delete_memory.sol` | `memoryStructDeleteResetsArray` |
| `smtCheckerTests/types/struct/struct_delete_storage.sol` | `storageStructDeleteResetsArray` |
| `smtCheckerTests/types/struct/struct_aliasing_storage.sol` | `storageTernaryStructAlias` |
| `smtCheckerTests/types/struct/struct_aliasing_memory.sol` | `memoryTernaryStructAlias` |
| `smtCheckerTests/types/struct/struct_recursive_4.sol` | `recursiveStructTernaryAliases` |
| `semanticTests/structs/conversion/recursive_storage_memory.sol` | `recursiveStorageToMemory` |
| `smtCheckerTests/types/struct/struct_aliasing_parameter_memory_1.sol` | `memoryStructParameterAliases` |
| `smtCheckerTests/types/struct/struct_aliasing_parameter_memory_2.sol` | `memoryInnerStructParameterAliases` |
| `smtCheckerTests/types/struct/struct_aliasing_parameter_memory_3.sol` | `memoryOuterStructParameterAliases` |
| `semanticTests/structs/memory_structs_as_function_args.sol` | `memoryStructsAsFunctionArgs` |
| `smtCheckerTests/types/struct_array_branches_1d.sol` | `memoryStructArrayBranch1d` |
| `smtCheckerTests/types/struct_array_branches_2d.sol` | `memoryStructArrayBranch2d` |
| `smtCheckerTests/types/struct_array_branches_3d.sol` | `memoryStructArrayLengths3d` |
| `smtCheckerTests/types/struct/struct_array_struct_array_memory_safe.sol` | `memoryNestedStructArrays` |
| `smtCheckerTests/types/struct/array_struct_array_struct_memory_safe.sol` | `memoryArrayOfNestedStructArrays` |
| `smtCheckerTests/types/mapping_1.sol` | `mappingWriteRead` |
| `smtCheckerTests/types/mapping_2d_1.sol` | `nestedMappingWriteRead` |
| `smtCheckerTests/types/mapping_3d_1.sol` | `tripleNestedMappingWriteRead` |
| `smtCheckerTests/types/mapping_3.sol` | `mappingOtherKeyUnchanged` |
| `smtCheckerTests/types/mapping_4.sol` | `boolMappingDefault` |
| `smtCheckerTests/types/mapping_equal_keys_1.sol` | `mappingEqualKeys` |
| `smtCheckerTests/types/mapping_aliasing_1.sol` | `mappingAliasReadsThrough` |
| `smtCheckerTests/types/mapping_as_local_var_1.sol` | `mappingTernaryLocalAlias` |
| `smtCheckerTests/types/mapping_as_parameter_1.sol` | `mappingAsParameter` |
| `semanticTests/functionCall/mapping_internal_argument.sol` | `mappingAsInternalArgument` |
| `semanticTests/functionCall/mapping_array_internal_argument.sol` | `fixedArrayOfMappingsAsInternalArgument` |
| `semanticTests/functionCall/mapping_internal_return.sol` | `mappingReturnedFromInternal` |
| `semanticTests/variables/mapping_local_assignment.sol` | `mappingLocalReassignment` |
| `semanticTests/variables/mapping_local_tuple_assignment.sol` | `mappingLocalTupleReassignment` |
| `semanticTests/types/mapping_simple.sol` | `mappingSetGetSequence` |
| `semanticTests/storage/mappings_array_pop_delete.sol` | `mappingsSurvivePopAndDelete` |
| `semanticTests/viaYul/storage/mappings.sol` | `mappingComputedKeys`, `twoDimMappingTransposedKey` |
| `semanticTests/storage/accessors_mapping_for_array.sol` | `mappingOfArrays` |
| `smtCheckerTests/types/mapping_aliasing_2.sol` | `mappingParametersMayAlias` |
| `smtCheckerTests/types/array_mapping_aliasing_1.sol` | `arrayOfMappingsElementAlias` |

### `SolcConstructors.sol`

| Upstream (`test/libsolidity/`) | Functions |
|---|---|
| `semanticTests/constructor/state_variable_initialization.sol` | `stateVariableInitialization` |
| `semanticTests/constructor/base_constructor_arguments.sol` | `baseConstructorArguments` |
| `semanticTests/constructor/inline_member_init_inheritence_without_constructor.sol` | `inlineMemberInit` |
| `semanticTests/constructor/functions_called_by_constructor.sol` | `functionsCalledByConstructor` |
| `semanticTests/constructor/constructor_arguments_internal.sol` | `constructorArgumentsInternal` |
| `semanticTests/constructor/order_of_evaluation.sol` | `orderOfEvaluation` |
| `semanticTests/constructor/arrays_in_constructors.sol` | `arraysInConstructors` |
| `semanticTests/immutable/assign_from_immutables.sol` | `assignFromImmutables` |
| `semanticTests/immutable/delete.sol` | `deleteImmutable` |
| `semanticTests/immutable/fun_read_in_ctor.sol` | `funReadInCtor` |
| `semanticTests/immutable/read_in_ctor.sol` | `readInCtor` |
| `semanticTests/immutable/multiple_initializations.sol` | `multipleInitializations` |
| `semanticTests/immutable/increment_decrement.sol` | `incrementDecrement` |
| `semanticTests/immutable/stub.sol` | `immutableStub` |
| `semanticTests/scoping/c99_scoping_activation.sol` | `c99ScopingAssignOuter`, `c99ScopingReadOuter`, `c99ScopingSelfInit` |
| `smtCheckerTests/functions/constructor_simple.sol` | `constructorSimple` |
| `smtCheckerTests/functions/constructor_state_value.sol` | `constructorStateValue` |
| `smtCheckerTests/functions/constructor_state_value_parameter.sol` | `constructorStateValueParameter` |
| `smtCheckerTests/array_members/pop_constructor_safe.sol` | `popConstructorSafe` |
| `smtCheckerTests/inheritance/constructor_state_variable_init.sol` | `constructorStateVariableInit` |
| `smtCheckerTests/inheritance/constructor_state_variable_init_chain.sol` | `constructorStateVariableInitChain` |
| `smtCheckerTests/inheritance/constructor_state_variable_init_asserts.sol` | `constructorStateVariableInitAsserts` |
| `smtCheckerTests/inheritance/constructor_hierarchy_base_calls_with_side_effects_1.sol` | `constructorBaseCallsWithSideEffects` |
| `smtCheckerTests/inheritance/constructor_state_variable_init_function_call.sol` | `constructorInitFunctionCall` |
| `smtCheckerTests/inheritance/constructor_uses_function_base.sol` | `constructorUsesFunctionBase` |
| `smtCheckerTests/functions/constructor_hierarchy_same_var.sol` | `constructorHierarchySameVar` |
| `smtCheckerTests/functions/constructor_hierarchy_mixed_chain_local_vars.sol` | `constructorMixedChainLocalVars` |
| `smtCheckerTests/functions/constructor_hierarchy_modifier.sol` | `constructorHierarchyModifier` |
| `smtCheckerTests/file_level/easy.sol` | `fileLevelEasy` |
| `smtCheckerTests/file_level/enum.sol` | `fileLevelEnum` |
| `smtCheckerTests/file_level/struct.sol` | `fileLevelStruct` |
| `semanticTests/variables/public_state_overridding.sol` | `publicStateOverridding` |
| `semanticTests/constants/simple_constant_variables_test.sol` | `simpleConstantVariables` |

### `SolcPayments.sol`

| Upstream (`test/libsolidity/`) | Functions |
|---|---|
| `smtCheckerTests/special/msg_sender_1.sol` | `msgSenderTwice` |
| `smtCheckerTests/special/msg_sender_2.sol` | `msgSenderNonZero` |
| `smtCheckerTests/special/msg_vars_chc_internal.sol` | `msgVarsInternal` |
| `smtCheckerTests/special/ether_units.sol` | `etherUnits` |
| `smtCheckerTests/special/time_units.sol` | `timeUnits` |
| `smtCheckerTests/special/shadowing_1.sol` | `shadowBuiltins` |
| `smtCheckerTests/functions/payable_2.sol` | `payableRequireThenAssert` |
| `semanticTests/payable/no_nonpayable_circumvention_by_modifier.sol` | `noCircumventionByModifier` |
| `smtCheckerTests/blockchain_state/transfer_4.sol` | `transferAfterPayment` |
| `semanticTests/reverts/assert_require.sol` | `assertValTrue`, `requireValTrue` |
| `semanticTests/reverts/simple_throw.sol` | `simpleThrowTaken`, `simpleThrowReverts` |
| `semanticTests/reverts/revert.sol` | `revertAfterWrite` |
| `semanticTests/reverts/error_struct.sol` | `errorStruct` |
| `semanticTests/errors/require_error_condition_evaluated_only_once.sol` | `conditionEvaluatedOnce` |
| `semanticTests/errors/require_error_evaluation_order_2.sol` | `evaluationOrder` |
| `semanticTests/errors/require_error_function_join_control_flow.sol` | `joinControlFlow`, `joinControlFlowSecondCallReverts` |
| `semanticTests/tryCatch/simple.sol` | `tryCatchSimple` |
| `semanticTests/tryCatch/simple_notuple.sol` | `tryCatchNotuple` |
| `semanticTests/tryCatch/require.sol` | `tryCatchRequire` |
| `semanticTests/tryCatch/assert.sol` | `tryCatchAssert` |
| `semanticTests/tryCatch/structured.sol` | `tryCatchStructured` |
| `semanticTests/tryCatch/nested.sol` | `tryCatchNested` |
| `semanticTests/tryCatch/panic.sol` | `tryCatchPanic` |
| `smtCheckerTests/try_catch/try_1.sol` | `tryCatchKeepsState` |
| `smtCheckerTests/try_catch/try_4.sol` | `tryCatchRevertsChanges` |
| `smtCheckerTests/try_catch/try_5.sol` | `tryCatchBoundKept` |
| `smtCheckerTests/try_catch/try_multiple_catch_clauses.sol` | `tryMultipleCatchClausesCatch` |
| `smtCheckerTests/try_catch/try_multiple_catch_clauses_2.sol` | `tryMultipleCatchClauses2` |
| `smtCheckerTests/try_catch/try_nested_2.sol` | `tryNested2` |
| `smtCheckerTests/try_catch/try_multiple_returned_values.sol` | `tryReturnsShadowState` |
| `semanticTests/fallback/falback_return.sol` | `fallbackReturn` |
| `semanticTests/fallback/fallback_or_receive.sol` | `fallbackOrReceive` |

### `SolcModifiers.sol`

| Upstream (`test/libsolidity/`) | Functions |
|---|---|
| `semanticTests/modifiers/return_does_not_skip_modifier.sol` | `returnDoesNotSkipModifier` |
| `semanticTests/modifiers/function_modifier_multiple_times.sol` | `modifierMultipleTimes` |
| `semanticTests/modifiers/function_modifier_local_variables.sol` | `modifierLocalVariablesTrue`, `modifierLocalVariablesFalse` |
| `semanticTests/modifiers/function_modifier.sol` | `modifierOnMsgValue` |
| `semanticTests/modifiers/function_modifier_overriding.sol` | `modifierSkipsBody` |
| `semanticTests/modifiers/access_through_contract_name.sol` | `modifierAccessThroughContractName` |
| `semanticTests/modifiers/function_return_parameter.sol` | `returnParameterIntoModifier` |
| `semanticTests/modifiers/modifer_recursive.sol` | `modifierRecursive` |
| `semanticTests/modifiers/function_return_parameter_complex.sol` | `returnParameterForwarded`, `returnParameterForwardedFourTimes` |
| `semanticTests/modifiers/access_through_module_name.sol` | `modifierRunsBeforeTupleBody` |
| `semanticTests/modifiers/transient_state_variable_value_type.sol` | `modifierArgumentReadsStateFirst` |
| `semanticTests/modifiers/modifier_init_return.sol` | `modifierConditionBelow`, `modifierConditionAbove` |
| `smtCheckerTests/modifiers/modifier_simple.sol` | `smtModifierSimple` |
| `smtCheckerTests/modifiers/modifier_overflow.sol` | `smtModifierOverflow` |
| `smtCheckerTests/modifiers/modifier_two_invocations.sol` | `smtTwoInvocations` |
| `smtCheckerTests/modifiers/modifier_multi.sol` | `smtMulti` |
| `smtCheckerTests/modifiers/modifier_control_flow.sol` | `smtControlFlow` |
| `smtCheckerTests/modifiers/modifier_multi_parameters.sol` | `smtMultiParameters` |
| `smtCheckerTests/modifiers/modifier_parameters.sol` | `smtParameters` |
| `smtCheckerTests/modifiers/modifier_same_local_variables.sol` | `smtSameLocalVariables` |
| `smtCheckerTests/modifiers/modifier_inside_branch_assignment_branch.sol` | `smtInsideBranchAssignmentBranch` |
| `smtCheckerTests/modifiers/modifier_inside_branch_assignment_multi_branches.sol` | `smtInsideBranchMultiBranches` |
| `smtCheckerTests/modifiers/modifier_inside_branch.sol` | `smtInsideBranch` |
| `smtCheckerTests/modifiers/modifier_overriding_1.sol` | `smtOverriding1` |
| `smtCheckerTests/modifiers/modifier_overriding_2.sol` | `smtOverriding2` |
| `smtCheckerTests/modifiers/modifier_overriding_3.sol` | `smtOverriding3` |
| `smtCheckerTests/modifiers/modifier_overriding_4.sol` | `smtOverriding4` |
| `smtCheckerTests/modifiers/modifier_virtual_static_call_2.sol` | `smtVirtualStaticCall2` |
| `smtCheckerTests/modifiers/modifier_assignment_outside_branch.sol` | `smtAssignmentOutsideBranch` |
| `smtCheckerTests/modifiers/modifier_code_after_placeholder.sol` | `smtCodeAfterPlaceholder` |
| `semanticTests/modifiers/function_modifier_library.sol` | `storageModifierParameter` |

### `SolcSmtControlFlow.sol`

| Upstream (`test/libsolidity/`) | Functions |
|---|---|
| `smtCheckerTests/control_flow/assignment_in_declaration.sol` | `assignmentInDeclaration` |
| `smtCheckerTests/control_flow/branches_assert_condition_1.sol` | `branchesAssertCondition1` |
| `smtCheckerTests/control_flow/branches_assert_condition_2.sol` | `branchesAssertCondition2` |
| `smtCheckerTests/control_flow/branches_inside_modifiers_1.sol` | `branchesInsideModifiers1` |
| `smtCheckerTests/control_flow/branches_inside_modifiers_2.sol` | `branchesInsideModifiers2` |
| `smtCheckerTests/control_flow/branches_inside_modifiers_3.sol` | `branchesInsideModifiers3` |
| `smtCheckerTests/control_flow/branches_inside_modifiers_4.sol` | `branchesInsideModifiers4` |
| `smtCheckerTests/control_flow/branches_merge_variables_1.sol` | `branchesMergeVariables1` |
| `smtCheckerTests/control_flow/branches_merge_variables_2.sol` | `branchesMergeVariables2` |
| `smtCheckerTests/control_flow/branches_merge_variables_3.sol` | `branchesMergeVariables3` |
| `smtCheckerTests/control_flow/branches_merge_variables_4.sol` | `branchesMergeVariables4` |
| `smtCheckerTests/control_flow/branches_merge_variables_5.sol` | `branchesMergeVariables5` |
| `smtCheckerTests/control_flow/branches_merge_variables_6.sol` | `branchesMergeVariables6` |
| `smtCheckerTests/control_flow/function_call_inside_branch.sol` | `functionCallInsideBranch` |
| `smtCheckerTests/control_flow/function_call_inside_branch_2.sol` | `functionCallInsideBranch2` |
| `smtCheckerTests/control_flow/function_call_inside_else_branch.sol` | `functionCallInsideElseBranch` |
| `smtCheckerTests/control_flow/function_call_inside_placeholder_inside_modifier_branch.sol` | `functionCallInsidePlaceholderInsideModifierBranch` |
| `smtCheckerTests/control_flow/require.sol` | `requireFalseUnreachable`, `requireFalseInBranch` |
| `smtCheckerTests/control_flow/return_1.sol` | `return1` |
| `smtCheckerTests/control_flow/return_2.sol` | `return2` |
| `smtCheckerTests/control_flow/revert.sol` | `revertUnreachable`, `revertInBranch` |
| `smtCheckerTests/control_flow/revert_complex_flow.sol` | `revertComplexFlow` |
| `smtCheckerTests/control_flow/short_circuit_and.sol` | `shortCircuitAnd` |
| `smtCheckerTests/control_flow/short_circuit_and_need_both.sol` | `shortCircuitAndNeedBoth` |
| `smtCheckerTests/control_flow/short_circuit_or.sol` | `shortCircuitOr` |
| `smtCheckerTests/control_flow/short_circuit_or_need_both.sol` | `shortCircuitOrNeedBoth` |
| `smtCheckerTests/control_flow/short_circuit_or_inside_branch.sol` | `shortCircuitOrInsideBranch` |
| `smtCheckerTests/control_flow/short_circuit_and_inside_branch.sol` | `shortCircuitAndInsideBranch` |
| `smtCheckerTests/control_flow/short_circuit_and_touched_function.sol` | `shortCircuitAndTouchedFunction` |
| `smtCheckerTests/control_flow/short_circuit_or_touched_function.sol` | `shortCircuitOrTouchedFunction` |
| `smtCheckerTests/control_flow/side_effects_inside_if_1.sol` | `sideEffectsInsideIf1` |
| `smtCheckerTests/control_flow/side_effects_inside_if_2.sol` | `sideEffectsInsideIf2` |
| `smtCheckerTests/control_flow/side_effects_inside_if_3.sol` | `sideEffectsInsideIf3` |
| `smtCheckerTests/control_flow/side_effects_inside_if_4.sol` | `sideEffectsInsideIf4` |
| `smtCheckerTests/control_flow/side_effects_inside_ternary_1.sol` | `sideEffectsInsideTernary1` |
| `smtCheckerTests/control_flow/side_effects_inside_ternary_2.sol` | `sideEffectsInsideTernary2` |
| `smtCheckerTests/control_flow/side_effects_inside_ternary_3.sol` | `sideEffectsInsideTernary3` |
| `smtCheckerTests/control_flow/side_effects_inside_ternary_4.sol` | `sideEffectsInsideTernary4` |
| `smtCheckerTests/control_flow/branches_with_return/nested_if.sol` | `nestedIfNever42` |
| `smtCheckerTests/control_flow/branches_with_return/return_in_both_branches.sol` | `returnInBothBranches` |
| `smtCheckerTests/control_flow/branches_with_return/simple_if.sol` | `simpleIfGuardsDivision` |
| `smtCheckerTests/control_flow/branches_with_return/simple_if_state_var.sol` | `simpleIfStateVar` |
| `smtCheckerTests/control_flow/branches_with_return/simple_if_struct.sol` | `simpleIfStruct` |
| `smtCheckerTests/control_flow/branches_with_return/simple_if_struct_2.sol` | `simpleIfStruct2` |
| `smtCheckerTests/control_flow/branches_with_return/simple_if_array.sol` | `simpleIfArray` |
| `smtCheckerTests/control_flow/branches_with_return/simple_if_tuple.sol` | `simpleIfTuple` |
| `smtCheckerTests/control_flow/branches_with_return/triple_nested_if.sol` | `tripleNestedIf` |
| `smtCheckerTests/control_flow/branches_with_return/constructors.sol` | `constructorsReturnInBothBranches` |
| `smtCheckerTests/control_flow/branches_with_return/constructor_state_variable_init_diamond.sol` | `constructorDiamondD1`, `constructorDiamondD2`, `constructorDiamondD3`, `constructorDiamondD4` |
| `smtCheckerTests/verification_target/simple_assert_with_require.sol` | `simpleAssertWithRequire` |
| `smtCheckerTests/verification_target/constant_condition_2.sol` | `constantCondition2NeverReverts` |
| `smtCheckerTests/bmc_coverage/assert.sol` | `bmcConditionUnreachable`, `bmcContradictoryRequires` |
| `smtCheckerTests/bmc_coverage/assert_in_constructor.sol` | `assertInConstructor` |
| `smtCheckerTests/invariants/loop_basic_for.sol` | `loopBasicFor` |
| `smtCheckerTests/invariants/loop_basic.sol` | `loopBasic` |
| `smtCheckerTests/invariants/loop_nested.sol` | `loopNested` |
| `smtCheckerTests/invariants/loop_nested_for.sol` | `loopNestedFor` |
| `smtCheckerTests/invariants/array_access.sol` | `arrayAccess` |
| `smtCheckerTests/invariants/struct_access.sol` | `structAccess` |
| `smtCheckerTests/invariants/unary_minus_formatting.sol` | `unaryMinusFormatting` |
| `smtCheckerTests/invariants/state_machine_1.sol` | `stateMachineStep` |
| `smtCheckerTests/invariants/aon_blog_post.sol` | `aonBlogPostSolvable` |
| `smtCheckerTests/complex/slither/data_dependency.sol` | `dataDependencyReferenceSet3`, `dataDependencyPropagateThroughArguments`, `dataDependencyPropagateThroughReturnValue` |
| `smtCheckerTests/control_flow/function_call_inside_branch_3.sol` | `functionCallInsideBranch3` |
| `smtCheckerTests/control_flow/ways_to_merge_variables_1.sol` | `waysToMergeVariables1` |
| `smtCheckerTests/control_flow/ways_to_merge_variables_2.sol` | `waysToMergeVariables2` |
| `smtCheckerTests/control_flow/ways_to_merge_variables_3.sol` | `waysToMergeVariables3` |
| `smtCheckerTests/control_flow/branches_with_return/simple_if2.sol` | `simpleIf2Counterexample` |
| `smtCheckerTests/control_flow/branches_with_return/constructor_state_variable_init.sol` | `constructorStateVariableInit` |
| `smtCheckerTests/control_flow/branches_with_return/constructor_state_variable_init_chain_alternate.sol` | `constructorStateVariableInitChainAlternate` |
| `smtCheckerTests/complex/slither/const_state_variables.sol` | `constStateVariable` |

### `SolcArithmetic.sol`

| Upstream (`test/libsolidity/`) | Functions |
|---|---|
| `semanticTests/arithmetics/divisiod_by_zero.sol` | `divisionTruncates`, `moduloRemainder` |
| `semanticTests/arithmetics/unchecked_div_by_zero.sol` | `uncheckedDivision`, `uncheckedModulo` |
| `semanticTests/arithmetics/signed_mod.sol` | `signedModPositive`, `signedModNegativeDivisor`, `signedModNegativeDividend`, `signedModBothNegative` |
| `semanticTests/arithmetics/exp_associativity.sol` | `expRightAssociativeThree`, `expRightAssociativeFour`, `expInvariant`, `expLiteralMix`, `expOtherOperators` |
| `semanticTests/exponentiation/literal_base.sol` | `literalBaseThirteen`, `literalBaseEvenExponent` |
| `semanticTests/integer/small_signed_types.sol` | `smallSignedTypesProduct` |
| `semanticTests/integer/many_local_variables.sol` | `manyLocalVariables` |
| `semanticTests/operators/compound_assign.sol` | `compoundAssignSequence` |
| `smtCheckerTests/operators/division_truncates_correctly_1.sol` | `divisionTruncatesUnsigned` |
| `smtCheckerTests/operators/division_truncates_correctly_2.sol` | `divisionTruncatesSignedPositive` |
| `smtCheckerTests/operators/division_truncates_correctly_3.sol` | `divisionTruncatesNegativeDividend` |
| `smtCheckerTests/operators/division_truncates_correctly_4.sol` | `divisionTruncatesNegativeDivisor` |
| `smtCheckerTests/operators/division_truncates_correctly_5.sol` | `divisionTruncatesBothNegative` |
| `smtCheckerTests/operators/mod.sol` | `modSignIgnoresDivisor` |
| `smtCheckerTests/operators/compound_add.sol` | `compoundAddSelfReference` |
| `smtCheckerTests/operators/compound_mul.sol` | `compoundMulSelfReference` |
| `smtCheckerTests/operators/compound_assignment_division_1.sol` | `compoundDivSelfReference` |
| `smtCheckerTests/operators/compound_add_mapping.sol` | `compoundAddMapping` |
| `smtCheckerTests/operators/compound_mul_mapping.sol` | `compoundMulMapping` |
| `smtCheckerTests/operators/compound_assignment_division_3.sol` | `compoundDivMapping` |
| `smtCheckerTests/operators/compound_add_array_index.sol` | `compoundAddArrayIndex` |
| `smtCheckerTests/operators/compound_mul_array_index.sol` | `compoundMulArrayIndex` |
| `smtCheckerTests/operators/compound_assignment_division_2.sol` | `compoundDivArrayIndex` |
| `smtCheckerTests/operators/unary_add.sol` | `unaryAddLocal` |
| `smtCheckerTests/operators/unary_sub.sol` | `unarySubLocal` |
| `smtCheckerTests/operators/unary_add_mapping.sol` | `unaryAddMapping` |
| `smtCheckerTests/operators/unary_sub_mapping.sol` | `unarySubMapping` |
| `smtCheckerTests/operators/unary_add_array.sol` | `unaryAddArray` |
| `smtCheckerTests/operators/unary_sub_array.sol` | `unarySubArray` |
| `smtCheckerTests/overflow/overflow_and_underflow_chc.sol` | `signedSumOfZeros` |
| `smtCheckerTests/overflow/unsigned_guard_sub_overflow.sol` | `guardedSubtractionNonNegative` |
| `semanticTests/arithmetics/unchecked_called_by_checked.sol` | `uncheckedCalledByChecked` |
| `smtCheckerTests/operators/constant_propagation_1.sol` | `constantPropagationPower` |
| `smtCheckerTests/operators/constant_propagation_2.sol` | `constantPropagationDivision` |
| `smtCheckerTests/operators/const_exp_1.sol` | `constantExponent` |
| `smtCheckerTests/operators/constant_evaluation_unary_minus_chc.sol` | `constantUnaryMinus` |

### `SolcHigherLevel.sol`

| Upstream (`test/libsolidity/`) | Functions |
|---|---|
| `semanticTests/inheritance/access_base_storage.sol` | `accessBaseStorage` |
| `semanticTests/inheritance/derived_overload_base_function_direct.sol` | `derivedOverloadBaseDirect` |
| `semanticTests/inheritance/derived_overload_base_function_indirect.sol` | `derivedOverloadBaseIndirect` |
| `semanticTests/inheritance/overloaded_function_call_resolve_to_first.sol` | `overloadResolveToFirst` |
| `semanticTests/inheritance/overloaded_function_call_resolve_to_second.sol` | `overloadResolveToSecond` |
| `semanticTests/inheritance/overloaded_function_call_with_if_else.sol` | `overloadWithIfElse` |
| `semanticTests/inheritance/inherited_function_named_parameters.sol` | `overrideNamedOrdered` |
| `semanticTests/virtualFunctions/virtual_function_calls.sol` | `virtualFunctionCallDirect` |
| `semanticTests/virtualFunctions/virtual_override_changing_mutability_internal.sol` | `overrideChangingMutability` |
| `smtCheckerTests/inheritance/state_variables_2.sol` | `baseStateVariablesChain` |
| `smtCheckerTests/inheritance/state_variables_3.sol` | `baseStateVariablesPrivate` |
| `semanticTests/getters/value_types.sol` | `getterValueTypes` |
| `semanticTests/getters/mapping.sol` | `getterNestedMapping` |
| `semanticTests/getters/mapping_with_names.sol` | `getterMappingWithNames` |
| `semanticTests/getters/mapping_to_struct.sol` | `getterMappingToStruct` |
| `semanticTests/getters/mapping_array_struct.sol` | `getterMappingArrayStruct` |
| `semanticTests/getters/array_mapping_struct.sol` | `getterArrayMappingStruct` |
| `semanticTests/getters/arrays.sol` | `getterArrays` |
| `semanticTests/inheritance/inherited_constant_state_var.sol` | `inheritedConstantStateVar` |

### `SolcTypes.sol`

| Upstream (`test/libsolidity/`) | Functions |
|---|---|
| `semanticTests/enums/using_enums.sol` | `usingEnums` |
| `semanticTests/enums/enum_referencing.sol` | `enumReferencing` |
| `semanticTests/enums/enum_with_256_members.sol` | `enumWith256Members` |
| `semanticTests/types/mapping_enum_key_v1.sol` | `mappingEnumKey` |
| `semanticTests/types/nested_tuples.sol` | `nestedTuplesTrailingWildcard` |
| `semanticTests/literals/denominations.sol` | `denominations`, `denominationsConstant` |
| `semanticTests/literals/ether.sol` | `etherDenomination` |
| `semanticTests/literals/gwei.sol` | `gweiDenomination` |
| `semanticTests/literals/wei.sol` | `weiDenomination` |
| `semanticTests/literals/fractional_denominations.sol` | `fractionalDenominations` |
| `semanticTests/literals/scientific_notation.sol` | `scientificNotation` |
| `smtCheckerTests/types/bool_int_mixed_1.sol` | `boolIntMixed1` |
| `smtCheckerTests/types/bool_int_mixed_2.sol` | `boolIntMixed2` |
| `smtCheckerTests/types/bool_int_mixed_3.sol` | `boolIntMixed3` |
| `smtCheckerTests/types/bool_simple_3.sol` | `boolSimple3` |
| `smtCheckerTests/types/bool_simple_4.sol` | `boolSimple4` |
| `smtCheckerTests/types/bool_simple_5.sol` | `boolSimple5` |
| `smtCheckerTests/types/bool_simple_6.sol` | `boolSimple6` |
| `smtCheckerTests/types/enum_explicit_values.sol` | `enumExplicitValues` |
| `smtCheckerTests/types/enum_in_struct.sol` | `enumInStruct` |
| `smtCheckerTests/types/enum_transitivity.sol` | `enumTransitivity` |
| `smtCheckerTests/types/enum_in_library.sol` | `enumParamReassigned` |
| `smtCheckerTests/types/storage_value_vars_3.sol` | `storageValueVars3` |
| `smtCheckerTests/types/rational_large_1.sol` | `rationalLarge` |
| `smtCheckerTests/types/tuple_assignment.sol` | `tupleAssignment` |
| `smtCheckerTests/types/tuple_assignment_compound.sol` | `tupleAssignmentCompound` |
| `smtCheckerTests/types/tuple_declarations.sol` | `tupleDeclarations` |
| `smtCheckerTests/types/tuple_declarations_function.sol` | `tupleDeclarationsFunction` |
| `smtCheckerTests/types/tuple_function_2.sol` | `tupleFunctionWildcard` |
| `smtCheckerTests/types/tuple_1_chain_1.sol` | `tupleSingleElementChain` |
| `smtCheckerTests/typecast/address_literal.sol` | `addressLiteral` |
| `smtCheckerTests/typecast/cast_address_1.sol` | `castAddress` |
| `smtCheckerTests/typecast/cast_larger_2.sol` | `castLarger` |
| `smtCheckerTests/typecast/downcast.sol` | `downcastSignedPreserving` |
| `smtCheckerTests/typecast/number_literal.sol` | `numberLiteral` |
| `smtCheckerTests/typecast/same_size.sol` | `sameSizePreserving` |
| `smtCheckerTests/typecast/upcast.sol` | `upcastPreserving` |
| `semanticTests/enums/using_contract_enums_with_explicit_contract_name.sol` | `enumExplicitContractName` |

## Provenance of `open/`

### `open/SolcLoopsOpen.sol`

| Function | Upstream (`test/libsolidity/`) | Why it stays open |
|---|---|---|
| `forBreakOrIncrement` | `smtCheckerTests/loops/for_1_break.sol` | the invariant cannot name the lowered break flag, so the exit by break keeps x in [0, 10) |
| `whileBreakOrIncrement` | `smtCheckerTests/loops/while_1_break.sol` | the invariant cannot name the lowered break flag, so the exit by break keeps x in [0, 10) |
| `nestedWhileBreak` | `smtCheckerTests/loops/while_nested_break.sol` | the invariant cannot name the lowered break flag of the inner loop |
| `nestedWhileContinue` | `smtCheckerTests/loops/while_nested_continue.sol` | the invariant cannot name the lowered break flag of the inner loop |
| `doWhileBreakDefaultZero` | `smtCheckerTests/loops/do_while_break.sol` | a local declared without initializer is not zero |
| `binomialFaithfulRowsSmall` | `semanticTests/array/memory_arrays_of_various_sizes.sol` | new uint256[][](n + 1) with a non-simple length is stuck on the program text |
| `binomialRowsNine` | `semanticTests/array/memory_arrays_of_various_sizes.sol` | nested memory rows: does not close within the default budget, nor with -m 1000000 |
| `multiArrayFillByPushLoops` | `semanticTests/array/dynamic_multi_array_cleanup.sol` | a pushed struct gets no empty array member, so the inner push loops start from an unknown length |
| `pushNoArgs2dLoop` | `semanticTests/array/push/push_no_args_2d.sol` | a pushed inner array is not known to be empty, so its length after the loop is unknown |
| `pushChainedAssign` | `semanticTests/array/push/push_no_args_2d.sol` | a pushed inner array is not known to be empty, so the second push writes at an unknown index |

### `open/SolcArrayMembersOpen.sol`

| Function | Upstream (`test/libsolidity/`) | Why it stays open |
|---|---|---|
| `pushNoArgs1d` | `semanticTests/array/push/push_no_args_1d.sol` | push() used as an rvalue (uint y = arr.push();) is stuck on the program text |
| `pushNested` | `semanticTests/array/push/array_push_nested.sol` | push() onto an array of arrays leaves the new element's length unknown, so nested[0].length == 0 does not follow |
| `pushNestedFromMemory` | `semanticTests/array/push/array_push_nested_from_memory.sol` | push(m) of a memory array onto a storage array of arrays is stuck on the program text |
| `copyStorageStorageDynDyn` | `semanticTests/array/copying/array_copy_storage_storage_dyn_dyn.sol` | assigning new uint[](n) directly to a storage array is stuck on the program text |
| `copyNestedStorageToMemory` | `semanticTests/array/copying/array_copy_storage_to_memory_nested.sol` | the pushed inner arrays start from an unknown length, so the pushed values land at unknown indices |
| `pushZero2d` | `smtCheckerTests/array_members/push_zero_2d_safe.sol` | the pushed inner array starts from an unknown length, so index 0 is not the slot the second push cleared |
| `pushLhs2dLast` | `smtCheckerTests/array_members/push_as_lhs_2d.sol` | c.push().push() = 2 appends to an inner array of unknown length, so its length is not known to be 1 |
| `pushLhs3dLast` | `smtCheckerTests/array_members/push_as_lhs_3d.sol` | same as pushLhs2dLast one level deeper |
| `pushLhsAndRhs1d` | `smtCheckerTests/array_members/push_as_lhs_and_rhs_1d.sol` | a.push() = a.push() is desugared to a.push(a.push()), whose push() argument is stuck on the program text |
| `pushLhsCompound` | `smtCheckerTests/array_members/push_as_lhs_compound_assignment.sol` | a compound assignment to push() (u.push() -= 1) is stuck on the program text |
| `pushLhsStruct` | `smtCheckerTests/array_members/push_as_lhs_struct.sol` | same as pushLhsAndRhs1d through struct members |
| `pushMemoryRowThenValue` | `smtCheckerTests/array_members/push_2d_arg_1_safe.sol` | push(x) of a memory array onto a storage array of arrays is stuck on the program text |
| `storagePointerPush` | `smtCheckerTests/array_members/storage_pointer_push_1.sol` | push() on the storage reference an internal call returns (f().push()) is stuck on the program text |
| `popIsolated` | `semanticTests/array/pop/array_pop_isolated.sol` | the bare member access popIso.pop; as a statement is stuck on the program text |
| `pushStructFromMemory` | `semanticTests/array/push/array_push_struct.sol` | push(s) of a memory struct onto a storage array of structs is stuck on the program text |
| `copyStaticToDynamic` | `semanticTests/array/copying/array_copy_storage_storage_static_dynamic.sol` | copying a uint[9] storage array into a uint[] storage array leaves the copy's length unknown instead of 9 |
| `copyStaticToLargerStatic` | `semanticTests/array/copying/array_copy_storage_storage_static_static.sol` | copying a uint[20] into a uint[40] storage array does not clear the tail; big[30] reads small[30] |
| `length2dStorageToStorage` | `smtCheckerTests/array_members/length_1d_assignment_2d_storage_to_storage.sol` | the pushed inner arrays start from an unknown length, so the two row lengths are unrelated |
| `memoryMultipleDynamic` | `semanticTests/array/create_multiple_dynamic_arrays.sol` | closes only with -m 100000 (about 25 s); the default step budget runs out before the last asserts |

### `open/SolcFunctionCallsOpen.sol`

| Function | Upstream (`test/libsolidity/`) | Why it stays open |
|---|---|---|
| `namedArgsUnordered` | `semanticTests/functionCall/named_args.sol` | named arguments are bound in call order, not by name, so the call computes 231 |
| `disorderedNamedArgs` | `semanticTests/functionCall/disordered_named_args.sol` | named arguments are bound in call order, not by name, so the call computes 312 |
| `freeFunctionRecursionBitAnd` | `semanticTests/freeFunctions/recursion.sol` | no rule for the bitwise `&` |
| `conditionalWithArguments` | `semanticTests/functionCall/conditional_with_arguments.sol` | a call whose callee is a conditional expression is read as `false ? g : h(2, 1)` and gets stuck |
| `functionsStorageVar1` | `smtCheckerTests/functions/functions_storage_var_1.sol` | no rule for an assignment used as a value, `a = (y = v)` |
| `functionsStorageVar2` | `smtCheckerTests/functions/functions_storage_var_2.sol` | no rule for an assignment used as a value, `a = (y = v)` |
| `functionsRecursiveUnbounded` | `smtCheckerTests/functions/functions_recursive.sol` | a uint state variable is not known to be non-negative, and the recursion unfolds without end |

### `open/SolcStructsMappingsOpen.sol`

| Function | Upstream (`test/libsolidity/`) | Why it stays open |
|---|---|---|
| `pushedStructIsZero` | `semanticTests/structs/struct_storage_push_zero_value.sol` | push() on a storage array of structs does not zero the appended element; every field of pushZero[0] stays symbolic |
| `copyStructArrayIntoNestedArray` | `semanticTests/structs/copy_struct_array_from_storage.sol` | va.push() and va[0].push() leave the appended inner array and struct symbolic (push does not zero non-primitive elements), so va[0].length == 3 is unprovable |
| `copyStructArraysIntoMemoryRows` | `semanticTests/structs/copy_struct_array_from_storage.sol` | temp[0] = va[0] (storage array copied into an element of a memory array of arrays) has no rule: symbolic execution stops on the statement |
| `storageNestedStructArrays` | `smtCheckerTests/types/struct/struct_array_struct_array_storage_safe.sol` | closes only with -m 300000 (~130 s); exceeds the suite budget of 50000 steps / 30 s |
| `storageArrayOfNestedStructArrays` | `smtCheckerTests/types/struct/array_struct_array_struct_storage_safe.sol` | closes only with -m 300000 (~340 s); exceeds the suite budget of 50000 steps / 30 s |

### `open/SolcConstructorsOpen.sol`

| Function | Upstream (`test/libsolidity/`) | Why it stays open |
|---|---|---|
| `constructor` | `semanticTests/constructor/state_variable_initialization.sol` | closes in KeY, but SolidityRuntimeCheck skips a contract with a constructor or an initialized state variable, so no EVM cross-check |
| `immutableUninitialized` | `semanticTests/immutable/uninitialized.sol` | an immutable is plain storage, so outside the constructor a never-assigned one is unconstrained instead of zero |
| `deleteLocal` | `semanticTests/variables/delete_local.sol` | no rule for delete on a local variable |
| `deleteLocals` | `semanticTests/variables/delete_locals.sol` | no rule for delete on a local variable |
| `c99ScopingShadowDefault` | `semanticTests/scoping/c99_scoping_activation.sol` | a local declared without initializer starts unconstrained instead of zero |
| `localDefaults` | `semanticTests/immutable/uninitialized.sol` | a local declared without initializer starts unconstrained instead of zero |
| `enumDefault` | `semanticTests/constants/constant_variables.sol` | a local declared without initializer starts unconstrained instead of zero |
| `msgValueZero` | `semanticTests/state/msg_value.sol` | a non-payable function does not assume msg.value == 0 |
| `multipleInitializationsAssignExpr` | `semanticTests/immutable/multiple_initializations.sol` | no rule for a compound assignment used as an expression |
| `mappingLocalCompoundAssignment` | `semanticTests/variables/mapping_local_compound_assignment.sol` | a parenthesized assignment as index base, (m = m2)[2] = 21, gets stuck |
| `baseConstructorArgumentsSelfMul` | `semanticTests/constructor/base_constructor_arguments.sol` | the right side of a compound assignment reads storage |

### `open/SolcPaymentsOpen.sol`

| Function | Upstream (`test/libsolidity/`) | Why it stays open |
|---|---|---|
| `msgSenderRange` | `smtCheckerTests/special/msg_sender_range.sol` | msgSender is an unbounded int with no range assumption, so msg.sender >= 0 is not provable |
| `msgValueArith` | `smtCheckerTests/special/msg_value_1.sol` | msgValue >= 0 is not assumed by a .sol obligation, so (5 + v + v) - (4 + v) > 0 is not provable |
| `msgValueRange` | `smtCheckerTests/special/range_check.sol` | msgValue >= 0 is not assumed by a .sol obligation |
| `msgValueNonPayable` | `smtCheckerTests/special/msg_value_3.sol` | a non-payable function's obligation does not assume msg.value == 0 |
| `nonPayableRequireHolds` | `smtCheckerTests/functions/payable_1.sol` | a non-payable function's obligation does not assume msg.value == 0, so the require may revert |
| `tryCatchExactReturn` | `semanticTests/tryCatch/simple_notuple.sol` | the callee of a try is never executed, so its return value 13 is unconstrained |
| `tryCatchPanicDefaults` | `semanticTests/tryCatch/panic.sol` | a local declared without an initializer is unconstrained rather than zero |
| `trySuccessRunsCallee` | `smtCheckerTests/try_catch/try_2.sol` | the callee of a try is never executed, so its write x = 42 is not seen on success |
| `trySuccessKeepsStorageUnsound` | `smtCheckerTests/try_catch/try_2.sol` | closes in KeY but fails on the EVM: under noCallback a successful try keeps the caller's storage, though the callee here is the contract itself and sets ix = 42 |
| `tryMultipleCatchClausesSuccess` | `smtCheckerTests/try_catch/try_multiple_catch_clauses.sol` | the callee of a try is never executed, so its write x = 42 is not seen on success |
| `tryNestedGetter` | `smtCheckerTests/try_catch/try_nested_1.sol` | the callee of a try is never executed, so the getter's result is not linked to ix |
| `tryArgumentSideEffect` | `smtCheckerTests/try_catch/try_3.sol` | no rule captures a try call argument with a side effect, so symbolic execution stops at the try |
| `callWithoutCodeReverts` | `semanticTests/revertStrings/called_contract_has_code.sol` | an external call outside try has no rule, so symbolic execution stops at it |
| `callRunsReceive` | `semanticTests/receive/empty_calldata_calls_receive.sol` | under noCallback a call keeps the caller's storage, so the receive increment is not seen |
| `callKeepsStorageUnsound` | `semanticTests/receive/empty_calldata_calls_receive.sol` | closes in KeY but fails on the EVM: under noCallback a call keeps the caller's storage, though the empty-calldata call to the contract itself runs receive |

### `open/SolcModifiersOpen.sol`

| Function | Upstream (`test/libsolidity/`) | Why it stays open |
|---|---|---|
| `modifierMultipleTimesLocalVars` | `semanticTests/modifiers/function_modifier_multiple_times_local_vars.sol` | nested applications of one modifier share its local b, so the outer assert(b == y) reads the inner b |
| `modifierArgumentEvaluationOrder` | `semanticTests/modifiers/evaluation_order.sol` | a function call as a modifier argument stays stuck as an unresolved fn#id(...) declaration |
| `modifierArgumentAssignsReturn` | `semanticTests/modifiers/function_modifier_return_reference.sol` | an assignment expression used as a modifier argument has no rule |
| `breakInModifier` | `semanticTests/modifiers/break_in_modifier.sol` | a for loop in a modifier body is not lowered, so no loop rule matches it |
| `continueInModifier` | `semanticTests/modifiers/continue_in_modifier.sol` | a for loop in a modifier body is not lowered, so no loop rule matches it |
| `modifierLoop` | `semanticTests/modifiers/function_modifier_loop.sol` | a for loop in a modifier body is not lowered, so no loop rule matches it |
| `returnInModifier` | `semanticTests/modifiers/return_in_modifier.sol` | a modifier containing return is not inlined |
| `stackedReturnWithModifiers` | `semanticTests/modifiers/stacked_return_with_modifiers.sol` | a modifier containing return is not inlined |
| `multiInvocationOnce` | `semanticTests/modifiers/function_modifier_multi_invocation.sol` | a modifier with two placeholders is not inlined |
| `multiInvocationTwice` | `semanticTests/modifiers/function_modifier_multi_invocation.sol` | a modifier with two placeholders is not inlined |
| `multiWithReturnTwice` | `semanticTests/modifiers/function_modifier_multi_with_return.sol` | a modifier with two placeholders is not inlined |
| `smtModifierReturn` | `smtCheckerTests/modifiers/modifier_return.sol` | a modifier containing return is not inlined |
| `smtTwoPlaceholders` | `smtCheckerTests/modifiers/modifier_two_placeholders.sol` | a modifier with two placeholders is not inlined |

### `open/SolcSmtControlFlowOpen.sol`

| Function | Upstream (`test/libsolidity/`) | Why it stays open |
|---|---|---|
| `functionCallInsideBranch4` | `smtCheckerTests/control_flow/function_call_inside_branch_4.sol` | `address a;` in hAddr starts unconstrained instead of address(0) |
| `sideEffectsInsideIf1` | `smtCheckerTests/control_flow/side_effects_inside_if_1.sol` | an uninitialised `uint x;` starts unconstrained instead of 0 |
| `sideEffectsInsideTernary1` | `smtCheckerTests/control_flow/side_effects_inside_ternary_1.sol` | an uninitialised `uint x;` starts unconstrained instead of 0 |
| `loopBasic` | `smtCheckerTests/invariants/loop_basic.sol` | an uninitialised `uint y;` starts unconstrained instead of 0, so the invariant fails on entry |
| `loopNested` | `smtCheckerTests/invariants/loop_nested.sol` | an uninitialised `uint y;` starts unconstrained instead of 0, so the trip count is unknown |
| `loopNestedFor` | `smtCheckerTests/invariants/loop_nested_for.sol` | an uninitialised `uint y;` starts unconstrained instead of 0, so the trip count is unknown |
| `shortCircuitAndTouched` | `smtCheckerTests/control_flow/short_circuit_and_touched.sol` | an assignment used as an operand, `b = (flag = false)`, is stuck on the program text |
| `shortCircuitOrTouched` | `smtCheckerTests/control_flow/short_circuit_or_touched.sol` | an assignment used as an operand, `b = (flag = true)`, is stuck on the program text |
| `bmcRequireAlwaysTrue` | `smtCheckerTests/bmc_coverage/assert.sol` | a uint parameter is not known to be non-negative, so the diamond leaves leq(x, -1) open |
| `msgValueZeroWhenNonPayable` | `smtCheckerTests/bmc_coverage/msg_value_4.sol` | a non-payable function does not fix msg.value to 0 |
| `msgValueNonNegative` | `smtCheckerTests/bmc_coverage/range_check.sol` | msg.value is not known to be non-negative |
| `branchesInModifiers` | `smtCheckerTests/control_flow/branches_with_return/branches_in_modifiers.sol` | a modifier containing `return` is not inlined, so the obligation stays open |
| `branchesInModifiers2` | `smtCheckerTests/control_flow/branches_with_return/branches_in_modifiers_2.sol` | a modifier containing `return` is not inlined, so the obligation stays open |

### `open/SolcArithmeticOpen.sol`

| Function | Upstream (`test/libsolidity/`) | Why it stays open |
|---|---|---|
| `modSignFollowsDividend` | `smtCheckerTests/operators/mod_signed.sol` | true-fact-unprovable: smod(42, y) has no sign lemma, goal leq(smod(42, y), -1) ==> y = 0 |
| `modBelowDivisor` | `smtCheckerTests/operators/mod_n.sol` | true-fact-unprovable: smod(x, y) < y is not derived, goal geq(smod(x, y), y), geq(y, 1) ==> |
| `modOfDoubleIsZero` | `smtCheckerTests/operators/mod_even.sol` | true-fact-unprovable: goal leq(x, 9999) ==> smod(mul(x, 2), 2) = 0 |
| `compoundSubSelfReference` | `smtCheckerTests/operators/compound_sub.sol` | true-fact-unprovable: a uint parameter carries no 0 <= x fact, goal leq(x, -1) ==> |
| `compoundSubMapping` | `smtCheckerTests/operators/compound_sub_mapping.sol` | true-fact-unprovable: a uint parameter carries no 0 <= x fact, goal leq(x, -1) ==> |
| `compoundSubArrayIndex` | `smtCheckerTests/operators/compound_sub_array_index.sol` | true-fact-unprovable: a uint parameter carries no 0 <= x fact, goal leq(x, -1) ==> |
| `compoundAddChain` | `smtCheckerTests/operators/compound_add_chain.sol` | unsupported-construct: a compound assignment used as a value (u = (b += c);) has no rule |
| `unaryAddOnPush` | `smtCheckerTests/operators/unary_add_array_push_1.sol` | unsupported-construct: ++pushed.push() has no rule |
| `checkedModifierCalledByUnchecked` | `semanticTests/arithmetics/checked_modifier_called_by_unchecked.sol` | unsupported-construct: an expression statement (a + b;) in the modifier has no rule |
| `uncheckedCalledByCheckedWraps` | `semanticTests/arithmetics/unchecked_called_by_checked.sol` | design-limitation: unbounded integers, uint16 0xffff + 0x100 does not wrap to 0xff |
| `checkedCalledByUncheckedWraps` | `semanticTests/arithmetics/checked_called_by_unchecked.sol` | design-limitation: unbounded integers, the unchecked uint16 sum 0x10000 does not wrap to 0 |
| `blockInsideUnchecked` | `semanticTests/arithmetics/block_inside_unchecked.sol` | design-limitation: unbounded integers, 2**256 - 1 + 1 does not wrap to 0 |
| `uncheckedIncrementWraps` | `smtCheckerTests/unchecked/inc_dec.sol` | design-limitation: unbounded integers, ++x on 2**256 - 1 does not wrap to 0 |
| `uncheckedUint8SubWraps` | `smtCheckerTests/overflow/underflow_sub.sol` | design-limitation: unbounded integers, uint8 0 - 1 does not wrap to 255 |
| `uncheckedInt8SumWraps` | `smtCheckerTests/overflow/overflow_sum_signed.sol` | design-limitation: unbounded integers, int8 127 + 1 does not wrap to -128 |
| `uncheckedInt8MulWraps` | `smtCheckerTests/overflow/overflow_mul_signed.sol` | design-limitation: unbounded integers, int8 100 * 2 does not wrap to -56 |
| `shiftLeftParam` | `semanticTests/operators/shifts/shift_left.sol` | unsupported-construct: << has no rule, stuck on r = a_0 << b_0; |
| `shiftRightParam` | `semanticTests/operators/shifts/shift_right.sol` | unsupported-construct: >> has no rule, stuck on r = a_0 >> b_0; |
| `shiftRightAssign` | `semanticTests/operators/shifts/shift_constant_right_assignment.sol` | unsupported-construct: >>= has no rule, stuck on a >>= 8; |
| `shiftRightNegativeLiteral` | `semanticTests/operators/shifts/shift_right_negative_literal.sol` | unsupported-construct: >> has no rule, stuck on r = -4266 >> 1; |

### `open/SolcHigherLevelOpen.sol`

| Function | Upstream (`test/libsolidity/`) | Why it stays open |
|---|---|---|
| `internalVirtualFunctionCalls` | `semanticTests/virtualFunctions/internal_virtual_function_calls.sol` | a call from a base function binds the base's own g() statically, so the result is 1, not the override's 2 |
| `virtualFunctionCallViaBase` | `semanticTests/virtualFunctions/virtual_function_calls.sol` | a call from a base function binds the base's own g() statically, so the result is 1, not the override's 2 |
| `explicitBaseClass` | `semanticTests/inheritance/explicit_base_class.sol` | a base-qualified call BaseBase.g() is lowered to address.g() and has no rule |
| `inheritedFunction` | `semanticTests/inheritance/inherited_function.sol` | a base-qualified call A.f() is lowered to address.f() and has no rule |
| `inheritedNamedBaseOrdered` | `semanticTests/inheritance/inherited_function_named_parameters.sol` | a base-qualified call A.f(...) is lowered to address.f(...) and has no rule |
| `inheritedNamedOverrideUnordered` | `semanticTests/inheritance/inherited_function_named_parameters.sol` | named arguments are bound in call order, not by name, so the call computes 17 |
| `overriddenFunctionStaticCallParent` | `smtCheckerTests/inheritance/overridden_function_static_call_parent.sol` | a base-qualified call BaseBase.init(c, d) is lowered to address.init(c, d) and has no rule |

### `open/SolcTypesOpen.sol`

| Function | Upstream (`test/libsolidity/`) | Why it stays open |
|---|---|---|
| `constructingEnumsFromInts` | `semanticTests/enums/constructing_enums_from_ints.sol` | no rule executes an integer-to-enum conversion E(x); the proof stops on it |
| `enumExplicitConversionInRange` | `semanticTests/enums/enum_explicit_overflow.sol` | no rule executes an integer-to-enum conversion E(x); the proof stops on it |
| `enumFromUint` | `smtCheckerTests/typecast/enum_from_uint.sol` | no rule executes an integer-to-enum conversion E(x); the proof stops on it |
| `enumRange` | `smtCheckerTests/types/enum_range.sol` | an enum parameter is an unconstrained int, its member range is not assumed |
| `enumToUintMaxValue` | `smtCheckerTests/typecast/enum_to_uint_max_value.sol` | an enum parameter is an unconstrained int, its member range is not assumed |
| `storageValueVarsNonNegative` | `smtCheckerTests/types/storage_value_vars_3.sol` | a uint state variable read from storage is not known to be non-negative |
| `castLargerKeepsRange` | `smtCheckerTests/typecast/cast_larger_1.sol` | a uint8 parameter is an unbounded int, its 0..255 range is not assumed |
| `castSmallerBoundsRange` | `smtCheckerTests/typecast/cast_smaller_1.sol` | the narrowing cast uint8(x) is the identity, so y is not bounded by 255 |
| `castSmallerTruncates` | `smtCheckerTests/typecast/cast_smaller_2.sol` | the narrowing cast uint16(a) is the identity instead of truncating |
| `downcastTruncates` | `smtCheckerTests/typecast/downcast.sol` | narrowing and sign-changing casts are the identity instead of wrapping |
| `sameSizeReinterprets` | `smtCheckerTests/typecast/same_size.sol` | a same-width signed/unsigned cast is the identity instead of reinterpreting the bits |
| `upcastAfterTruncation` | `smtCheckerTests/typecast/upcast.sol` | int8(int(255)) is the identity instead of wrapping to -1 |
| `numberLiteralWraps` | `smtCheckerTests/typecast/number_literal.sol` | uint(int(-1)) is the identity instead of wrapping to 2**256-1 |
| `typeConversionCleanup` | `semanticTests/types/type_conversion_cleanup.sol` | uint128(x) is the identity instead of truncating to the low 128 bits |
| `packingSignedTypes` | `semanticTests/types/packing_signed_types.sol` | int8(uint8 0xfa) is the identity instead of reinterpreting to -6 |
| `nestedTuplesParenthesized` | `semanticTests/types/nested_tuples.sol` | a parenthesized tuple target ((a, b)) = (...) has no rule |
| `ternaryLiteralOverflow` | `semanticTests/literals/ternary_operator_with_literal_types_overflow.sol` | closes in KeY, but the uint8 addition 63 + 255 panics with 0x11 on the EVM |

## Gaps the first port found

Every example closes. Seven were red when first written; the port is what found them, and they
stay in the suite as regression tests.

| Function | Was | Fix |
|---|---|---|
| `SolcArrays.pushedSlotIsZeroed` | `storagePushLengthSave` bumped `size` without touching the new slot, so a freshly pushed slot stayed symbolic rather than zero | `push` now clears the appended slot with the same lazy `delAt` marker `pop` uses, which resolves by sort: a primitive element becomes `defaultValue`, a struct becomes a `delNode` that preserves mapping members (`TestSuite.testDeepPopDoesNotResetMappingMember`) |
| `SolcExpressions.bareIncrementOnLocal` | `i++;` as a statement had rules for storage paths but not for a local | added `local{Pre,Post}{in,de}crement` |
| `SolcExpressions.compoundAssignOnLocal` | `r += e;` existed for storage paths only, and a non-simple RHS had no capture at any location | added `local{Add,Sub,Mul,Div,Mod}Assign` and the location-neutral `*AssignValueRhsCapture` family |
| `SolcArrays.storageArrayAliasWritesThrough` | the array index rules split on `global` vs `complex`, so a `simple`+`local` alias root matched neither | dropped `global` from `storageIndex{Read,Write}Array*_root`, matching their `BindLocalRoot`/`StoreRoot`/`CopySource` siblings |
| `SolcStructs.structArrayElementCopy` | `TermCreationException`: the index-write *save* rules took an unrestricted `SimpleExpression`, so a storage-alias variable matched and `save(…, path)` was ill-sorted | restricted the value to `SimpleExpression[primitive]` so the copy routes to `…CopySource`, and made that rule carry the value sort-free (`find<[StValue]>`) |
| `SolcMappings.arrayElementsToMapping` | same, plus `storageIndexWriteMappingCopySource` hard-coding `find<[Struct]>` | same generic-sort treatment |
| `SolcControlFlow.ternarySelectsFirstMemorySource` | `SolJSONParser.parseConditional` typed every `?:` as `bool`, so a reference-typed ternary was captured into a `bool` temp and got stuck | take the type from the branches, as `SolidityToKeyConverter` already did |

### Cost of `push`/`pop`

`pushThenPopRestoresLength` was what exposed this: writing the cleared slot as a `save` of the
deleted value at that path mentions the storage **twice**, so the term doubled on every `push`
or `pop`. Six operations built a term whose deleted-value markers held 93% of the sequent, and the
proof cost ~89× one operation for only ~9× the rule applications — the extra time was spent
rewriting ever-larger terms, not taking more steps.

`delAt(st, p)` (`structRules.key`) denotes the same storage while mentioning `st` once, so the
term now grows linearly. The reset value is still chosen on read, through the
`delField<[alpha]>` dispatch — struct elements keep their `delNode`, so mapping members survive a
`pop` (`TestSuite.testDeepPopDoesNotResetMappingMember`).

| Function | ops | Peak term before | after | Time before | after |
|---|---|---|---|---|---|
| `pushEmptyGrowsLength` | 1 | 118 | 88 | 41 ms | 71 ms |
| `lengthTracksPushAndPop` | 2 | 437 | 284 | 277 ms | 239 ms |
| `popThenPushSlotIsZeroed` | 3 | 1031 | 353 | 664 ms | 483 ms |
| `pushThenPopRestoresLength` | 6 | 6183 | **589** | 4081 ms | **1120 ms** |

The one-operation timings are in the noise; the point is the slope. Peak term size at six
operations fell 10×, and the duplicated storage copies are gone entirely.

## Not ported

Whole upstream families that fall outside the fragment, listed so the omission is deliberate
rather than an oversight. `docs/limitations.md` ranks them by how many tests each blocks.

- **Width, cleanup and wrapping** (`cleanup/`, most of `integer/`, `array/copying/*_uint40|uint128|packed`,
  checked-overflow reverts): the calculus uses unbounded integers. The value-preserving cases are
  in `SolcTypes`/`SolcArithmetic`, the rest in `open/`.
- **Bitwise and shifts**, `bytes`, `bytesN`, `string`, `keccak256`, `abi.*`
  (`expressions/bit_operators.sol`, `strings/`, `abicoder/`, `crypto/`, `abi/`): no LDT.
- **Reverting behaviour as the expectation** (`array/pop/array_pop_empty_exception.sol`,
  `array/dynamic_out_of_bounds_array_access.sol`, checked-revert tests): "this always reverts"
  is ported only where a box function can end in `assert(false)` after the reverting statement
  (`SolcPayments`).
- **Constructs that do not load or get stuck**: `events/`, custom errors, `libraries/`,
  `using/`, `userDefinedValueType/`, `functionTypes/`, `inlineAssembly/`, struct literals,
  `type(T)`, `addmod`/`mulmod`, `this`/`block`/`tx`, `super`, file-level declarations, contract
  creation (`deployment/`), external calls outside `try`.
- **Tests without an observable claim**: expectations on raw calldata or return data, SMTChecker
  files whose only `assert` is refuted and whose negation is not always true, and loops that
  never terminate for the pinned inputs (the EVM run would run out of gas).

Per-theme counts of closed, open and not-ported tests are in `docs/limitations.md`, section (e).
