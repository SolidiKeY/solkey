# Solidity Taclet Examples

These examples exercise source-level Solidity program taclets loaded through
`standardSolidityRules.key`.

## Layout

The program of each example lives in `../TestSuite.sol`, the single contract shared with
`../mainFeatures/`. A `.key` file supplies only the pre/postcondition and calls one function:

```
\programSource "../TestSuite.sol";
\programVariables { int result; }
\problem { \<{ result = storageFieldWriteRead()@TestSuite; }\>(result = 34) }
```

so every test program is real Solidity, type-checked by `solc` on load. Check the contract
with `solc --ast-compact-json ../TestSuite.sol > /dev/null` before running a suite.

Conventions:

- The function name is the `.key` basename in lowerCamelCase
  (`storage-field-write-read.key` → `storageFieldWriteRead`).
- One observed value → a named return, assigned by plain assignment. `return e;` is not
  supported by the calculus; see `docs/taclets-implementation.md`.
- Several observed values → in-body `assert`s and postcondition `true`
  (`memory-delete`, `memory-assign-forms`, `storage-alias-rebind-original`).
- Observing only storage/memory → no return value at all.
- A problem with two modalities calls two functions with `Write`/`Read` suffixes
  (`storage-field-global-age`, `storage-index-root-array`, …), unless both conjuncts run the
  *same* program, in which case they share one function (`storageFieldDisjointFields`,
  `storageFieldDisjointRoots`, `storageAliasWrite`).

**Still inline.** The seven `net-*` examples keep their program inside the modality:
`msg.value` needs a `payable` function and `.transfer` needs `address payable`, and
`SolJSONParser` supports neither `msg` nor `.transfer` (`SolidityToKeyConverter` does).
`net-manual-update` has no program at all. See `docs/net.md`.

## Memory

- `memory-decl-fresh.key` and `memory-root-delete-fresh.key` cover fresh memory
  root allocation and rebinding.
- `memory-decl-default.key`, `memory-root-alias.key`,
  `memory-field-alias.key`, `memory-deep-field.key`, and
  `memory-field-reference-assign.key` cover identity-backed memory aliases,
  field reads, field writes, and complex receiver unfolding.
- `memory-delete.key` covers primitive delete and reference-field freshening.
- `memory-array-index.key` covers fixed-length memory array allocation and
  bounded index read/write.
- `memory-struct-array-index.key` covers index read/write through a *complex*
  memory receiver (`basket.items[1]`), exercising
  `memoryIndexWrite_unfold_leftFst` / `memoryIndexRead_unfold_rightFst`.
- `memory-assign-forms.key` covers the assignment forms directly (no
  declaration-with-initializer): `carol = alice;` (`memoryStorageCopy`) and
  `xs = new uint[](4);` (`memoryArrayFreshAlloc`). Declarations with an
  initializer are first decomposed by `memoryLocalDeclInitDrop`
  (`T memory m = x;` ⇝ `m = x;`).
- `storage-to-memory.key` and `memory-to-storage.key` cover lazy `copySt` and
  `copyMem` transfers.

## Payments (net ledger, docs/net.md)

- `net-manual-update.key` exercises the `net` program variable with a manual
  `storeSt`/`selectSt` update — no program rules involved.
- `net-msg-value.key` covers the `msg.sender` / `msg.value` desugaring to the
  built-in `msgSender` / `msgValue` program variables.
- `net-transfer-simple.key` covers the `transferNoCallback` terminal rule
  (`to.transfer(5);` books `net(to) -= 5`).
- `net-transfer-capture-argument.key` and `net-transfer-capture-receiver.key`
  cover the nonsimple-amount and nonsimple-receiver capture rules.
- `net-transfer-withcallback-simple.key` covers the `transferWithCallback`
  rule (selected via `\withOptions transferSemantics:withCallback;`): the
  contract invariant `CInv(storage, net)` is defined per example by an
  `insertCInv` taclet; the rule proves it on exit and assumes it after the
  storage/net havoc.
- `net-transfer-withcallback-storage.key` covers the transfer-last discipline
  with an invariant relating a storage field to the ledger; swapping the two
  statements reproduces the classic re-entrancy bug (the "invariant on exit"
  branch stays open).

## Require

- `require-holds-diamond.key` covers `requireConditionCapture` +
  `requireSimple` in a diamond (both branches close from a known-true guard).
- `require-guard-box.key` covers the box guard semantics: the "Reverts" branch
  closes via `revertBox` without knowing the condition.

## Capture partition (non-simple RHS / index)

- `storage-index-write-nse-chain.key` — `balances[i+1] = x*y + 3;` — the full
  `nse1[nse2] = nse3` chain: `indexWriteValueRhsCapture`, then
  `storageIndexWriteNonSimpleIndexCapture`, then the mapping save.
- `storage-matrix-nse-index.key` — `matrix[i+1][j+1] = x + y;` — inner
  (`indexWriteInnerNonSimpleIndexCapture`) plus outer index captures.
- `storage-index-read-nse-index.key` — `result = values[i+1];`
  (`storageIndexRead_unfold_rightSndIndex`).
- `storage-root-write-rhs-capture.key` / `storage-field-write-rhs-capture.key`
  — nested value RHS into a root / field target
  (`storageRootWriteValueRhsCapture`, `fieldWriteValueRhsCapture`).
- `storage-field-copy-value-field.key` — `alice.age = bob.age;`
  (`storageFieldRead_unfold_rightSndResult`).
- `storage-index-copy-value.key` — `balances[i] = values[j];`
  (`storageIndexRead_unfold_rightSndResult`).
- `storage-field-write-capture-src.key` — `alice.account = bob.account;`
  (`storageFieldWriteCaptureSrc`, reference-path RHS into a storage alias).
- `memory-index-write-nse.key` — `xs[i+1] = a + b;` — memory value-RHS and
  index captures.
- `storage-index-delete-nse-index.key` — `delete balances[k+1];`
  (`storageIndexDeleteNonSimpleIndexCapture`).
- `storage-index-read-mapping-store-root.key` — `total = balances[k];`
  (`storageIndexReadMappingStoreRoot`).
