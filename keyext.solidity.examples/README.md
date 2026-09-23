# Solidity Examples

`TestSuite.sol` holds the taclet examples; `net/` holds the scenario contracts with their
invariant-based `.key` proof obligations (see "The `net/` directory"); `contracts/` holds the
solidiKeY example contracts, specified in natspec `@custom:key` clauses (see "The `contracts/`
directory").

There are no `.key` problem files beside `TestSuite.sol`: the loader reads the contract and
synthesizes one obligation per function, so the whole specification lives in the Solidity body
and every test program is real Solidity, type-checked by `solc` on load. `AGENTS.md` has the
commands to run any of it, and `docs/taclets-implementation.md` the synthesized obligation's
exact shape.

## Runtime cross-checking

`SolidityRuntimeExecutionTest` compiles `TestSuite.sol` and every `solc/*.sol` with solc,
deploys each contract on an in-process Besu EVM, and executes every provable function — a
closed proof must not hit a failing `assert` (Panic 0x01) when actually run. A parameterized
function runs with the values its leading `require` pins (an equality, or the tightest bound of
a range). Three kinds of case are skipped rather than failed:

- a box-tagged function whose `require` reverts on the fresh all-zero storage — vacuous at
  runtime, exactly as the box modality treats it;
- a parameterized function whose leading requires do not determine a value for every parameter;
- entries of the test's `KNOWN_DIVERGENT` set: examples proved with KeY's unbounded
  integers that panic under the EVM's checked arithmetic (currently none).

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

Array lengths themselves need no `require`: `sizeNotNegative` makes
`0 <= arr.length` available unconditionally (`docs/storage.md` §8b), which is
what proves the untagged `storagePopUnknownLength`, `storagePushLengthPositive`
and `storagePushReadBack` on a fully unknown storage.

### Directives

| Natspec tag | Effect |
|---|---|
| `/// @custom:key box` | box modality — `require` becomes an assumption |
| `/// @custom:key skip` | no obligation for this function (loops, constructors) |
| `/// @custom:key invariant e` | on the contract: a conjunct of the contract invariant `CInv` |
| `/// @custom:key requires e` | assumed before the call (the old `only_if`) |
| `/// @custom:key ensures e` | proved after the call (the old `on_success`) |
| `/// @custom:key assignable …` | accepted and ignored: bodies are inlined whole |

`@custom:` is solc's extension prefix; any other tag is rejected as invalid documentation. A
tag starts a line and a clause runs to the next tag, so a long clause may continue on the next
`///` line. The last four make a function *specified* — see "The `contracts/` directory" for
what that means and for the expression language.

## Known gaps

Three shapes have no `assert` form and are not covered by any example:

- **"this always reverts"** — was `\[{ … }\](false)`. `require`'s box false-branch and
  out-of-bounds array access used to be checked this way.
- **The `net` payment ledger** (`docs/net.md`) — needs a `\rules` block to define `CInv`
  and an obligation that books `msg.value`, which the plain `assert`-style obligation does
  not carry. Covered by the `.key` problems of `net/` (see "The `net/` directory" below) and,
  since the `@custom:key` clauses exist, by the specified contracts of `contracts/`, whose
  obligation is generated in exactly that shape. (Taclet *options* a plain obligation can
  take too: `run-key.sh -O category:choice`, e.g. `-O transferSemantics:withCallback`.)
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
with-callback). The transfer rules are split by modality — box books the debit with no
funds check, diamond owes `0 <= v & v <= selfBalance` as a "sufficient funds" goal — so
the starters run in box with no funding premises (`net-transfer-unfunded.key` pins the
unconditional booking on a fully symbolic balance), while
`net-transfer-diamond-funded.key` / `net-transfer-withcallback-diamond-funded.key`
discharge the diamond obligation from a funding antecedent. The negative twins (an
unfunded diamond must stay open) live in
`keyext.solidity.core/src/test/resources/org/key_project/solidity/examples/open/`,
asserted by `NetExamplesTest#unfundedDiamondStaysOpen`.

Symbolic POs occasionally need two kinds of sound antecedent strengthening, always noted
in the file comment: `geq(field, 0)` uint-range assumptions ("Solidity Light" uses
unbounded ints), and participant-distinctness pins (`bidder != owner`) where automode
will not perform a mapping-alias case split on its own.

Run one with `./gradlew :keyext.solidity.core:solidityCli -PkeyFile=<path>` (or pass the
path in `--args`); `NetExamplesTest` enumerates the directory, so a new `.key` problem
joins `./gradlew :keyext.solidity.core:testSolidityExamples` (the CI-only examples group)
by being written.

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
  proof later needs the value.

## The `contracts/` directory

`contracts/` holds the example contracts of solidiKeY (`solidity-contracts-examples` of the
`solidityFlattening` branch), whose `SoliditySpecCompiler` turned `/*@ contract_invariant …
only_if … on_success … */` blocks into a `.key` problem. Here the same specification lives in
`@custom:key` natspec clauses and the obligation is generated when the `.sol` is loaded, so
every function runs with `./run-key.sh contracts/Escrow.sol -f placeInEscrow`, with the whole
file (`./run-key.sh contracts/Escrow.sol`), and in KeYther (open the file, pick the function;
the browser's header says what it is proved against). `--print-problem` prints the generated
`.key` text, which is the `net/*.key` shape: an `insertCInv` taclet defining `CInv(s, n)` as
the conjoined invariants, and the ISoLA 2020 eq.-4 problem

```
msg.value bound & requires & CInv(storage, net) ->
{[old := storage || oldNet := net ||] net := storeSt(net, at(msgSender), net(msgSender) + msgValue)
 || selfBalance := selfBalance + msgValue}
\[{ [result = ]f()@C; }\] (CInv(storage, net) & ensures)
```

A specified function is always proved in the box modality (partial correctness, as the paper);
`msg.value >= 0` is assumed for a `payable` function and `msg.value == 0` otherwise. `-O
transferSemantics:withCallback` (the dialog's "transfer with callback" in KeYther) selects the
callback rules, which re-establish `CInv` at every `.transfer`.

### Specification expressions

Solidity-like, with a few logic additions:

```
expr    ::= iff
iff     ::= impl ('<->' impl)*            impl ::= or ('->' impl)?
or      ::= and ('||' and)*               and  ::= eq ('&&' eq)*
eq      ::= rel (('==' | '!=') rel)?      rel  ::= add (('<' | '<=' | '>' | '>=') add)?
add     ::= mul (('+' | '-') mul)*        mul  ::= unary (('*' | '/' | '%') unary)*
unary   ::= ('!' | '-') unary | postfix   postfix ::= primary ('[' expr ']' | '.' IDENT)*
primary ::= INT | true | false | IDENT | net(expr) | \old(expr) | \result | address(expr)
          | this | (expr) | (\forall | \exists) sort IDENT ; expr
```

A quantifier's body extends as far right as possible; parenthesize it to end it earlier. What
each construct becomes (`S` is `s` inside the invariant, `storage` in requires/ensures, `old`
inside `\old`; `N` likewise `n`/`net`/`oldNet`):

| Spec | Term |
|---|---|
| state variable `x` | `find<[int]>(S, cons1(C$x))`, a `bool` one `find<[bool]>(…) = TRUE` |
| `m[k]`, `arr[i]`, `arr.length`, `st.f` | `find<[τ]>(S, cons2(C$m, at(k)))`, `… at(i)`, `… size`, `cons2(C$st, C$St$f)` |
| `Enum.Member` | the member's ordinal |
| `net(a)`, `msg.sender`, `msg.value`, `address(this)` | `selectSt<[int]>(N, at(a))`, `msgSender`, `msgValue`, `self` |
| parameter `p`, `\result` | the program variable `p`, `result` (needs one *named* return) |
| `\old(e)` | `e` over `old`/`oldNet` (ensures only) |
| `== != < <= > >= + - * / %` | `= , !(=), <, <=, >, >=, +, -, *, div, mod`; `==` on bools is `<->` |
| `&& \|\| ! -> <->`, `\forall address a; e` | `& \| ! -> <->`, `\forall int a; e` (`bool` stays `bool`) |

Errors (an unknown identifier, `\old` outside ensures, indexing a non-mapping, …) name the
function and the clause. The grammar is `src/main/antlr/SolSpec.g4`, the compiler a visitor
over its parse tree (`speclang/natspec/SpecCompiler`), and the golden tests `SpecCompilerTest`
list every row.

### The contracts

- **`Escrow.sol`** — escrow-v2 with its `State` enum and all three functions, the course
  invariant (`sender != receiver`, `amountInEscrow == net(sender) + net(receiver)`, the two
  state conditionals) and the original `only_if`/`on_success` clauses. All close; the
  original's extra `\forall address a; …` "no third party pays" conjunct is left out — it
  would need the callee-side reasoning of `withdrawFromEscrow`'s transfer.
- **`PiggyBank.sol`** — PiggyBank1 with its enum; the original carried no specification, so
  the course invariant of `net/PiggyBankNet.sol` is used and the modifier conditions become
  `requires`. Both functions close.
- **`MultiAuction.sol`** — the quantified invariant ported as written (`\exists address hb;
  \forall address a; …`, array membership via `\exists uint i; … bidders[i] == a`,
  `auctionOwner != address(this)`). Its obligations load, expand the invariant and run, but
  automode does not close them: the quantifier instantiations explode into hundreds of goals
  within the step budget, including goals that are closable by hand (`ContractExamplesTest`
  lists `placeOrIncreaseBid`, `withdraw` and even the empty `myTest` as known-open).
  `closeAuction` is skipped: its loops have no rules.
- **`Storage.sol`** — MyContract's live probes `m`, `a1`, `d1`, `f`, `h` with their `\result`
  clauses as named returns. All close. (`m` writes 8 and the original spec claimed 7; the port
  states what the body does.)

Not ported: `multicontract/MultiAuction.sol` (a `library` with a struct-with-mapping parameter;
libraries are unsupported) and `text/`. The `net/*.key` problems stay as the hand-written
reference for the same obligation shape.

Every body follows the calculus conventions listed under "The `net/` directory": modifiers
are inlined requires, `now` is a `timeNow` state variable, a storage read inside a comparison
or a compound expression is bound to a local first. `ContractExamplesTest` enumerates the
directory (the CI-only `solidityExamples` group), so a new contract joins by being written.

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
joins `./gradlew :keyext.solidity.core:testSolidityExamples` (the CI-only examples group)
by being written.

## The `proofs/` directory

`TacletCoverageTest` (the CI-only examples group) requires every taclet of the Solidity rule
files to be applied by some proof: a `TestSuite.sol` function in automode, or a saved proof here
for the taclets no such function reaches. Each `NAME.key` is a problem whose proof applies the
taclet `NAME`. It is either a logic lemma, for a taclet whose function symbols the program rules
never produce (`headDefinition`, `precOfInt`, `applySkip3`, …), or a `TestSuite.sol` obligation
the synthesizer cannot state: one where automode prefers a competing rule
(`localDeclPostdecrement`), or a diamond `transfer` that needs a `selfBalance` precondition.
`NAME.proof` beside it is the saved proof the test replays. After adding a `.key` or changing
the rules, regenerate the proofs with

```bash
./gradlew :keyext.solidity.core:testSolidityExamples --tests "*TacletCoverageTest" \
    -Dorg.key_project.solidity.taclets.TacletCoverageTest.update=true
```

A new taclet that a `TestSuite.sol` function can exercise gets a function there instead.

## The `unprovable/` directory

`unprovable/` holds valid solc ≥ 0.8 examples whose EVM behavior the calculus does not model,
so their obligations cannot close — currently `Unprovable.sol`, whose functions revert on the
EVM's checked arithmetic (Panic 0x11) before their `assert(false)`, while SolKey's unbounded
mathematical integers give the overflowing operation a non-reverting path. No test suite scans
the directory; moving an example here is how it is retired from the suites while staying
compilable.

## The `illegal/` directory

`illegal/` holds the mapping shapes Solidity itself rejects — currently `IllegalMappings.sol`.
A mapping lives only in storage: it has no memory or calldata representation, it cannot be
copied, assigned or deleted as a whole, and its key must be elementary. Each construct is a
commented-out line with the verbatim solc diagnostic above it, so the file compiles and no suite
scans the directory; uncommenting a line must reproduce the quoted error. The record says which
rules the calculus never has to have, and its legal counterparts are the "Mapping indices"
section of `TestSuite.sol`.

`illegal/nocompile/` is the executable half of that record: one **minimal contract per construct**,
written out rather than commented out, so each file really does fail to compile.
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

## Other directories

`fieldAccess/`, `functionBody/` and `newVariable/` still use `.key` problems — they exercise
loader and taclet-application details rather than program rules.
