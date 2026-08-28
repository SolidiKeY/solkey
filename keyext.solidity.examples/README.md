# Solidity Examples

`TestSuite.sol` holds the taclet examples; `PiggyBank.sol`, `Escrow.sol`, `Auction.sol` and
`Casino.sol` hold scenario examples (see below). All are loaded the same way.

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

`@custom:` is solc's extension prefix; any other tag is rejected as invalid documentation.

## Known gaps

Three shapes have no `assert` form and are not covered by any example:

- **"this always reverts"** — was `\[{ … }\](false)`. `require`'s box false-branch and
  out-of-bounds array access used to be checked this way.
- **The `net` payment ledger** (`docs/net.md`) — needs `\rules` blocks to define `CInv`
  and `\withOptions transferSemantics:withCallback`, which a synthesized obligation cannot
  carry. Covered by the `.key` problems of `net/` instead, which call real functions of
  `net/PiggyBankNet.sol` (see "The `net/` directory" below).
- **Whole-subtree equality** — `find<[int]>(storage, cons2(matrix, at(0))) = find<[int]>(storage,
  cons1(values))` compares two storage subtrees; Solidity cannot state it, and reading
  `matrix[0].length` back does not discharge. `storageIndexCopysourceAfterPush` therefore only
  checks that the copy-after-push executes.

Two smaller ones, worked around in place with a comment:

- a negative literal directly inside an assert condition (`assert(r == -5)`) does not
  discharge; bind it first (`int expected = -5;`);
- a popped slot is out of bounds, so `pop` clearing it is only observable after pushing again.

## Scenario contracts

`PiggyBank.sol`, `Escrow.sol`, `Auction.sol` and `Casino.sol` port the SolidityCalculus
course examples (piggy bank lifecycle, two-party escrow, single/multi auction with the
reentrancy-ordering lesson, casino bet resolution) into the supported fragment. Each
function is a self-contained scenario: box-tagged requires set up the pre-state, the
transition statements run inline, asserts state the postcondition or invariant. Constructs
the calculus lacks are modeled away uniformly:

- enum states are a `uint state;` with the values documented in a comment;
- participants are `uint` ids and `msg.sender` is a `caller` state variable;
- `block.timestamp` is a `timeNow` state variable;
- `.transfer` and the `net` ledger are outbound counters (`ownerReceived`, `paidOut`) or
  credit mappings (`refunds`, `withdrawable`), so conservation properties become sums of
  those; loops are unrolled for two participants;
- a modifier is inlined as requires (happy path) or as an `if (guard) { body }` wrapper
  whose test asserts the state unchanged — the positive no-op form of "this reverts".

Run them like the test suite: `./run-key.sh keyext.solidity.examples/PiggyBank.sol` — every
function of every file closes. `ScenarioExamplesTest` enumerates all four contracts, so a
new scenario function joins `./gradlew :keyext.solidity.core:test` by being written.

The port covers state transitions and conservation properties, not the transfer/reentrancy
semantics itself: `.transfer` is a counter update here, so nothing re-enters, and "the
contract invariant holds at every external transfer" (`CInv`, `docs/net.md`) is not
expressible in a `.sol`-synthesized obligation. The `closeV1OpenDuringPayout`-style
snapshot tests state the surface positively instead: they observe (and prove) which state
a reentrant callee *would* see at the payout point. The real transfer/invariant obligations
live in the `net/` `.key` problems (next section).

## The `net/` directory

`net/` holds the `net`-ledger examples (`docs/net.md`). `PiggyBankNet.sol` is a real
Solidity contract — `payable` functions reading `msg.sender`/`msg.value`, `address
payable` receivers, `.transfer` — and each `.key` problem beside it verifies one of its
functions by calling it in the modality, solidiKeY-style:

```
\problem {
    pre -> \<{ payTo(to)@PiggyBankNet; }\>(selectSt<[int]>(net, at(to)) = -5)
}
```

The body is loaded from the `.sol` source (`SolJSONParser` desugars `msg.sender` /
`msg.value` to the `msgSender`/`msgValue` program variables and resolves
`transfer`/`send` to the builtins) and inlined by `functionBodyExpand`. The problems stay
`.key`-based because their obligations need what a synthesized `.sol` obligation cannot
carry: a `\rules { insertCInv … }` block defining the contract invariant
`CInv(storage, net)`, the `\withOptions transferSemantics:withCallback` choice, and a
hand-written proof-obligation shape. The `net-*` starters cover the ledger update,
`msg.*` desugaring, and `.transfer` under both semantics (simple, capture-argument,
capture-receiver, with-callback); the two `piggybank-*-invariant` problems verify the
course PiggyBank invariant `balance = net(owner)` at the transfer boundary:

- `piggybank-addMoney-invariant.key` — the paper's proof-obligation schema (ISoLA 2020,
  eq. 4): book the incoming payment on `net(msgSender)`, run `addMoney()@PiggyBankNet`,
  prove the invariant restored;
- `piggybank-break-invariant.key` — `transferWithCallback` on
  `breakPiggyBank()@PiggyBankNet`: the transfer-last body closes both the "invariant on
  exit" and the havocked "resume after callback" branch; moving the transfer before the
  storage write reopens the exit branch — the Fig. 4 re-entrancy bug.

Run one with `./gradlew :keyext.solidity.core:solidityCli -PkeyFile=<path>` (or pass the
path in `--args`); `NetExamplesTest` enumerates the directory, so a new `.key` problem
joins `./gradlew :keyext.solidity.core:test` by being written.

### Calculus conventions the scenario files follow

Found while closing these proofs; violating one leaves an open goal (or fails to load)
without pointing at the culprit:

- a comparison may read storage on **one side only** — `require(caller == owner)` stalls;
  bind one side first (`uint o = owner; require(caller == o);`);
- the right side of a compound assignment must not read storage —
  `ownerReceived += balance` stalls; bind first (`uint b = balance; ownerReceived += b;`);
- an assert compares bound locals, never an arithmetic expression —
  `assert(r == x + y)` stalls; bind `uint expected = x + y;` first;
- a storage-to-storage copy (`betTimestamp = timeNow;`) stalls when other storage writes
  precede it in the body; bind the read before the writes;
- a bare `require(someBool)` on a storage bool is assumable but not observable: a local
  bound from that bool won't discharge an assert. Use `require(someBool == true)` when the
  test later asserts the snapshot;
- a parenthesized subexpression inside a larger condition
  (`require(a && (b || c))`) is a solc `TupleExpression`, which `SolJSONParser` rejects at
  load time — split it into its own `require(b || c)`;
- `address` equality has no discharging rule, hence the `uint` participant ids.

## Other directories

`fieldAccess/`, `functionBody/` and `newVariable/` still use `.key` problems — they exercise
loader and taclet-application details rather than program rules.
