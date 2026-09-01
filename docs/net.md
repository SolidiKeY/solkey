# Net Rules — Implementation Plan

Roadmap for implementing the `net` ledger model of Ahrendt & Bubel,
*"Functional Verification of Smart Contracts via Strong Data Integrity"*
(ISoLA 2020, `paper.pdf`) on top of the current SolKey calculus. Steps 1–4
are implemented (see the status table below and
`docs/taclets-implementation.md` "Payments"); the rest is the ordered backlog
for the payment/environment model that `taclet-ideas.md` Tier 5 defers
("`msg.sender`, `msg.value`, `.transfer` — require an environment/ledger
model beyond the storage/memory heaps").

Beyond the paper, the implemented rules add the EVM balance check: the
program variable `selfBalance` models the contract's own funds, `transfer`
reverts when `0 <= v & v <= selfBalance` fails and debits `selfBalance`
otherwise, the callback havoc quantifies over `selfBalance` alongside
`storage` and `net`, and the PO pattern credits `msgValue` to `selfBalance`
next to the `net(msgSender)` booking.

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
| `net` mapping | **Done** (Step 1): `Struct net` in `netHeader.key`, read/write via `selectSt`/`storeSt` |
| `msg.sender` / `msg.value` | **Done** (Step 2): desugared to the `msgSender`/`msgValue` program variables in `SolidityToKeyConverter` |
| `transfer` / `send` / `call{value:}` | `transfer` **done with both semantics** (Step 3: builtin + classification + `transferNoCallback` and both capture rules; Step 4: `transferWithCallback` under the `transferSemantics` choice); `send` has builtin + classification but no rule; `call{value:}` missing |
| Havoc update | **Done** in `transferWithCallback`: `{storage := storageSk \|\| net := netSk \|\| selfBalance := selfBalanceSk}` with skolem SVs (the `memoryReferenceDeclFreshAlloc` fresh-symbol pattern) |
| Contract invariant storage + retrieval | **Phase 1 done**: uninterpreted `CInv(Struct, Struct)` predicate (`netHeader.key`) expanded by a per-example `insertCInv` taclet. Repository-backed retrieval still missing; the loop-invariant machinery (`SpecificationRepository`, `\getInvariant`/`\hasInvariant` varconds in `TacletBuilderManipulators`) is the exact template |
| Proof-obligation generator | **Missing**; `proof/init/` already has `AbstractPO` / `ContractPO` / `FunctionalOperationContractPO` scaffolding. The paper's prototype also wrote POs by hand, so a manual pattern is faithful for phase 1 |

## 3. Design Decisions

**`net` is a second `Struct`-sorted program variable.** Declare
`Struct net;` next to `Struct storage;`. Since `address` is the `int`
sort, `net(a)` is expressed with the existing struct primitives:

    read   :  selectSt<[int]>(net, at(a))
    write  :  {net := storeSt(net, at(a), selectSt<[int]>(net, at(a)) + v)}

Everything needed already exists in `structRules.key`: read-over-write
(`selectOnStore`), and `selectOnEmpty` + `defaultValue<[int]> = 0`, which
gives the paper's implicit "initially `net(a) = 0`" from `net = mtSt` in the
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
`require` rule. The plan originally used this to omit an "unbacked funds"
branch from `transfer`; the implemented rules carry the branch explicitly
(`0 <= v & v <= selfBalance`, else `revert()`) — box still discharges the
reverting run for free, but diamond POs now owe the funds guard, which is
the EVM's actual behavior (the diamond starters carry `selfBalance`
antecedents, and `net-transfer-insufficient.key` pins the revert).

## 4. Next Steps to Implement `net`

Ordered; each step has a runnable milestone. Rules go in
`\rules(programRules:Solidity)` of `solidityProgramRules.key`.

> **`net` examples live in `keyext.solidity.examples/net/`.** Each `.key` problem there
> verifies a real function of a contract beside it by calling it in the modality
> (`\[{ makeBid()@AuctionNet; }\] (CInv(storage, net) & post)`): `SolJSONParser` desugars
> `msg.sender`/`msg.value` to the `msgSender`/`msgValue` program variables, resolves
> `.transfer`/`.send` to the builtin declarations, and unwraps `payable(...)` /
> `address(...)` casts (identity — addresses are ints), so the `.sol` bodies load and
> `functionBodyExpand` inlines them. The problems stay `.key`-based because their
> obligations need a problem-local `\rules { insertCInv … }`, `\withOptions
> transferSemantics:withCallback`, and the hand-written eq.-4 PO shape — none of which a
> synthesized `.sol` obligation can carry yet. The Step-6 course port is done for the
> loop-free examples: `PiggyBankNet` (the course PiggyBank1 state machine:
> state != Broken -> balance = net(owner), state == Broken -> net(owner) = 0), `EscrowNet` (escrow-v2
> conservation with state conditionals), `AuctionNet` (transfer-last makeBid under both
> semantics + closeAuction) and `AuctionWithdrawNet` (the effective_net withdrawal
> pattern; like the course, closeAuction has no PO — the stated invariant provably does
> not survive it), plus a `CasinoNet` conservation set of our own design. The `net-*`
> starters cover the raw ledger/transfer machinery. `NetExamplesTest` enumerates the
> directory. Still open from Step 6: the multi-auction `closeAuction` loop (needs loop
> invariants over `net`) and paper Table 1's expected-open buggy variants.

> **Status:** Steps 1–4 are implemented (`netHeader.key`, the converter
> desugaring, the `transfer` builtin + no-callback rules, and the
> with-callback rule); the seven `net-*` starter examples close. The
> `transferSemantics:{noCallback, withCallback}` choice exists in
> `optionsDeclarations.key` (default `noCallback`; examples select the other
> variant with `\withOptions transferSemantics:withCallback;`). Step 4 took
> the CInv-predicate route instead of the `inv` schema formula below:
> `netHeader.key` declares an uninterpreted `CInv(Struct, Struct)` over
> `(storage, net)`, `transferWithCallback` references it directly, and each
> problem file defines it via its own `insertCInv` rewrite taclet (phase 1 of
> the invariant plumbing; the `\getContractInvariant` varcond remains phase
> 2). One enabling fix along the way:
> `SolidityInfo.registerPredefinedTypes` now resolves *all* pending
> sort-only `KeYSolidityType`s (LDT headers may declare several program
> variables of the same sort, and primitives like `int msgSender;`).

### Step 1 — Declare the ledger state

1. Add `netHeader.key` under `proof/rules/`:

   ```key
   \programVariables {
       Struct net;
       int msgSender;
       int msgValue;
       int self;        // address of the contract under verification
       int selfBalance; // funds held by the contract under verification
   }
   ```

   Register it in `ldt.key`'s `\includeLDTs` (after `structHeader`, whose
   `Struct` sort it uses). No Java LDT class is needed for parsing; when
   Java-side code later needs the variables (converter, PO generator), add
   name constants to `StructLDT` following `STORAGE_NAME`.
2. `self` is the paper's `this`: the `c ≠ this` side condition of the
   transfer rules and invariants like OneAuction's `address(this) != owner`
   need it. It is just an int PV the PO constrains.

**Milestone:** a `.key` problem that manually applies
`{net := storeSt(net, at(1), selectSt<[int]>(net, at(1)) + 5)}` and proves
`selectSt<[int]>(net, at(1)) = 5 & selectSt<[int]>(net, at(2)) = 0` from
`net = mtSt` closes using only existing struct rules (this was
`net-manual-update.key`; once the transfer rules landed, the hand-written
update became derivable and the example was folded into
`net-transfer-simple.key`, whose postcondition checks both the booked slot
and an untouched one).

### Step 2 — `msg.sender` and `msg.value`

1. In `SolidityToKeyConverter`, resolve the `MemberAccess` with base
   identifier `msg` and member `sender`/`value` to the `msgSender` /
   `msgValue` program variables (type `address` resp. `uint`). `msg` itself
   is never a value; any other member is an error for now.
2. `SolJSONParser` needs the same mapping for `.sol` sources (the solc AST
   marks these as `MemberAccess` on the builtin `msg` global). ✅ Done, together
   with `transfer`/`send` builtin resolution, in `parseMemberAccess` /
   `inferFunctionCallType`.

**Milestone:** `\<{ x = msg.value; }\>(x = msgValue)`-style example closes
(no new taclets — `msg.value` is a simple expression after desugaring).

### Step 3 — `transfer` statement and the no-callback rule

**Java-side pipeline.** `a.transfer(v);` reuses the push/pop plumbing
end-to-end; no new AST node and no grammar change (`MemberAccess` +
`FunctionCallExp` + `ExpressionStatement` already cover the shape):

1. **Builtin registration** — add `transfer` (return `VOID`, one `uint`
   parameter) and `send` (return `BOOL`; its rule comes later, §5) to
   `SolidityInfo.createBuiltinFunctions`, next to `push`/`pop`.
2. **Member resolution** — `SolidityToKeyConverter.visitMemberAccess`
   resolves a builtin member name to a
   `MemberExp(receiver, builtinFunctionDecl, type)`; the gate is currently
   hard-coded to `push`/`pop` — extend it with `transfer`/`send`. Keep any
   receiver-type check lenient: in taclet patterns the receiver is a schema
   variable whose Solidity type is unknown at rule-parse time.
3. **Call classification** — nothing to do: `visitFunctionCallExp` already
   turns a `MemberExp` whose right side is a `FunctionDeclaration` into a
   `FunctionCallExpression`, and `inferBuiltinMemberCallType` falls through
   to the declaration's own type (`VOID`) for non-push names. As a
   statement, the call sits in the existing `ExpressionStatement`.
4. **Taclet matching comes free** — the `\find` pattern
   `s#a.transfer(s#se);` is parsed by the same converter in schema mode:
   `s#a` is an SV receiver, the literal member name `transfer` resolves to
   the same builtin `FunctionDeclaration` object, so structural matching
   works exactly as it does for `s#sp.push(s#se);` today. Receiver SV sort:
   `SimpleExpression[primitive]` (matches literals and primitive-typed
   program variables — an address-typed local or literal); nonsimple
   receivers are handled by the capture rule below, not the sort.
5. **`.sol` sources** — mirror the same builtin mapping where
   `SolJSONParser` resolves member calls from the solc AST, so bodies
   loaded from Solidity files classify `transfer` identically to bodies
   written inside a modality.
6. **Smoke-test the front end** before writing any rule: a `.key` file
   whose modality contains `a.transfer(1);` must load with `--no-prove`
   (parse + classification), and pretty-printing must survive a proof save
   (`Layouter` handles the statement generically; verify, don't assume).

**`transferNoCallback`** (plain English first):

- *Precondition:* the active statement is `a.transfer(v);` where `a` is a
  simple address-typed expression, `v` a simple uint expression, and the
  recipient is known not to be the contract itself (`a ≠ self`).
- *Transformation:* consume the statement and emit the ledger update — the
  contract has now sent `v` to `a`, so `net(a)` decreases by `v`. Symbolic
  execution continues immediately: a gas-limited `transfer` cannot call
  back, and the callee's own storage is not modeled, so the paper's
  `havoc(c)` is a no-op here.
- *Postcondition:* the continuation runs with all knowledge intact except
  the updated `net(a)`; the invariant is *not* consulted.

```key
transferNoCallback {
    \schemaVar \formula post;
    \schemaVar \program SimpleExpression[primitive] a;
    \schemaVar \program SimpleExpression[primitive] se;

    \find(\modality{#mod}{c# s#a.transfer(s#se); #c}\endmodality(post))
    \replacewith(\if(0 <= se & se <= selfBalance)
        \then({selfBalance := selfBalance - se
               || net := storeSt(net, at(a),
                                 selectSt<[int]>(net, at(a)) - se)}
            \modality{#mod}{c# #c}\endmodality(post))
        \else(\modality{#mod}{c# revert(); #c}\endmodality(post)))
    \heuristics(simplify_prog)
};
```

(The guard is the EVM value-transfer check: the implemented rule extends the
paper's, which could not fail. A negative amount is folded into the same
revert branch — unsigned in the EVM's value field.)

(Guard the rule set with the `transferSemantics:noCallback` choice. The
`a ≠ self` side condition can start as a proof obligation added via a second
`\replacewith`-branch or be deferred until `self` is used by POs.)

Capture rules mirror the push family (receiver before argument,
left-to-right): `transfer_unfold_receiver` hoists a nonsimple receiver
(`nse.transfer(e);` ⇝ `address r = nse; r.transfer(e);`, via `\newTypeOf`),
`transfer_unfold_argument` hoists a nonsimple amount
(`a.transfer(nse);` ⇝ `uint pv = nse; a.transfer(pv);`, cf.
`storagePushValue_unfold_rightSndArgument`).

**Milestone:** a piggy-bank-style body (`require` + storage write +
`transfer`) closes in box modality against a hand-written PO
(see Step 5). This was `net-transfer-simple.key`.

### Step 4 — Havoc and the with-callback rule

Front-end-wise this step is free: the rule matches the same
`ExpressionStatement` shape as Step 3, so all Java-side work is the
invariant varcond described below.

**`transferWithCallback`** (plain English first):

- *Precondition:* same statement shape as Step 3, and a contract invariant
  `I` is available for the contract under verification.
- *Transformation:* split into three branches. Branch 0 ("insufficient
  funds"): when the guard `0 <= v & v <= selfBalance` fails, the transfer
  reverts before control leaves. Branch 1 ("control leaves"): under the
  guard, after debiting `selfBalance` and decrementing `net(a)` by `v` —
  funds move *before* control does — the invariant `I` must be proven; the
  rest of the program is discarded. Branch 2 ("control returns"): the callee
  may have done anything, including re-entering this contract, so all
  knowledge of `storage`, `net` *and* `selfBalance` is erased (fresh skolem
  constants); under the assumption that `I` holds again, symbolic execution
  continues.
- *Postcondition:* branch 1 guarantees the contract is consistent whenever
  the callee (or any re-entrant execution) runs; branch 2 resumes with only
  `I` known about storage and ledger (and nothing about the balance).
  Local/memory state survives: the callee cannot touch this call frame.

```key
transferWithCallback {
    \schemaVar \formula post, inv;
    \schemaVar \program SimpleExpression[primitive] a;
    \schemaVar \program SimpleExpression[primitive] se;
    \skolemTerm Struct storageSk;
    \skolemTerm Struct netSk;
    \skolemTerm int selfBalanceSk;

    \find(\modality{#mod}{c# s#a.transfer(s#se); #c}\endmodality(post))
    \varcond(\getContractInvariant(inv))
    "insufficient funds":
        \replacewith(!(0 <= se & se <= selfBalance)
            -> \modality{#mod}{c# revert(); #c}\endmodality(post));
    "invariant on exit":
        \replacewith(0 <= se & se <= selfBalance
            -> {selfBalance := selfBalance - se
                || net := storeSt(net, at(a),
                                  selectSt<[int]>(net, at(a)) - se)} inv);
    "resume after callback":
        \replacewith(0 <= se & se <= selfBalance
            -> {storage := storageSk || net := netSk
                || selfBalance := selfBalanceSk}
               (inv -> \modality{#mod}{c# #c}\endmodality(post)))
    \heuristics(simplify_prog)
};
```

Note one deliberate deviation: the paper's rule writes `havoc(storage)`
only, but its `I` mentions `net`, and a re-entrant call changes `net`;
havocking both — plus `selfBalance`, which the callee can move funds into
or out of — is the sound reading of "eliminate all knowledge about the
whole storage". `CInv` stays binary over `(storage, net)`: after a callback
nothing is known about `selfBalance`, so a proof of a *funded* transfer
after a callback would need the ternary extension (`taclet-ideas.md`).

Invariant plumbing, in two phases:

1. **Manual:** declare the rule *in the example `.key` file* with `inv`
   spelled out literally (exactly how `insertCInv` is declared in
   `net-transfer-withcallback-simple.key` did). This validates the
   rule shape with zero infrastructure.
2. **Varcond:** add a `ContractSpecification` to `speclang/`, store it in
   `SpecificationRepository`, and register `\getContractInvariant` in
   `TacletBuilderManipulators`, cloning the `LoopInvariantCondition` /
   `\getInvariant` pattern. Populate it first from a `.key`-file section
   (extend `KeYUserProblemFile`), later from `/*@ invariant … @*/` comments
   in the `.sol` source (§6).

**Milestone:** the paper's `makeBid` counterexample behaves as in Table 1 —
the fixed body (transfer last, paper Fig. 5) closes under
`transferWithCallback`; the original body (paper Fig. 4) closes only under
`transferNoCallback` and leaves the "invariant on exit" branch open under
`transferWithCallback`.

### Step 5 — Proof obligations

Phase 1 is a documented **manual pattern** (faithful to the paper's
prototype). For contract `C`, function `f`, invariant `I`, pre/post
`pre`/`post`:

```key
\programVariables { Struct old; }

\problem {
       geq(msgValue, 0)             // msgValue = 0 for non-payable f
     & I & pre
  -> {old := storage}
     {net := storeSt(net, at(msgSender),
                     selectSt<[int]>(net, at(msgSender)) + msgValue)
      || selfBalance := selfBalance + msgValue}
     \[{ result = f(args)@C; }\] (I & post)
}
```

- `I` and `post` are formulas over `storage`/`net` (they automatically
  denote post-state values under the modality); `\old(e)` in the paper is
  `e` with `storage` replaced by `old`.
- Constructor PO: start from `{storage := mtSt || net := mtSt}`, then the
  incoming-payment update, then `[constructor body] I`. `selectOnEmpty`
  yields `net(a) = 0` for all untouched `a`.
- Distinctness assumptions involving `self` (e.g. `self != msgSender`) go
  into the antecedent, mirroring OneAuction's `address(this) != owner`.

Phase 2 automates this in `proof/init/` (a `ContractInvariantPO` /
function-contract PO next to `AbstractPO`), consuming the
`ContractSpecification` of Step 4. This goes beyond the paper's prototype;
do it only after several hand-written POs have stabilized the pattern.

### Step 6 — Examples, tests, docs

- Port the paper's example suite in dependency order: **piggyBank**
  (`addMoney`/`break`: needs only Steps 1–5), then **OneAuction/makeBid**
  (additionally needs enums and modifiers, §6), then **escrow** (needs
  Tier-3 `if`), then **auction/closeAuction** (needs `for` + arrays + loop
  invariants).
- Reproduce paper Table 1: each example under both `transferSemantics`
  choices, expecting the same closed/open matrix.
- Add starters as functions of `keyext.solidity.examples/TestSuite.sol` (they are picked up
  automatically), update `keyext.solidity.examples/README.md`, move the implemented items
  from `taclet-ideas.md` Tier 5 into `taclets-implementation.md`.

Verification, as for all taclet work:

```bash
./run-key.sh keyext.solidity.examples/TestSuite.sol <function>
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.taclets.TacletStarterExamplesTest"
./gradlew :keyext.solidity.core:test   # after any Java-side change
```

## 5. Implementing the Rest of the Paper

What remains after §4, mapped to this repository. Roughly in order of
usefulness:

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
   `solidity-json-documentation.md`), build `speclang/` objects, register in
   `SpecificationRepository`. `net(a)` in specs lowers to
   `selectSt<[int]>(net, at(a))`; `\old(e)` lowers against the `old`
   variable. This feeds both the Step-4 varcond and the Step-5 PO
   generator.
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
   system" outlook, paper §4.1): Step 5 phase 2, plus surfacing contract
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

