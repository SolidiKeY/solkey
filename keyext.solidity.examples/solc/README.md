# solc semantic-test ports

Ports of the Solidity compiler's own semantic tests
([`ethereum/solidity`](https://github.com/ethereum/solidity), `test/libsolidity/semanticTests/`)
into SolKey's proof-obligation style. Upstream those files are small contracts with an expected
return value written by the language implementers to pin down a corner case; here each one
becomes a function whose specification lives in the body as `assert`.

The point is a cross-check against a description of Solidity semantics that SolKey did not
write. `TestSuite.sol` exercises one taclet each; these exercise what the language says.

Six contracts, one per upstream theme:

| File | Upstream |
|---|---|
| `SolcExpressions.sol` | `expressions/`, `exponentiation/` |
| `SolcStructs.sol` | `structs/` |
| `SolcArrays.sol` | `array/`, `array/push`, `array/pop`, `array/delete` |
| `SolcMemory.sol` | `structs/memory_*`, `array/copying`, `array/delete` |
| `SolcMappings.sol` | `structs/*mapping*`, `array/copying/array_elements_to_mapping.sol` |
| `SolcControlFlow.sol` | `statements/`, `various/`, `expressions/conditional_expression_storage_memory_*` |

## Running

```bash
./run-key.sh keyext.solidity.examples/solc/SolcArrays.sol                    # every function
./run-key.sh keyext.solidity.examples/solc/SolcArrays.sol pushWithArgument   # one function
./gradlew :keyext.solidity.core:test --tests "*SolcSemanticsExamplesTest"
```

`SolcSemanticsExamplesTest` enumerates the directory and derives the contract name from the file
name, so a new example joins the suite by being written. Type-check first — it costs ~150 ms and
catches a Solidity error before a multi-minute proof run:

```bash
solc --ast-compact-json keyext.solidity.examples/solc/SolcArrays.sol > /dev/null
```

## Adaptation rules

Applied uniformly; every function names the upstream file it came from.

- `returns (T r) { … return e; }` becomes a `void` function ending in `assert` over a bound
  local — `return e;` has no consuming rule, and the obligation generator only accepts
  functions that return nothing.
- The upstream `// f() -> 42` expectation becomes an in-body `assert`.
- `constructor` bodies and state-variable initializers become plain assignments at the top of
  the body.
- Loops are unrolled to a fixed small trip count (there is no loop rule). What these tests pin
  down — what `push` appends, which element `delete` resets, which branch runs — does not
  depend on the loop construct.
- Tuple assignment becomes the sequential assignments it desugars to.
- `uint8`/`uint16`/`uint32`/`bytesN` become `uint`, with values kept far from any bound. The
  calculus uses unbounded integers, so the upstream width-truncation and cleanup tests are out
  of scope rather than mistranslated (see "Not ported" below).
- `address` becomes a `uint` id — address equality has no discharging rule.
- Fixed-size arrays (`S[5] data`) become dynamic arrays with their length assumed.
- A `bool` parameter becomes a `uint` parameter with the flag derived in the body: an obligation
  is only generated when every parameter is integer-typed.
- Symbolic execution starts from unconstrained storage, so wherever upstream relies on storage
  being zero-initialized the premise is stated with `require` and the function is tagged
  `/// @custom:key box`.

## Deviations forced by the loader or calculus

Beyond the uniform rules above, three rewrites were needed to get a file to load or a proof to
close. They are noted in place at the affected function.

- **Self-recursive struct types.** Upstream `struct_reference.sol` and `structs.sol` declare
  `struct s2 { …; mapping(k => s2) recursive; }`. `SolJSONParser` throws an NPE building a
  struct type that refers to itself (`getOrCreateMappingKeYSolidityType`), and that kills the
  whole file at load. The hierarchy is unrolled into `Depth0`/`Depth1`/`Depth2`, the two levels
  the tests actually walk.
- **Mapping members must be aliased before being indexed.** `nested.recursive[4].z` leaves an
  open goal; binding `mapping(uint => Depth1) storage map = nested.recursive;` first and using
  `map[4].z` closes. At depth two each member mapping needs its own alias. Upstream
  `struct_reference.sol` happens to be written that way already.
- **A `push` argument that reads storage must be bound first**, per the general convention in
  `../README.md`.

## Known failures

These are in the directory and in CI **on purpose**: each states upstream semantics the calculus
cannot discharge yet, so the gap stays visible instead of being quietly dropped. Each function
carries a `KNOWN FAILURE` comment, and each gap has a backlog entry in `docs/taclet-ideas.md`.

| Function | Upstream | Missing |
|---|---|---|
| `SolcExpressions.bareIncrementOnLocal` | `expressions/inc_dec_operators.sol` | `++`/`--` as a statement of its own has rules at storage root/field/index level but not for a local. The result forms (`uint p = a++;`) are covered, and `bareIncrementOnStateVariable` beside it closes. |
| `SolcExpressions.compoundAssignOnLocal` | `expressions/inc_dec_operators.sol` | `+=` and friends exist at storage root/field/index level only; `r += e;` on a local has no rule. `r = r + e;` closes. |
| `SolcArrays.pushedSlotIsZeroed` | `array/array_storage_index_zeroed_test.sol` | No axiom ties a slot beyond `length` to its default, so a freshly pushed slot is symbolic rather than zero. Only the pop-then-push form (`popThenPushSlotIsZeroed`) is observable today. |
| `SolcArrays.storageArrayAliasWritesThrough` | `array/storage_array_ref.sol` | An alias of an array with *primitive* elements cannot be indexed — `ref[1]` is open for reads and writes, even with the bound stated on the alias. `ref.length` works, and the struct-element twin `storageStructArrayAliasWritesThrough` closes. |
| `SolcStructs.structArrayElementCopy` | `array/copying/array_copy_storage_storage_struct.sol` | A whole-struct copy into an *array* element raises a `TermCreationException`; the same copy into a *mapping* element works. Member-wise workaround: `structArrayElementCopyMemberwise`. |
| `SolcMappings.arrayElementsToMapping` | `array/copying/array_elements_to_mapping.sol` | A whole-array copy into a mapping entry raises the same `TermCreationException`. Element-wise workaround: `arrayElementsToMappingElementwise`. |
| `SolcControlFlow.ternarySelectsFirstMemorySource` | `expressions/conditional_expression_storage_memory_1.sol` | A `?:` over two memory references assigned into a *storage* target is open. The memory-target form (`ternaryIntoMemoryTarget`) and the `if`/`else` form (`ifElseSelectsMemorySource`) both close, so the gap is the storage-target copy after the split. |

## Not ported

Whole upstream families that fall outside the fragment, listed so the omission is deliberate
rather than an oversight:

- **Width and cleanup** (`cleanup/`, `integer/`, most of `array/copying/*_uint40|uint128|packed`,
  `bytes*`): the calculus uses unbounded integers, so truncation, wrapping and storage packing
  have nothing to check against.
- **Bitwise and `bytesN`** (`expressions/bit_operators.sol`, `array/*bytes*`): no bitwise LDT.
- **Reverting behaviour** (`reverts/`, `array/pop/array_pop_empty_exception.sol`,
  `array/dynamic_out_of_bounds_array_access.sol`): "this always reverts" has no `assert` form
  (see `../README.md`, "Known gaps").
- **Loops as such** (`statements/`, `array/array_storage_push_pop.sol` at large lengths): the
  ports unroll, so the loop rule itself is untested.
- **Everything above the fragment**: `inheritance/`, `libraries/`, `events/`, `modifiers/`,
  `functionTypes/`, `tryCatch/`, `inlineAssembly/`, `abicoder/`, `constructor/`, `getters/`,
  external calls, casts, struct literals, tuples, `return e;`.
