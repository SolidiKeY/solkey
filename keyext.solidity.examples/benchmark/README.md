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
./run-key.sh keyext.solidity.examples/benchmark/ERC20.sol -f mint --open-goals
```

The benchmark is not part of any test group. It is expected to have open goals.

## Results (2026-09-26): 19 of 21 obligations close

| Contract | Source | Loads verbatim | Closed | Code lines (−/+) | What is left |
|---|---|---|---|---|---|
| `Counter` | Solidity by Example | yes | 2/2 | 0/0 | — |
| `SimpleStorage` | Solidity docs | yes | 1/1 | 0/0 | — |
| `Mapping`, `NestedMapping` | Solidity by Example | yes | 4/4 | 0/0 | — |
| `Coin` | Solidity docs | no: events | 2/2 | −4/+1 | — |
| `EtherWallet` | Solidity by Example | no: `require` message | 1/1 | −5/+1 | `getBalance` dropped |
| `Purchase` | Solidity docs | no: custom errors | 4/4 | −17/+5 | — (the modifiers are kept) |
| `SimpleAuction` | Solidity docs | no: events | 2/2 | −16/+11 | `withdraw`: `send`, returns `bool` |
| `ERC20` | Solidity by Example | no: import | 3/5 | −19/+7 | `mint`, `burn`: internal calls |

"Closed" counts the obligations the prover generates. A function that returns an unnamed value,
or takes a `string`, has no obligation, so e.g. `SimpleAuction.withdraw` is not in the
denominator. Code lines count the source lines removed/added, excluding `@custom:key` clauses
and the header.

Every closed function was checked for vacuity: with `ensures false` added, each proof stays
open. So neither the requires nor the invariant is contradictory, and the function does not
always revert.

The light hacks used, and how often:

- events and `emit` dropped: `Coin`, `ERC20`, `SimpleAuction`, `Purchase`;
- custom errors dropped, `revert Err(..)` → `revert()`, `require(c, Err(..))` → `require(c)`:
  `Coin`, `SimpleAuction`, `Purchase`;
- `address(this).balance` avoided (it crashes the parser): `EtherWallet` (function dropped),
  `Purchase` (paid as `2 * value`, equal under the invariant);
- `require(c, "msg")` → `require(c)`: `EtherWallet`;
- `return e;` → a named return: `ERC20`;
- `import` inlined or dropped: `ERC20`;
- `block.timestamp` → a `timeNow` state variable, since the spec language has no `block`:
  `SimpleAuction`;
- a storage read on the right of `m[k] +=` bound to a local: `SimpleAuction.bid`.

## Not in the benchmark yet

Loaded as published, these fail before any function is proved, and a light hack does not get
them through. They join, with a specification, once their first blocker is fixed.

| Contract | Source | First blocker | Then |
|---|---|---|---|
| `Todos` | [Solidity by Example](https://raw.githubusercontent.com/Cyfrin/solidity-by-example.github.io/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/structs/Structs.sol) | struct constructor `Todo(..)` | tuple return |
| `Ballot` | [Solidity docs](https://raw.githubusercontent.com/ethereum/solidity/v0.8.30/docs/examples/voting.rst) | `bytes32` | loops, internal call, `require` messages |
| `BlindAuction` | [Solidity docs](https://raw.githubusercontent.com/ethereum/solidity/v0.8.30/docs/examples/blind-auction.rst) | events | `bytes32`, loops, `keccak256`, internal call |
| `Ownable` | [OpenZeppelin v5.0.0](https://raw.githubusercontent.com/OpenZeppelin/openzeppelin-contracts/v5.0.0/contracts/access/Ownable.sol) | import of `Context` | `bytes`, internal calls in every function |
| `WETH9` | [canonical WETH9](https://raw.githubusercontent.com/gnosis/canonical-weth/master/contracts/WETH9.sol) | pragma 0.4 (the in-JVM solc is 0.8) | needs a 0.8 port (`real-world/WETH9.sol`) |

## Blockers, by the number of contracts they stop

Counted over all 13 sources above, benchmarked or not, this ranks the backlog of
`docs/taclet-ideas.md` by what real code needs.

| Blocker | Contracts | Status |
|---|---|---|
| events / `emit` | 7: Coin, ERC20, SimpleAuction, Purchase, BlindAuction, Ownable, WETH9 | parser rejects `EventDefinition` |
| custom errors | 5: Coin, SimpleAuction, Purchase, BlindAuction, Ownable | parser rejects `ErrorDefinition` |
| internal calls | 4: ERC20, Ownable, Ballot, BlindAuction | stuck (`docs/bugs.md`) |
| `return e;` / tuple return | 4: ERC20, Todos, SimpleAuction, BlindAuction | no rule |
| `address(this).balance` | 3: EtherWallet, Purchase, WETH9 | NPE in `SolJSONParser.parseIdentifier` |
| `bytes32` / `bytes` | 3: Ballot, BlindAuction, Ownable | no `KeYSolidityType` |
| struct constructors | 3: Todos, Ballot, BlindAuction | parser rejects `Todo(..)` |
| imports | 2: ERC20, Ownable | only the opened file is given to solc |
| loops | 2: Ballot, BlindAuction | no rule |
| `require(c, "msg")` | 2: EtherWallet, Ballot | "Not yet supported literal" |
| `block.timestamp` | 2: SimpleAuction, BlindAuction | spec language has no `block` |
| `send` | 2: SimpleAuction, BlindAuction | no rule |

Events, custom errors and `require` messages have no effect on the state the calculus
models. Accepting them in the parser (events and errors skipped, `emit` a no-op, the message
ignored) is the cheapest step. It leaves `Coin` and `SimpleAuction` with nothing else in the way
of loading verbatim, and removes most of the edits to `Purchase` and `ERC20`. After that come
`address(this).balance` (a crash, and cheap to route to `selfBalance`) and internal calls.
