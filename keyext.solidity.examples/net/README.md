# Net scenario examples

`net/` holds the scenario contracts and their `net`-ledger proof obligations
(`docs/net.md`). Each contract is real Solidity — `payable` functions reading
`msg.sender`/`msg.value`, `address payable` receivers, `.transfer`, `payable(...)` casts
— and each `.key` problem beside it verifies one function by calling it in the modality,
solidiKeY-style, against a contract invariant supplied by a per-problem `insertCInv`
taclet:

```
\problem {
    pre & CInv(storage, net) ->
    {old := storage || net := storeSt(net, at(msgSender),
                                      selectSt<[int]>(net, at(msgSender)) + msgValue)}
    \[{ makeBid()@AuctionNet; }\] (CInv(storage, net) & post)
}
```

(the ISoLA 2020 eq.-4 schema: assume the invariant, book the incoming payment, run the
function, prove the invariant restored plus the function's postcondition). Every
`insertCInv` conjunct, antecedent pin, and postcondition conjunct carries a `//` comment
stating it in the course's surface syntax (`// net(owner) <= 0 :`), so a PO reads
top-to-bottom without decoding the `find`/`selectSt` terms. The bodies are
loaded from the `.sol` sources (`SolJSONParser` desugars `msg.*` to the
`msgSender`/`msgValue` program variables, resolves `transfer`/`send` to the builtins, and
unwraps `payable(...)`/`address(...)` casts) and inlined by `functionBodyExpand`. The
problems stay `.key`-based because their obligations need what a synthesized `.sol`
obligation cannot carry: the `\rules { insertCInv … }` block, the `\withOptions
transferSemantics:withCallback` choice, and the hand-written PO shape.

The contract sets, ported from the SolidityCalculus course (`maltaCourseKey`):

- **`PiggyBankNet.sol`** — the course PiggyBank1 state machine (Unused/InUse/Broken):
  invariant `state != Broken -> balance = net(owner)`, `state == Broken -> net(owner) = 0`.
  `piggybank-addMoney-invariant` (full course postcondition — the bank ends InUse with
  `balance = net(owner)`; carries the course's enum-range assumption),
  `piggybank-breakPiggyBank-invariant` (`net(owner) = 0` and Broken after the payout),
  `piggybank-breakPiggyBank-withcallback` (the course order — Broken written before the
  payout leaves — keeps the invariant at the transfer point). The contract also hosts the
  `readMsg`/`payTo`/`payToPlus`/`payOwner` helpers the `net-*` starters call.
- **`EscrowNet.sol`** — invariant `sender != receiver`, `amountInEscrow = net(sender) +
  net(receiver)`, plus state conditionals. `escrow-placeInEscrow-invariant`
  (`net(sender) = msg.value` after the deposit), `escrow-releaseEscrow-invariant`
  (flag flips preserve the invariant), `escrow-withdrawFrom-invariant`
  (`net(receiver) = -net(sender)`; noCallback, since the course body order breaks the
  invariant at the transfer point).
- **`AuctionNet.sol`** (transfer-last) — invariant `bid = net(bidder) + net(owner)`,
  `net(owner) <= 0`, `mode = Open -> net(owner) = 0`. `auction-makeBid-invariant`
  (full course postcondition, noCallback), `auction-makeBid-withcallback` (the paper's
  Table-1 entry: the transfer-last body keeps the invariant at the refund point),
  `auction-closeAuction-invariant` (`net(owner) = -net(bidder)`).
- **`AuctionWithdrawNet.sol`** (withdrawal pattern) — the same invariant over
  `effective_net(a) = net(a) - withdrawableBalances[a]`, with mapping reads via
  `cons2(...$withdrawableBalances, at(a))`. `auction-withdraw-makeBid-invariant` and
  `auction-withdraw-withdraw-invariant` (paying out one's credited balance leaves every
  `effective_net` unchanged). Like the course, `closeAuction` has no PO: it credits the
  owner without resetting `bid`, so the stated invariant provably does not survive it.
- **`CasinoNet.sol`** — own design (the course ships no casino `.key`): conservation
  `net(operator) + net(player) = pot` (+ `bet` while a bet is active).
  `casino-addToPot-invariant`, `casino-placeBet-invariant`, `casino-decideBet-invariant`
  (both parity branches), `casino-decideBet-withcallback` (payout last keeps the
  invariant at the transfer point).

The `net-*` starters cover the raw machinery: the ledger update, `msg.*` desugaring, and
`.transfer` under both semantics (simple, capture-argument, capture-receiver,
with-callback, transfer to self). The transfer rules are split by modality — box books the
debit with no funds check, diamond owes `0 <= v` as a "non-negative amount" goal — and a
transfer to `self` leaves `net` unchanged, so a starter paying an arbitrary address assumes
it is not `self` (`net-transfer-self.key` / `net-transfer-withcallback-self.key` cover that
case). `net-transfer-diamond.key` / `net-transfer-withcallback-diamond.key` discharge the
diamond obligation; the negative twins (a diamond over an unconstrained amount must stay
open) live in `keyext.solidity.core/src/test/resources/org/key_project/solidity/examples/open/`,
asserted by `NetExamplesTest#negativeAmountDiamondStaysOpen`.

Symbolic POs occasionally need two kinds of sound antecedent strengthening, always noted
in the file comment: `geq(field, 0)` uint-range assumptions ("Solidity Light" uses
unbounded ints), and participant-distinctness pins (`bidder != owner`) where automode
will not perform a mapping-alias case split on its own.

Run one with `./gradlew :keyext.solidity.core:solidityCli -PkeyFile=<path>` (or pass the
path in `--args`); `NetExamplesTest` enumerates the directory, so a new `.key` problem
joins `./gradlew :keyext.solidity.core:testSolidityExamples` (the CI-only examples group)
by being written.

## Calculus conventions the `.sol` bodies follow

Found while closing these proofs; violating one leaves an open goal (or fails to load)
without pointing at the culprit:

- the right side of a compound assignment must not be a bare storage path, and its mapping key
  must not read storage — `pot += other;` and `m[king] += b;` stall; bind first
  (`uint o = other; pot += o;`, `address k = king; m[k] += b;`). A comparison may now read
  storage on either side (`require(msg.sender == sender)` closes);
- an assert compares bound locals, never an arithmetic expression —
  `assert(r == x + y)` stalls; bind `uint expected = x + y;` first;
- a storage-to-storage copy (`releaseTime = timeNow;`) stalls when other storage writes
  precede it in the body; bind the read before the writes
  (`uint rt = timeNow + delayUntilRelease; releaseTime = rt;`);
- a bare `require(someBool)` on a storage bool is assumable but not observable: a local
  bound from that bool won't discharge an assert. Use `require(someBool == true)` when the
  proof later needs the value.

