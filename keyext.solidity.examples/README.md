# Solidity Examples

`TestSuite.sol` holds the taclet examples; `net/` holds the scenario contracts with their
invariant-based `.key` proof obligations (see "The `net/` directory").

`TestSuite.sol` holds the taclet examples. There are no `.key` problem files beside it: the
loader reads the contract, and for each function synthesizes

```
\programSource "<abs path>/TestSuite.sol";
\problem { \<{ someFunction()@TestSuite; }\>(true) }
```

(a function with parameters additionally gets a `\programVariables` block declaring one
unconstrained variable per parameter, passed as the call's arguments)

so the whole specification lives in the Solidity body and every test program is real Solidity,
type-checked by `solc` on load.

## Running

```bash
./run-key.sh keyext.solidity.examples/TestSuite.sol                    # every function
./run-key.sh keyext.solidity.examples/TestSuite.sol testSimpleAssert   # one function
./gradlew :keyext.solidity.core:test --tests "*TacletStarterExamplesTest"
./gradlew :keyext.solidity.core:test --tests "*PaperTestExamplesTest"
```

`TacletStarterExamplesTest` runs the focused one-rule-each functions, `PaperTestExamplesTest`
the end-to-end `test*` ones. Both enumerate the contract, so a new function joins the suite by
being written — nothing has to be registered.

Check the contract with `solc --ast-compact-json keyext.solidity.examples/TestSuite.sol` before
running a suite.

## Writing an example

Every function is `public` and returns nothing. A function may take arguments: the loader
declares one unconstrained program variable per parameter and passes them as the call's
arguments, so the function must be box-tagged and pin the values its asserts rely on in one
conjoined `require(x == 5 && y == 7)` — it plays the role of the old `.key` precondition
`x = 5 & y = 7`. Storage bounds stay in their own ordered requires (see below), since each one
guards the evaluation of the next.

**What the test observes** goes in the body as `assert`. A value the postcondition talks about
is bound to a local first — `return e;` is not supported by the calculus:

```solidity
function storageFieldWriteRead() public {
    alice.age = 34;
    uint r = alice.age;
    assert(r == 34);
}
```

**What the test assumes** goes in the body as `require`, and the function is tagged
`/// @custom:key box`:

```solidity
/// @custom:key box
function storageIndexAddAssign() public {
    require(1 < values.length);
    values[1] = 40;
    values[1] += 2;
    uint r = values[1];
    assert(r == 42);
}
```

The tag is what makes `require` an assumption. Under a diamond `require(c)` means `c ∧ φ` — an
obligation — while under a box it means `c → φ` (`docs/require-assert.md`). `assert` is an
obligation under both, so the specification keeps its force either way.

Tag only functions that need it. A box discharges a reverting execution vacuously, so an
untagged function additionally proves it never reverts — and a tagged one must `require` every
bound its body relies on, outermost first:

```solidity
require(2 < matrix.length);
require(3 < matrix[2].length);
```

Miss one and the proof closes on the out-of-bounds revert branch without checking anything.

### Directives

| Natspec tag | Effect |
|---|---|
| `/// @custom:key box` | box modality — `require` becomes an assumption |
| `/// @custom:key checked` | checked (solc ≥ 0.8) arithmetic — proves under `\withOptions intRules:soliditySemantics;`, which expands the `inUintN`/`inIntN` range guards to their bounds, so out-of-range results revert |

`@custom:` is solc's extension prefix; any other tag is rejected as invalid documentation.

A checked function still reasons over unbounded logic ints for its inputs: `require` both
bounds of every parameter the arithmetic relies on (`require(x >= 0 && x <= 100)` — the lower
bound too, even for `uint`). The `checked*` functions of `TestSuite.sol` are the reference
examples: each `*Reverts` function closes in box exactly because the overflowing operation
reverts, and each `*InRange` one shows the guard discharging.

## Known gaps

Three shapes have no `assert` form and are not covered by any example:

- **"this always reverts"** — was `\[{ … }\](false)`. `require`'s box false-branch and
  out-of-bounds array access used to be checked this way.
- **The `net` payment ledger** (`docs/net.md`) — needs `\rules` blocks to define `CInv`
  and `\withOptions transferSemantics:withCallback`, which a synthesized obligation cannot
  carry. Covered by the `.key` problems of `net/` instead, which call real functions of
  the contracts beside them (see "The `net/` directory" below).
- **Whole-subtree equality** — `find<[int]>(storage, cons2(matrix, at(0))) = find<[int]>(storage,
  cons1(values))` compares two storage subtrees; Solidity cannot state it, and reading
  `matrix[0].length` back does not discharge. `storageIndexCopysourceAfterPush` therefore only
  checks that the copy-after-push executes.

Two smaller ones, worked around in place with a comment:

- a negative literal directly inside an assert condition (`assert(r == -5)`) does not
  discharge; bind it first (`int expected = -5;`);
- a popped slot is out of bounds, so `pop` clearing it is only observable after pushing again.

## The `net/` directory

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
with-callback).

Symbolic POs occasionally need two kinds of sound antecedent strengthening, always noted
in the file comment: `geq(field, 0)` uint-range assumptions ("Solidity Light" uses
unbounded ints), and participant-distinctness pins (`bidder != owner`) where automode
will not perform a mapping-alias case split on its own.

Run one with `./gradlew :keyext.solidity.core:solidityCli -PkeyFile=<path>` (or pass the
path in `--args`); `NetExamplesTest` enumerates the directory, so a new `.key` problem
joins `./gradlew :keyext.solidity.core:test` by being written.

### Calculus conventions the `.sol` bodies follow

Found while closing these proofs; violating one leaves an open goal (or fails to load)
without pointing at the culprit:

- a comparison may read storage on the **left side only** — `require(msg.sender == sender)`
  and `b = msgSender == sender` stall; bind the storage read first
  (`address snd = sender; require(msg.sender == snd);`);
- the right side of a compound assignment must not read storage —
  `pot += msg.value` stalls; bind first (`uint p = pot; pot = p + msg.value;`);
- an assert compares bound locals, never an arithmetic expression —
  `assert(r == x + y)` stalls; bind `uint expected = x + y;` first;
- a storage-to-storage copy (`releaseTime = timeNow;`) stalls when other storage writes
  precede it in the body; bind the read before the writes
  (`uint rt = timeNow + delayUntilRelease; releaseTime = rt;`);
- a bare `require(someBool)` on a storage bool is assumable but not observable: a local
  bound from that bool won't discharge an assert. Use `require(someBool == true)` when the
  proof later needs the value;
- a parenthesized subexpression inside a larger condition
  (`require(a && (b || c))`) is a solc `TupleExpression`, which `SolJSONParser` rejects at
  load time — split it into its own `require(b || c)`.

## The `solc/` directory

`solc/` holds ports of the Solidity compiler's own semantic tests
(`ethereum/solidity`, `test/libsolidity/semanticTests/`) — six contracts written in the same
`require`/`assert` style as `TestSuite.sol`, one per upstream theme (expressions, structs,
arrays, memory, mappings, control flow). Where `TestSuite.sol` exercises one taclet each, these
cross-check the calculus against a description of Solidity semantics SolKey did not write.

`solc/README.md` has the provenance table (upstream file → function), the adaptation rules
(loops unrolled, `return e;` turned into `assert`, `bytesN` dropped), and the list of known
failures — examples that state upstream semantics the calculus cannot discharge yet and are
kept red on purpose. `SolcSemanticsExamplesTest` enumerates the directory, so a new example
joins `./gradlew :keyext.solidity.core:test` by being written.

## Other directories

`fieldAccess/`, `functionBody/` and `newVariable/` still use `.key` problems — they exercise
loader and taclet-application details rather than program rules.
