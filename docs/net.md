# The payment model (`net`)

The `net` ledger of Ahrendt & Bubel, *"Functional Verification of Smart
Contracts via Strong Data Integrity"* (ISoLA 2020, `paper.pdf`), as
implemented in this fork. The ledger, `msg.sender`/`msg.value`, both
`transfer` semantics and phase 1 of the contract invariant are implemented;
§4 lists what is left. Rules live in `solidityProgramRules.key` under the
"Rules for address payments" banner (`scripts/taclet.sh --index`); examples
and their proof obligations are in `keyext.solidity.examples/net/`.

Beyond the paper, the implemented rules add the EVM balance check as a
**diamond-only proof obligation**: the program variable `selfBalance`
models the contract's own funds, the diamond transfer rules owe
`0 <= v & v <= selfBalance` as a "sufficient funds" goal, while the box
rules book the debit unconditionally (a reverting run is trivially correct
under partial correctness, so the box does not owe the check). Both
modalities debit `selfBalance`, the callback havoc quantifies over
`selfBalance` alongside `storage` and `net`, and the PO pattern credits
`msgValue` to `selfBalance` next to the `net(msgSender)` booking.

Read `storage.md` (calculus conventions), `key-taclets.md` (authoring
syntax), and `require-assert.md` (box/diamond revert discipline) first.

## 1. What the Paper Adds

The paper's central device is a built-in mapping

    net : address → ℤ        net(a) = money received from a − money sent to a

maintained by the calculus, not by the program. On top of it:

- **Strong data integrity**: a contract invariant `I` relating fields to
  `net` must hold whenever control can leave the contract — after the
  constructor, at the end of every public function, and at every external
  call *after* funds have been passed — and may be assumed whenever control
  (re-)enters. This makes verified invariants immune to re-entrancy.
- **Proof obligation schema** (paper eq. 4), per public function `f`:

      msg.value ≥ 0 ∧ I ∧ pre →
        {old := storage}
        {net(msg.sender) := net(msg.sender) + msg.value}
        [result = f(msg, args)@C;] (I ∧ post)

  For non-payable `f`, `msg.value ≥ 0` is strengthened to `msg.value = 0`.
- **Two `transfer` rules**: with callbacks (prove `I` when control leaves,
  havoc storage, assume `I` on resume) and without callbacks (gas-limited
  `transfer` cannot call back; only the net update and a havoc of the
  callee's state remain).
- `require`/`assert`/`unfoldArgument` rules — **already implemented** here,
  in refined form (see `require-assert.md`; the box/diamond false-branch
  split is more precise than the paper's box-only treatment).

## 2. Current State vs. Paper

| Paper ingredient | Status in SolKey |
|---|---|
| `require` / `assert` / `revert` | Done, refined (`require-assert.md`) |
| Storage model | Done, richer than the paper's (paths, aliases, `storage.md`) |
| Unbounded ints ("Solidity Light") | Done (`intHeader.key`); bounded (checked) semantics is not modeled — see `docs/taclet-ideas.md` Tier 5 |
| `result = f(args)@C;` call statement | Parses (`Solidity.g4` `FunctionBodyStatement`); inlined by `ExpandFunctionBody`. The `functionBodyExpand` taclet is in the standard rule set (`solidityProgramRules.key`), together with `blockEmpty`, which discards the inlined body block |
| `address` type | Registered in `SolidityInfo`, mapped to the `int` sort |
| `net` mapping | **Done**: `Struct net` in `netHeader.key`, read/write via `selectSt`/`storeSt` |
| `msg.sender` / `msg.value` | **Done**: desugared to the `msgSender`/`msgValue` program variables in `SolidityToKeyConverter` |
| `transfer` / `send` / `call{value:}` | `transfer` **done with both semantics, each split by modality** (`transferNoCallbackBox`/`transferNoCallbackDiamond` and both capture rules; `transferWithCallbackBox`/`transferWithCallbackDiamond` under the `transferSemantics` choice); `send` has builtin + classification but no rule; `call{value:}` missing |
| Havoc update | **Done** in `transferWithCallbackBox`/`Diamond`: `{storage := storageSk \|\| net := netSk \|\| selfBalance := selfBalanceSk}` with skolem SVs (the `memoryReferenceDeclFreshAlloc` fresh-symbol pattern) |
| Contract invariant storage + retrieval | **Phase 1 done**: uninterpreted `CInv(Struct, Struct)` predicate (`netHeader.key`) expanded by a per-example `insertCInv` taclet. Repository-backed retrieval still missing; the loop-invariant machinery (`SpecificationRepository`, `\getInvariant`/`\hasInvariant` varconds in `TacletBuilderManipulators`) is the exact template |
| Proof-obligation generator | **Missing**; `proof/init/` already has `AbstractPO` / `ContractPO` / `FunctionalOperationContractPO` scaffolding. The paper's prototype also wrote POs by hand, so a manual pattern is faithful for phase 1 |

## 3. Design Decisions

**`net` is a second `Struct`-sorted program variable.** Declare
`Struct net;` next to `Struct storage;`. Since `address` is the `int`
sort, `net(sadr)` is expressed with the existing struct primitives:

    read   :  selectSt<[int]>(net, at(sadr))
    write  :  {net := storeSt(net, at(sadr), selectSt<[int]>(net, at(sadr)) + v)}

Everything needed already exists in `structRules.key`: read-over-write
(`selectOnStore`), and `selectOnEmpty` + `defaultValue<[int]> = 0`, which
gives the paper's implicit "initially `net(sadr) = 0`" from `net = mtSt` in the
constructor PO. Zero new sorts, functions, or simplification rules.

Rejected alternatives: a reserved field inside `storage` (would entangle the
ledger with contract data — `delete`, whole-storage havoc, and `old` must
treat them differently); a rigid function `net(int)` (rigid symbols cannot
be assigned by updates).

**`msg.sender` / `msg.value` are two int program variables** (`msgSender`,
`msgValue`), desugared from the `msg.x` member access in
`SolidityToKeyConverter`. They stay constant during one external call, which
is correct: internal calls inlined by `ExpandFunctionBody` preserve `msg` in
Solidity, and re-entrant external executions are never symbolically executed
(they are havocked), so no save/restore stack is needed. The paper's extra
`msg` function argument is then only a PO-level convention we do not need.

**`transfer` semantics is a KeY choice.** The paper's tool offers "with
callbacks" and "without callbacks" as user-selectable calculus variants; map
this to an option in `optionsDeclarations.key` (e.g.
`transferSemantics:withCallback` / `noCallback`) so both rule sets can
coexist and examples pin the variant they need.

**POs use the box modality.** The paper proves partial correctness
(reverting runs are trivially correct). `require` already degenerates to
`c → φ` in box (`require-assert.md` §5), which is exactly the paper's
`require` rule. The transfer rules are split by modality along the same
line: the box rules carry no funds guard at all — the debit is booked
unconditionally, exactly the paper's rule — while the diamond rules owe
`0 <= v & v <= selfBalance` as an explicit "sufficient funds" goal, which
is the EVM's actual behavior under total correctness
(`net-transfer-unfunded.key` pins the unconditional box booking, the
`net-transfer-*diamond-funded.key` starters discharge the diamond
obligation, and the `examples/open/` twins in the core test resources pin
that an unfunded diamond stays open).

## 4. Remaining work

Mapped to this repository, roughly in order of usefulness:

1. **Promote `functionBodyExpand`** from the per-example declaration into
   `solidityProgramRules.key`, and add an argument-capture rule
   (`unfoldArgument` in the paper) so `f(nse)@C` hoists complex arguments
   into fresh locals left-to-right. `ExpandFunctionBody` already declares
   fresh formals initialized with the actuals, so only the capture step is
   missing.
2. **Modifiers** (`inMode(m)`, `notBy(c)`, placeholder `_;`, paper §5):
   desugar at parse time in `SolidityToKeyConverter` by splicing the
   modifier body around the function body (substituting `_;`), the same way
   push-lvalue is desugared in `ParserUtils.parseAssignmentMaybe`. No new
   taclets: the spliced `require`s are handled by existing rules. Needed for
   OneAuction.
3. **Enums** (`AuctionMode`): register as int-backed types in
   `SolidityInfo`, lower members to int literals at parse time. Needed for
   OneAuction.
4. **Specification language in `.sol` comments** (`/*@ invariant …;
   requires …; after_success …; @*/`, `\old`, `net(...)` as spec syntax):
   parse from the solc AST `documentation` nodes in `SolJSONParser` (see
   `solc-ast.md`), build `speclang/` objects, register in
   `SpecificationRepository`. `net(a)` in specs lowers to
   `selectSt<[int]>(net, at(sadr))`; `\old(e)` lowers against the `old`
   variable. This feeds both the callback varcond and the PO generator.
5. **`send` and `call{value: v}(data)`**: `b = a.send(v)` is `transfer`
   that binds `false` instead of reverting — two branches (success: net
   update + `b := TRUE`; failure: `b := FALSE`, no net change), each
   followed by the callback/no-callback treatment. `call` always permits
   callbacks (paper Remark 1), so it only gets the with-callback rule;
   its `data` payload stays opaque.
6. **Control flow for the remaining examples**: Tier-3 `if` (escrow,
   closeAuction) and loop rules. The loop-invariant infrastructure
   (`LoopSpecification`, `\getInvariant`, `\getVariant`) already exists —
   wire it to `whileStatement` before attempting `closeAuction`'s
   reimbursement loop, whose loop invariant must itself speak about `net`.
7. **Bounded (round-robin) integers**: not modeled — the calculus keeps the
   paper's unbounded "Solidity Light" integers. A bounded/checked model
   (per-type range guards reverting out of range, KeY-Java's
   `inInt`/`expandInInt` structure) is backlogged in `docs/taclet-ideas.md`
   Tier 5, orthogonal to `net`.
8. **Storage/memory distinction**: already **ahead of the paper** — the
   paper's "Solidity Light" collapses them, while this repo implements both
   (`storage.md`, `memory.md`). Nothing to do.
9. **Automatic PO generation + GUI selection** ("mature verification
   system" outlook, paper §4.1): phase 2 of the PO generator, plus surfacing contract
   specifications in `key.ui` the way Java contracts are shown. Farthest
   out; everything before it works with hand-written `\problem`s.

The paper's own limitations to keep in mind when porting examples: it
verifies partial correctness only (box), assumes `transfer`'s gas stipend
for the no-callback variant (a callee can still be re-entered through
`call`), and its Table-1 auction examples deliberately contain seeded bugs —
open proofs there are the expected result, not a regression.

## Why the `net-*` examples call `PiggyBankNet.sol` instead of `TestSuite.sol`

Every other example lives in `keyext.solidity.examples/TestSuite.sol` and is called as
`f()@TestSuite`. The `net` examples call functions of the dedicated
`keyext.solidity.examples/net/PiggyBankNet.sol` instead, because solc's typing rules would
leak into the shared contract: `msg.value` is only allowed in a `payable` function, and
`.transfer` requires `address payable` receivers. `SolJSONParser` handles both forms since
`parseMemberAccess` learned them (an identifier base named `msg` with a negative
`referencedDeclaration` desugars to the `msgSender`/`msgValue` program variables of
`netHeader.key`; `transfer`/`send` resolve to the `SolidityInfo` builtins, typed
`VOID`/`BOOL` in `inferFunctionCallType`).
