# Solidity examples

| Directory | Holds | Checked by |
|---|---|---|
| `taclets/` | one function per taclet example, grouped by theme (`taclets/README.md`: how to write one) | `TacletStarterExamplesTest`, `PaperTestExamplesTest`, `SolidityRuntimeExecutionTest` |
| `coverage/` | saved proofs for the taclets no `taclets/` function reaches | `TacletCoverageTest` |
| `semantics/` | ports of the Solidity compiler's own tests; `open/` keeps what does not close yet | `SolcSemanticsExamplesTest`, `OpenExamplesStayOpenTest` |
| `contracts/` | contracts specified in `@custom:key` clauses: solidiKeY's, published ones, and a benchmark; `solidikey-plain/` holds the `net` scenario contracts with their `.key` obligations | `ContractExamplesTest`, `OpenExamplesStayOpenTest`, `scripts/benchmark.sh`, `NetExamplesTest` |
| `negative/` | what the calculus does not model (`unprovable/`), true claims that must stay open because no clean storage tail is assumed (`no-clean-tail/`) and what solc rejects (`illegal/`) | `NoCleanTailExamplesStayOpenTest`, `IllegalExamplesCompileTest` |

Each directory's `README.md` describes it. The directory names are constants of the test class
`ExampleDirs` and of `scripts/examples-dirs.sh`; a move updates both. `docs/limitations.md`
explains what the `open/` directories keep. `AGENTS.md` has the commands to run any of it.

## Step budgets

The example tests prove each function with no timeout and a step budget that depends only on
its source: `StepBudget.estimate`, 60 rule applications per byte of the function's
declaration, clamped to 10 000..100 000. So a result never depends on the machine. A dense
proof such as `NestedArrays.dynamicCubeMemoryToStorageIsDeep` needs about 10 steps per byte, so the budget
leaves headroom; a proof that closes only with more steps fails with `STEP_LIMIT`, and
`./run-key.sh FILE.sol --json` shows the count it needs.

Default-value assertions about unknown storage tails are checked to stay open, without adding
a clean-tail invariant or changing the assertions.

