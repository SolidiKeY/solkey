# Pre-licenciate-paper Taclet Starters

This directory contains the runnable taclet subset inspired by the
Pre-licenciate-paper storage rules. `storageRules.key` is the authoritative
rule file; the other `.key` files in this directory are examples that include
it, declare only example variables, and define closing problems.

## Implemented Here

- `storageRules.key` collects the currently runnable subset of the
  Pre-licenciate-paper storage taclets. It is the only rule-bearing `.key` file
  in this directory. The split follows the `solidity-key-taclets` skill
  guidance: root storage rules use `SimpleStoragePath`, field/index paths use
  `Path[...]` plus `\sameAsTerm`, and complex member/index cases stay as
  structural `.key` source patterns.
- `storageRulesExamples.key` contains small closing examples for the
  consolidated rules. It covers final effects for root copy/read, local field
  write, structural member-field decomposition, and the nested `matrix[2][3]`
  path.
- `storage-root-read-write.key` demonstrates `storageRootWriteStore` and
  `storageRootReadSelect` for contract storage fields using `\sameAsTerm`.
- `storage-root-copy-source.key` demonstrates `storageRootWriteCopySource` for
  copying one contract storage field into another.
- `storage-root-paths.key` demonstrates the same root-level storage paper
  rules using the path-aware `SimpleStoragePath` program schema-variable sort.
- `storage-field-path-read-write.key` demonstrates the first path-lowering
  bridge for source-level member paths such as `st.age`, using `\sameAsTerm` to
  bind the program path to the `List` consumed by `save` and `find`.
- `storage-field-decomposition.key` demonstrates pattern-based path
  decomposition for a literal terminal field: rules with source patterns such
  as `s#a.balance` match `alice.account.balance`, bind `a` to
  `alice.account`, and append the literal `balance` segment in the replacement.
  This implements the first complex-path step directly in `.key` patterns,
  without a Java normalization meta-construct or generated alias block.
- `storage-index-decomposition.key` demonstrates the indexed counterpart:
  rules with source patterns such as `s#a[s#i]` match nested indexed paths like
  `matrix[2][3]`, bind `a` to `matrix[2]` and `i` to `3`, and append
  `at(i)` as the `Field`-sorted index segment in the replacement.
- `commonFields.key` provides legacy shared field constants, program variables,
  and identities for ad hoc experiments. Runnable examples in this directory do
  not include it.
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
  `ledger.balances[k]` lower to `List` terms for `save` and `find`. Indexed
  segments are represented as `at(index)` so they have sort `Field` and can be
  unfolded by the structural storage taclets.

Run these through the focused JUnit test added in `keyext.solidity.core`, or
load an individual file with the Solidity CLI from the repository root.

## Work Needed For The Full Paper Rule Set

The remaining paper taclets should be added as focused `.key` patterns only
when the matcher can preserve the relevant Solidity path shape faithfully.

1. Continue direct complex-path pattern support. Implemented for member-field
   and indexed-path decomposition.

   Member-field decomposition is implemented for patterns such as
   `s#a.field`; indexed-path decomposition is implemented for patterns such as
   `s#a[s#i]`. Further work in this direction should keep field/index base
   unfolding in structural `.key` patterns where possible.

2. Preserve storage vs. memory delete semantics.

   The paper/Lean model distinguishes storage delete targets from memory delete
   targets, and distinguishes simple from complex targets. Do not collapse this
   into a single `delete path` rule: examples such as `delete carol.age` depend
   on whether `carol` is a memory object, storage root, storage alias, or a
   complex computed path.

3. Add Java/logic support for the paper rules that need generated aliases or
   heap helpers.

   The runnable `.key` subset does not yet cover declaration split/skip,
   alias-introducing unfold rules, array bounds/revert branches, push/pop
   length updates, delete, compound-update desugaring, memory heap read/write
   allocation rules, or storage-memory copy rules using `copySt`/`copyMem`-style
   helpers.

## Suggested Next Examples After Java Support

- `storage-delete.key`: `storageDeleteSimpleTarget` and
  `storageDeleteComplexTarget`.
- `storage-field-read-write.key`: broader `storageFieldWriteSave` and
  `storageFieldReadFind` variants for additional source-level member paths.
- `memory-delete.key`: `memoryDeleteSimpleTarget` and
  `memoryDeleteComplexTarget`.
- `storage-index-read-write.key`: broader indexed array/mapping read and write
  rules beyond the starter decomposition pattern.
- `memory-field-index.key`: memory field/index write, read, and alias-root
  cases.
