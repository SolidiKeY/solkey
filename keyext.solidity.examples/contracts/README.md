# Specified contracts

Contracts whose specification lives in `@custom:key` natspec clauses, proved function by function
by `ContractExamplesTest` (except `benchmark/`):

| Directory | Holds |
|---|---|
| `solidikey/` | the example contracts of solidiKeY |
| `real-world/` | published contracts, rewritten until every function closes; `open/` keeps what does not close yet |
| `real-world-plain/` | `real-world/` with its specification written as `require` and `assert` (see below) |
| `benchmark/` | published contracts kept as published, to measure what SolKey verifies without a rewrite (`benchmark/README.md`) |

`real-world-plain/` mirrors `real-world/` with its specification written in the code: each
`requires` becomes a `require` at entry and each `ensures` an `assert` at exit (`\old(e)` a local
saved at entry, `->` written `!a || b`, `<->` written `==`); a contract `invariant` is a `require`
at entry and an `assert` at exit of every specified function, and only an `assert` at the end
of a constructor. Modifiers are inlined, `unchecked` blocks are removed (their code kept), and an
early `return` becomes an assignment to the named return so the asserts run last. Each such
function is tagged `/// @custom:key box`, so a `require` is assumed as the `requires` was, and a
payable one that needs it gets the `require(msg.value >= 0)` a specified function assumes; loop
invariants stay as `@custom:key` tags. Its `solidikey/` holds `Counter` of `solidikey/` written
the same way.

Quantifiers become parameters, since a proof holds for every argument:

- `ensures \forall address a; P(a)` adds a parameter `a` and `assert(P(a))`; an `\old(…)` that
  mentions `a` is a local saved at entry, guarded when it indexes an array (`j < arr.length ?
  arr[j] : 0`).
- `invariant \forall address a; P(a)` adds `a` too: `require(P(a))` at entry, plus `require`s of
  `P` at the points the body touches (`msg.sender`, the last key, index `0`, …), and `assert(P(a))`
  at exit. Each `require` is an instance of the invariant, and preserving `P` at an arbitrary `a`
  proves it for all. `!(\exists …)` is the `\forall` of the negation.
- `ensures \exists uint w; P(w)` names its witness: `winnerName` binds `winningProposal(…)` to a
  local `w` and asserts `P(w)`.

Two clauses still have no Solidity form: `net(a)`, the payment ledger, and `address(this)`,
which the prover does not load in a body. `real-world-plain/` itself carries no `@custom:key`
clause other than `box`, `skip` and loop invariants, so `real-world-plain/open/` holds the rest
in two parts:

| Directory | Holds |
|---|---|
| `open/key-spec/` | every function such a clause belongs to, with that clause kept as its `@custom:key` `requires`, `ensures`, `invariant` or `assignable` and the rest as `require`/`assert`; a contract invariant belongs to every function, so such a contract moves whole (`Escrow` and `PiggyBank` of `solidikey/` included), and its constructor keeps the `skip` of the specified source |
| `open/unprovable/` | what does not close yet: `ArrayRemoveByShifting.sol` mirrors `real-world/open/`, whose loop invariant cannot state the shifted prefix, and `MultiAuction.sol` of `solidikey/`, whose quantified invariants (kept as `@custom:key` clauses, as in `key-spec/`) exhaust the step budget as in `solidikey/` |

`ContractExamplesTest` lists the functions that do not close as known open. The directives are
listed in `../taclets/README.md`, "Directives".

## `solidikey/`

`solidikey/` holds the example contracts of solidiKeY (`solidity-contracts-examples` of the
`solidityFlattening` branch), whose `SoliditySpecCompiler` turned `/*@ contract_invariant …
only_if … on_success … */` blocks into a `.key` problem. Here the same specification lives in
`@custom:key` natspec clauses and the obligation is generated when the `.sol` is loaded, so
every function runs with `./run-key.sh contracts/solidikey/Escrow.sol -f placeInEscrow`, with the whole
file (`./run-key.sh contracts/solidikey/Escrow.sol`), and in KeYther (open the file, pick the function;
the browser's header says what it is proved against). `--print-problem` prints the generated
`.key` text, which is the `net/obligations/*.key` shape: an `insertCInv` taclet defining `CInv(s, n)` as
the conjoined invariants, and the ISoLA 2020 eq.-4 problem

```
msg.value bound & !(msgSender = self) & requires & CInv(storage, net) ->
{[old := storage || oldNet := net ||] net := storeSt(net, at(msgSender), net(msgSender) + msgValue)
 || selfBalance := selfBalance + msgValue}
\[{ [result = ]f()@C; }\] (CInv(storage, net) & ensures)
```

A specified function is always proved in the box modality (partial correctness, as the paper);
`msg.value >= 0` is assumed for a `payable` function and `msg.value == 0` otherwise, and the
caller is assumed not to be the contract itself (`msg.sender != address(this)`). `-O
transferSemantics:withCallback` (the dialog's "transfer with callback" in KeYther) selects the
callback rules, which re-establish `CInv` at every `.transfer`.

## Specification expressions

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

## The contracts

- **`ArrayMatrix*.sol` / `ArrayCube*.sol`** — 76 examples covering all four fixed/dynamic
  matrix shapes, two mixed cube shapes, aliases, deep copies across memory and storage,
  row sharing, deletion, push/pop, and shape-changing copies. `docs/array-shape-coverage.md`
  records the breadth-first construction and the legal operation edges.
- **`ArrayMappingShapes.sol`** — four boundary cases distinguishing deletion of a
  fixed-array mapping value from pop/delete of its containing mapping array.
- **`ArrayCopyShapes.sol`** — compiler-backed copy examples: fixed and dynamic target
  lengths, nested tail clearing, struct members, independent copies, and storage aliases.
  `docs/array-copy-shapes.md` gives the proposed mathematical proof and compiler inspection
  commands.
- **`Escrow.sol`** — escrow-v2 with its `State` enum and all three functions, the course
  invariant (`sender != receiver`, `amountInEscrow == net(sender) + net(receiver)`, the two
  state conditionals) and the original `only_if`/`on_success` clauses. All close; the
  original's extra `\forall address a; …` "no third party pays" conjunct is left out — it
  would need the callee-side reasoning of `withdrawFromEscrow`'s transfer.
- **`PiggyBank.sol`** — PiggyBank1 with its enum; the original carried no specification, so
  the course invariant of `../net/PiggyBankNet.sol` is used and the modifier conditions become
  `requires`. Both functions close.
- **`MultiAuction.sol`** — the quantified invariant ported as written (`\exists address hb;
  \forall address a; …`, array membership via `\exists uint i; … bidders[i] == a`,
  `auctionOwner != address(this)`). Its obligations load, expand the invariant and run, but
  automode does not close them: the quantifier instantiations explode into hundreds of goals
  within the step budget, including goals that are closable by hand (`ContractExamplesTest`
  lists `placeOrIncreaseBid`, `withdraw` and even the empty `myTest` as known-open).
  `closeAuction` is skipped: its loops transfer, so their invariants would have to
  re-establish the contract invariant at every callback.
- **`Storage.sol`** — MyContract's live probes `m`, `a1`, `d1`, `f`, `h` with their `\result`
  clauses as named returns. All close. (`m` writes 8 and the original spec claimed 7; the port
  states what the body does.)

Not ported: `multicontract/MultiAuction.sol` (a `library` with a struct-with-mapping parameter;
libraries are unsupported) and `text/`. The `net/obligations/*.key` problems stay as the hand-written
reference for the same obligation shape.

Every body follows the calculus conventions listed in `../net/README.md`: modifiers
are inlined requires (written before the prover inlined modifiers itself), `now` is a `timeNow` state variable, a storage read inside a comparison
or a compound expression is bound to a local first. `ContractExamplesTest` enumerates the
directory (the CI-only `solidityExamples` group), so a new contract joins by being written.

## `real-world/`

`real-world/` holds small contracts taken from well-known public sources: the Solidity
documentation, Solidity by Example, OpenZeppelin, solmate and the deployed WETH9. Each one is
specified in `@custom:key` clauses like `solidikey/`, and every function is proved. Each file's
header has a pinned `// Source:` URL and a `// Changes:` list of every change to the original.
Those changes are only what the supported fragment forces: events, custom errors and string
messages are dropped; a call inside a modifier is replaced by the callee's body, libraries are
inlined and inheritance is flattened (each crashes the loader, `docs/bugs.md`); `bytes32`
becomes `uint256`; struct constructors become `push()` plus field writes; `block.timestamp`
becomes `timeNow`; storage reads are bound to locals as described in `../net/README.md`, "Calculus conventions";
and return values are named, since an obligation refers to them by name. `ContractExamplesTest`
enumerates this directory (not `real-world/open/`).

Checked arithmetic is not modelled (integers are unbounded), so a `uint` parameter gets a
`requires x >= 0`, and a function whose body would underflow gets a `requires` for the case in
which the EVM does not revert. Access control is stated as an `ensures` rather than a
`requires`: `ensures \old(_owner) == msg.sender` holds on every run that does not revert, so it
proves that the code's own checks keep everyone else out.

- **`SimpleAuction.sol`** — the Solidity docs' open auction. The invariant is
  `\forall address a; pendingReturns[a] >= 0`. `bid` credits the outbid leader with their bid;
  `withdraw` zeroes the caller's refund and pays exactly that amount (`net` drops by it);
  `auctionEnd` pays the highest bid to the beneficiary. `send` has no rule yet, so `withdraw`
  uses `transfer` and returns nothing.
- **`Purchase.sol`** — the Solidity docs' Safe Remote Purchase, a state machine with a
  ledger invariant for each state: the seller has 2 × value deposited while Created, Locked and
  Release, and the buyer has 0, 2 × value and value in those states; once Inactive, their nets
  cancel out. `refundSeller` ends with the seller at `-value` (deposit back plus the price) and
  the buyer at `value`. `confirmPurchase` assumes a fresh buyer.
- **`ERC20.sol`** — Solidity by Example's token. `transfer`/`transferFrom` move the amount
  between the two balances (self-transfer leaves the balance unchanged), keep `totalSupply` and
  spend the allowance; `transfer` also proves that no other balance changes (`\forall address a;
  …`). `approve`, `mint` and `burn` are specified too.
- **`WETH9.sol`** — Wrapped Ether ported to 0.8. `deposit` and `withdraw` prove that the token
  balance and the payment ledger move together (tokens minted or burned = ether received or
  paid out); `transferFrom` also covers the unlimited-allowance case.
- **`EtherWallet.sol`** — Solidity by Example's wallet. `withdraw` proves that only the owner
  can withdraw and that the owner receives exactly `_amount`.
- **`Ownable.sol`** — OpenZeppelin's `Ownable`, simplified: `transferOwnership` and
  `renounceOwnership` succeed only for the current owner and set the new one.

- **`OZPausable.sol`** — OpenZeppelin v5.0.0 `Pausable` with its mock: `pause`/`unpause`
  succeed only from the opposite state and flip `_paused`; `whenNotPaused`/`whenPaused` bodies
  run only in that state.
- **`OZReentrancyGuard.sol`** — `ReentrancyGuard` with its mock under the invariant
  `_status == 1`; `countLocalRecursive` proves that the guarded recursion reverts for every
  `n > 0` (`ensures n == 0`). The constants are pinned in the invariant (a constant reads as
  unconstrained storage).
- **`OZNonces.sol`** — `useNonce` returns the old nonce and increments only the owner's
  (`\forall` frame); `useCheckedNonce` too.
- **`OZOwnable2Step.sol`** — two-step ownership: `transferOwnership` only sets the pending
  owner; `acceptOwnership` succeeds only for it, makes it owner and clears the slot.
- **`OZMath.sol`** — `tryAdd`/`trySub`/`tryMul` (success implies the exact result; `trySub`
  succeeds iff `b <= a`), `tryDiv`, `tryMod`, `average`, `ceilDiv`, `max`, `min`.
- **`OZCounters.sol`** — the v4.9.0 `Counters` library inlined into a contract.
- **`SolmateOwned.sol`** — solmate v7 `Owned`.
- **`DocsBallot.sol`** — the docs' `Ballot` (`voting.rst`): `delegate` follows the delegation
  chain under a loop invariant; `winningProposal` proves its result has the maximal `voteCount`
  over the scanned prefix. The constructor gets no obligation (a `uint256[] memory` parameter).
- **Solidity by Example** (pinned at commit `5bcdca02`): `ArrayReplaceFromEnd.sol`,
  `ArrayRemoveByShifting.sol` (bounds only; shifting checked on concrete data), `Enum.sol`,
  `FunctionModifier.sol` (the `noReentrancy` guard proves the recursive call reverts),
  `Counter.sol`, `Mapping.sol` and `NestedMapping.sol` (frames over `\forall` keys),
  `KingOfEther.sol`, `EtherGame.sol` (`0 <= balance <= TARGET_AMOUNT`), `EtherStore.sol`
  (`\forall address a; balances[a] == net(a)` under the default transfer semantics; under
  `-O transferSemantics:withCallback` `withdraw` stays open — the re-entrancy the example
  teaches), `Account.sol`, `IterableMapping.sol` (library inlined; three quantified invariants
  of the index structure, swap-and-pop removal included), `SendEther.sol` (`transfer`, `send`,
  `call{value}`) and `TimeLock.sol` (no withdrawal before the lock time).

Every clause was checked for vacuity: changing it to something false leaves the proof open.

`real-world/open/` holds specifications that load but do not close, with the reason in the file:
`DocsBallot.sol` (`winnerName` with one `\exists` over a nested `\forall`) and
`ArrayRemoveByShifting.sol` (the full functional spec of `remove`: a loop invariant cannot use
`\old`). `OpenExamplesStayOpenTest` fails when one of them starts to close. `docs/limitations.md` explains each.

## `benchmark/`

Where `real-world/` rewrites a contract until it closes, `benchmark/` keeps the published source
and allows only light, listed hacks; every file carries a specification. A contract that needs
more is not added, only listed with its blocker. `scripts/benchmark.sh` prints `N/M` per
contract. The results, and the blockers ranked by how many contracts they stop, are in
`benchmark/README.md`. No test group runs this directory.

