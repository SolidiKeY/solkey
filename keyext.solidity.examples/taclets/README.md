# Solidity Taclet Examples

These examples exercise source-level Solidity program taclets loaded through
`standardSolidityRules.key`.

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
