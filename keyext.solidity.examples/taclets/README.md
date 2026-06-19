# Pre-licenciate-paper Taclet Starters

This directory contains small, runnable taclet examples inspired by the
Pre-licenciate-paper storage rules. They are intentionally narrower than the
paper rules: each example uses a concrete path shape that the current Java
parser, program schema-variable sorts, and logic bridge can already handle.

## Implemented Here

- `storage-root-read-write.key` demonstrates `storageRootWriteStore` and
  `storageRootReadSelect` for contract storage fields using `FieldReference`
  plus `\sameAsTerm`.
- `storage-root-copy-source.key` demonstrates `storageRootWriteCopySource` for
  copying one contract storage field into another.
- `storage-root-paths.key` demonstrates the same root-level storage paper
  rules using the path-aware `SimpleStoragePath` program schema-variable sort.
- `storage-field-path-read-write.key` demonstrates the first path-lowering
  bridge for source-level member paths such as `st.age`, using `\sameAsTerm` to
  bind the program path to the `List` consumed by `save` and `find`.
- `commonFields.key` provides shared field constants, program variables, and
  default-value simplification rules for the examples.
- Solidity `delete target` now parses to `UnaryExpression(Operator.DELETE,
  target)` and prints as `delete target`.
- Path-aware program schema-variable sorts are available. The fixed names
  `StoragePath`, `SimpleStoragePath`, `ComplexStoragePath`, `MemoryPath`,
  `SimpleMemoryPath`, and `ComplexMemoryPath` cover common cases. More precise
  filters use `Path[name=...]`, with dotted flags such as `storage`, `memory`,
  `simple`, `complex`, `root`, `field`, `index`, `array`, `mapping`,
  `primitive`, `reference`, `local`, and `global`. Roots are simple; member
  and indexed paths such as `alice.account` are complex. Storage paths may be
  `local` or `global`; memory paths are always local. Field/index paths lower
  to the `List` sort consumed by `save` and `find` in `structRules.key`.
- `\sameAsTerm` lowers full program paths to logic path terms. Root contract
  fields still lower to `Field` constants for `storeSt` and `selectSt`; nested
  member and indexed paths such as `alice.account.balance`, `tokens[i]`, and
  `ledger.balances[k]` lower to `List` terms for `save` and `find`.

Run these through the focused JUnit test added in `keyext.solidity.core`, or
load an individual file with the Solidity CLI from the repository root.

## Java Work Needed For The Full Paper Rule Set

The remaining paper taclets should not be added as broad `.key` patterns until
the Java side can normalize and preserve Solidity paths faithfully.

1. Add normalization meta-constructs for complex paths.

   Rules such as `storageDeleteComplexTarget`, index capture, field/index base
   unfolding, and storage/memory copy rules synthesize fresh aliases and replace
   one statement with multiple statements. Implement Java helpers similar in
   spirit to `ExpandFunctionBody`, so taclets can create blocks like
   `T storage sp = nsp; delete sp;` without hard-coding every source shape in
   `.key`.

2. Preserve storage vs. memory delete semantics.

   The paper/Lean model distinguishes storage delete targets from memory delete
   targets, and distinguishes simple from complex targets. Do not collapse this
   into a single `delete path` rule: examples such as `delete carol.age` depend
   on whether `carol` is a memory object, storage root, storage alias, or a
   complex computed path.

## Suggested Next Examples After Java Support

- `storage-delete.key`: `storageDeleteSimpleTarget` and
  `storageDeleteComplexTarget`.
- `storage-field-read-write.key`: `storageFieldWriteSave` and
  `storageFieldReadFind` for source-level member paths such as
  `alice.account.balance`.
- `memory-delete.key`: `memoryDeleteSimpleTarget` and
  `memoryDeleteComplexTarget`.
- `storage-index-read-write.key`: indexed array/mapping read and write rules.
- `memory-field-index.key`: memory field/index write, read, and alias-root
  cases.
