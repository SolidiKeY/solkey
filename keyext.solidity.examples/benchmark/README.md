# Benchmark: published contracts, as published

`real-world/` holds contracts rewritten until every function closes. This directory measures
the opposite: how much of a published contract SolKey verifies **as it is**. Every file is the
original source, pinned by the `// Source:` line on top. It is specified with `@custom:key`
clauses and changed only by light hacks, each listed in the `// Changes:` line under the source.
A contract that would need a rewrite is not added; it is listed under "Not in the benchmark yet"
with its blocker, and joins once the blocker is gone.

```bash
scripts/benchmark.sh                                  # every contract, one line each, and the total
scripts/benchmark.sh keyext.solidity.examples/benchmark/Coin.sol
./run-key.sh keyext.solidity.examples/benchmark/WithdrawalContract.sol -f becomeRichest --open-goals
```

The benchmark is not part of any test group. It is expected to have open goals.

## Results (2026-10-06): 37 of 39 obligations close

| Contract | Source | Loads verbatim | Closed | Code lines (−/+) | What is left |
|---|---|---|---|---|---|
| `Counter` | Solidity by Example | yes | 2/2 | 0/0 | — |
| `SimpleStorage` | Solidity docs | yes | 1/1 | 0/0 | — |
| `Mapping`, `NestedMapping` | Solidity by Example | yes | 4/4 | 0/0 | — |
| `Fund` | Solidity docs, security considerations | yes | 1/1 | 0/0 | — |
| `Coin` | Solidity docs | no: event, custom error | 3/3 | −4/+1 | — |
| `EtherWallet` | Solidity by Example | no: `require` message | 2/2 | −4/+1 | `getBalance` dropped |
| `SendingEther` (`ReceiveEther`, `SendEther`) | Solidity by Example | no: `require` messages | 3/3 | −6/+3 | `getBalance` dropped |
| `Purchase` | Solidity docs | no: custom errors | 5/5 | −17/+5 | — (the modifiers are kept) |
| `SimpleAuction` | Solidity docs | no: events | 4/4 | −17/+12 | — |
| `ERC20` | Solidity by Example | no: import | 5/5 | −13/+18 | — (+15 of them the inlined `IERC20`) |
| `SendContract` | Solidity docs, common patterns | no: custom error | 2/2 | −2/+1 | — |
| `Todos` | Solidity by Example | no: struct constructors | 1/1 | −12/+0 | `create` dropped |
| `WithdrawalContract` | Solidity docs, common patterns | no: custom error | 2/3 | −2/+1 | `becomeRichest`: `m[storageKey] += e` |
| `AccessRestriction` | Solidity docs, common patterns | no: custom errors | 2/3 | −12/+6 | `forceOwnerChange`: bitwise `&` |

Since 2026-09-26 (19/21): internal calls, `return e;`, tuples, constructors, loops, `send` and
`call{value: v}("")` were added to the prover. `ERC20.mint`/`burn` now close; `SimpleAuction.withdraw`
(`send`) and every constructor are now obligations and close; `ERC20` keeps `return true;` and
its `IERC20` base. Six contracts were added: `Fund`, `SendContract`, `WithdrawalContract`,
`AccessRestriction`, `SendingEther`, `Todos`. `SimpleAuction.auctionEnd` needed
`beneficiary != address(this)` in its requires: a transfer to `self` now leaves `net` unchanged,
so its old ensures was false for that case.

"Closed" counts the obligations the prover generates. A function that returns an unnamed value,
or takes a `string`, has no obligation, so e.g. `Counter.get` or `Todos.get` is not in the
denominator. Code lines count the non-blank source lines removed/added, excluding `@custom:key`
clauses and the header.

Every function was checked for vacuity on 2026-10-06: with `ensures false` added to each public
function and constructor, the benchmark closes 0/39. So neither the requires nor the invariant
is contradictory, and no function always reverts. All files compile with solc 0.8.34
(`./run-key.sh FILE --solc`).

The light hacks used, and how often:

- events and `emit` dropped: `Coin`, `ERC20`, `SimpleAuction`, `Purchase`;
- custom errors dropped, `revert Err(..)` → `revert()`, `require(c, Err(..))` → `require(c)`:
  `Coin`, `SimpleAuction`, `Purchase`, `SendContract`, `WithdrawalContract`, `AccessRestriction`;
- `require(c, "msg")` → `require(c)`: `EtherWallet`, `SendingEther`;
- `address(this).balance` avoided: `EtherWallet`, `SendingEther` (`getBalance` dropped),
  `Purchase` (paid as `2 * value`, equal under the invariant);
- `block.timestamp` → a `timeNow` state variable: `SimpleAuction`, `AccessRestriction`;
- `returns (bool)` → `returns (bool success)`, since `\result` needs one named return:
  `ERC20`, `SimpleAuction.withdraw`;
- `import` inlined: `ERC20`;
- a storage read used as the key of `m[k] +=` bound to a local: `SimpleAuction.bid`;
- a function that does not load dropped: `Todos.create` (struct constructors; it takes a
  `string`, so it had no obligation);
- the unused `bytes memory data` of `(bool sent, bytes memory data) = a.call{..}("")` not bound:
  `SendingEther`.

## Not in the benchmark yet

Loaded as published, these fail before any function is proved, and a light hack does not get
them through. Each "then" column was found by hacking the earlier blockers away in a scratch copy.

| Contract | Source | First blocker | Then |
|---|---|---|---|
| `Ballot` | [Solidity docs](https://raw.githubusercontent.com/ethereum/solidity/v0.8.30/docs/examples/voting.rst) | `bytes32` | struct constructor `Proposal({..})`; indexing the memory-array parameter `proposalNames[i]` crashes |
| `BlindAuction` | [Solidity docs](https://raw.githubusercontent.com/ethereum/solidity/v0.8.30/docs/examples/blind-auction.rst) | events | custom errors, `block.timestamp`, `bytes32`, `keccak256`, struct constructor |
| `Ownable` | [OpenZeppelin v5.0.0](https://raw.githubusercontent.com/OpenZeppelin/openzeppelin-contracts/v5.0.0/contracts/access/Ownable.sol) | import of `Context` | `bytes` (`_msgData`); `onlyOwner` calls `_checkOwner()`: an internal call in a modifier crashes |
| `StateMachine` | [Solidity docs](https://raw.githubusercontent.com/ethereum/solidity/v0.8.30/docs/common-patterns.rst) | custom error | `block.timestamp`; `timedTransitions` calls `nextStage()` (internal call in a modifier, crash); `Stages(uint(stage) + 1)` stalls |
| `Token` | [Solidity docs](https://raw.githubusercontent.com/ethereum/solidity/v0.8.30/docs/examples/modular.rst) | `using Balances for *` | events, `require` message, library call |
| `KingOfEther` + `Attack` | [Solidity by Example](https://raw.githubusercontent.com/Cyfrin/solidity-by-example.github.io/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/hacks/denial-of-service/DenialOfService.sol) | `require` messages | `Attack` calls `kingOfEther.claimThrone{value: v}()`; without `Attack`, `claimThrone` closes |
| `WETH9` | [canonical WETH9](https://raw.githubusercontent.com/gnosis/canonical-weth/master/contracts/WETH9.sol) | pragma 0.4 (the in-JVM solc is 0.8) | needs a 0.8 port (`real-world/WETH9.sol`) |

## Blockers, by the number of contracts they stop

Counted over all 21 sources above, benchmarked or not; a hacked-around blocker counts. This
ranks the backlog of `docs/taclet-ideas.md` by what real code needs. Each was re-tested on
2026-10-06 with a minimal contract.

| Blocker | Contracts | Status (2026-10-06) |
|---|---|---|
| custom errors | 9: Coin, SimpleAuction, Purchase, SendContract, WithdrawalContract, AccessRestriction, BlindAuction, Ownable, StateMachine | open: `Unknown node type ErrorDefinition` |
| events / `emit` | 8: Coin, ERC20, SimpleAuction, Purchase, BlindAuction, Ownable, WETH9, Token | open: `Unknown node type EventDefinition` |
| `require(c, "msg")` | 5: EtherWallet, SendingEther, Ballot, Token, KingOfEther | open: `Not yet supported literal` |
| `address(this)` | 4: EtherWallet, Purchase, SendingEther, WETH9 | open: NPE in `SolJSONParser.parseIdentifier` |
| `block.timestamp` | 4: SimpleAuction, AccessRestriction, BlindAuction, StateMachine | open: the same NPE; the spec language has no `block` |
| `bytes32` / `bytes` | 4: Ballot, BlindAuction, Ownable, SendingEther | open: `No KeYSolidityType for bytes32`; a `bytes32` parameter gets no obligation; `(bool ok, bytes memory d) = a.call{..}("")` is rejected |
| struct constructors | 3: Todos, Ballot, BlindAuction | open: `Unexpected reference declaration Todo expected a state variable` |
| unnamed return value | 2 hacked (ERC20, SimpleAuction); no obligation for `get`-style functions | by design: `\result` needs one named return |
| internal call inside a modifier | 2: Ownable, StateMachine | **new**, crash: the same NPE |
| storage read as the key of `m[k] op= e` | 2: SimpleAuction (hacked), WithdrawalContract | **new**, stalls on `m[r] += e;` |
| imports | 2: ERC20, Ownable | open: only the opened file is given to solc |
| bitwise operators | 1: AccessRestriction | design: no bitwise LDT, stalls on `owner & 0` |
| enum conversion `E(uint(e) + 1)` | 1: StateMachine | **new**, stalls |
| libraries / `using for` | 1: Token | open: `Unknown node type UsingForDirective`; `L.f(x)` hits the same NPE |
| external call with options `c.f{value: v}()` | 1: KingOfEther | open: `Not yet supported expression type: FunctionCallOptions` |
| indexing a memory-array parameter | 1: Ballot | **new**, crash: `ClassCastException` in `SolJSONParser.getVariableExpression` |
| internal calls | 0 | **fixed**: `ERC20.mint`/`burn` close |
| `return e;` / tuple return | 0 | **fixed**: `ERC20` keeps `return true;` |
| loops | 0 | **fixed** (a minimal `for` with an invariant closes; Ballot and BlindAuction do not reach theirs) |
| `send`, `call{value: v}("")` | 0 | **fixed**: `SimpleAuction.withdraw`, `SendEther` close |
| constructors | 0 | **fixed**: six constructors are obligations and close |

Events, custom errors and `require` messages have no effect on the state the calculus models,
and still account for most of the hacks: accepting them in the parser (events and errors
skipped, `emit` a no-op, the message ignored) would make `Coin`, `SendContract` and
`WithdrawalContract` load verbatim and remove most of the edits elsewhere. The next
cheapest step is the null declaration at `SolJSONParser.parseIdentifier`: one site behind
`address(this)`, `block.timestamp`, library calls and internal calls in modifiers, which
together stop or force hacks in ten contracts.
