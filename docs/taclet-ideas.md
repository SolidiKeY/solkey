# Taclet Ideas (Backlog)

Constructs that `Solidity.g4` parses but that have **no symbolic-execution
taclet yet**, ordered simple → complex. This is a scratch backlog of *ideas*,
not a spec. For each item: the grammar rule, the program shape, and a one-line
note on the intended sequent transformation. Implement against the conventions
in `docs/key-taclets.md`; storage/memory shapes follow `docs/storage.md`.

Already done (for reference, do **not** re-add): local assign/decl, `+`,
`+=`, `==`, `assert`, `require`, `revert`, `++`/`--`, `delete`, the
storage/memory read/write/copy/push/pop family, the non-simple RHS/index
capture partition (`docs/taclets-implementation.md` §Capture partition),
**all of Tier 1 below except the deferred
bitwise / unary-plus / short-circuit items**, and the **Tier 2 arithmetic
compound assignments `-=`, `*=`, `/=`, `%=`**. See
`docs/taclets-implementation.md`.

## Tier 1 — Pure expression evaluation (easiest)

These mirror the existing `addition_unfold_*` / `boolEqualityAssignment`
rules: capture non-simple operands into fresh value locals, then assign the
logic-level result of the operator to the target var. Pattern to copy:
`v = se1 ⊕ se2;` ⇝ `{v := t1 ⊕ t2}`.

- **Arithmetic** (`BinaryOp`): `-`, `*`, `/`, `%`, `**`. ✅ Done — see
  `docs/taclets-implementation.md` ("Tier 1 expression operators"). `/` and `%`
  revert on a zero denominator.
- **Relational** (`BinaryOp`): `!=`, `<`, `>`, `<=`, `>=`. ✅ Done — twins of
  `==`: `v = se1 < se2;` ⇝ `{v := \if(lt(t1, t2))\then(TRUE)\else(FALSE)}`.
- **Logical** (`BinaryOp`/`UnaryPrefix`): `&&`, `||`, `!`. ✅ Done —
  both-simple operands, left-operand capture, and right-operand short-circuit
  (`logicalAnd/OrShortCircuitRhs` split directly at the sequent level, the
  Solidity analog of Java KeY's `compound_assignment_3/5_nonsimple` if-else
  rewrite; no `if` rule needed).
- **Unary prefix** (`UnaryPrefix`): `-x` ✅ Done (`v = -x;` ⇝ `{v := -t}`).
  `+x` is skipped (Solidity ≥0.5 removed it; Java has no rule). `~x` is bitwise
  (see below).
- **Bitwise** (`BinaryOp`): `&`, `|`, `^`, `<<`, `>>` (and `~x`). ⏳ Deferred —
  need bitwise LDT operators; lower priority until int bit-ops are modelled.

## Tier 2 — Remaining compound assignments

Clone the `+=` family (`storageRootAddAssign` / `…Field…` / `…Index…` +
`_unfold_leftFst`) for each operator at root/field/index level:

- `-=`, `*=`, `/=`, `%=`. ✅ Done — twins of `+=` with the infix operator swapped;
  `/=`, `%=` add a `se != 0` revert branch on the terminals. See
  `docs/taclets-implementation.md` ("Compound assignment operators").
- `&=`, `|=`, `^=`, `<<=`, `>>=`. ⏳ Deferred — gated on Tier-1 bitwise support.

## Tier 3 — Control flow

- **`ifStatement`** (`if (c) s1 else s2`): the keystone rule. Evaluate the
  guard to a bool term, then split into `c ⇒ ⟨s1⟩φ` and `¬c ⇒ ⟨s2⟩φ`
  (else-less variant: second branch is `¬c ⇒ ⟨⟩φ`). Port KeY's
  `ifElseSplit`. Unblocks most real contract bodies.
- **`returnStatement`** (`return e;`): bind the function's named return value
  and discard the rest of the block. Pairs with `functionBodyStatement`
  inlining (`ExpandFunctionBody`).
- **`uncheckedStatement`** (`unchecked { … }`): execute the block under a flag
  that switches arithmetic to wrapping (no overflow revert branch). Checked
  arithmetic itself is implemented (`intRules:soliditySemantics`, see
  `docs/taclets-implementation.md` "Checked arithmetic"); this item is the
  wrapping escape hatch. Since the overflow guard is now the single
  `#inBounds`/`inUintN` predicate layer, a first cut can rewrite the
  in-bounds predicates originating inside the block to `true` (the
  `ignoreOverFlow` expansion, scoped); real wrapping needs a `wrap`/modulo
  function like Java KeY's `moduloInt`.
- **`whileStatement`** / **`forStatement`** / **`doWhileStatement`**: loop
  unrolling rule (one iteration + residual loop) for bounded proofs, plus an
  invariant rule later. `for` first desugars init/cond/update into a `while`.
- **`breakStatement`** / **`continueStatement`**: only meaningful with the loop
  rules; model via abrupt-completion markers like KeY's Java loop scope.

## Tier 4 — Calls, types, events

- **`emitStatement`** (`emit E(args);`): no state effect in the core memory
  model — likely a skip rule that evaluates args and continues. Needed so
  bodies that emit events don't get stuck.
- **`NewInstance`** (`new T[](n)`, `new T`): allocate a fresh memory array /
  struct of the given length and bind the reference (extends
  `memoryArrayFreshAlloc`).
- **`ObjectInit`** (`Token(42)`, `Token{value: 42}`): struct-literal
  construction. Blocks the disabled `testStorageArrayPushPop`
  (`tokens.push(Token(42));`). Needs a struct-value source for the existing
  copy/push-source rules.
- **Cast / `elementaryTypeName` conversion** (`uint8(x)`, `address(0)`): width
  truncation / sign handling on the logic value; reuse `cast.key`.
- **`functionCall` on non-inlined callees**: beyond `ExpandFunctionBody` —
  external calls (`a.call`, `transfer`, `send`) and their value/revert effects.

## Tier 5 — Harder / out of current scope

- **`SliceAccess`** (`a[start:end]`): array-slice value; no calculus support
  yet.
- **`tryStatement` / `catchClause`**: exception-handling control flow; depends
  on a revert/exception model richer than `revert();`.
- **Bitwise & fixed-point arithmetic** (`Fixed`/`Ufixed`, full bit-ops):
  needs new LDTs.
- **Address/payable builtins & globals** (`msg.sender`, `msg.value`,
  `block.*`, `.balance`, `.transfer`): require an environment/ledger model
  beyond the storage/memory heaps. Ordered implementation plan: `docs/net.md`.
  ✅ First slice done — `net` ledger, `msg.sender`/`msg.value`, and
  `transfer` in both callback semantics, now with the EVM balance check
  against `selfBalance` (see `docs/taclets-implementation.md` "Payments").
  Still open: `send`, `call{value:}`, `block.*`, and `address(this).balance`
  reading `selfBalance` in the parsers.

## Refinements to implemented rules

Edge cases of already-supported constructs (see `docs/taclets-implementation.md`):

- **Whole-struct write from a struct *value*** (`alice = pVal;`, vs. the
  supported root-to-root `alice = bob;`): needs Step-1 unfolding for struct
  constructors / memory-struct sources.
- **Dynamic-array `delete arr;` length reset**: not modeled by the current
  memory/storage delete rules. (Struct-`delete` preserving mapping members is now
  implemented via the lazy `delNode` marker — see `docs/storage.md` §6.)
- **In-range PO antecedents under `intRules:soliditySemantics`**: synthesize
  `min <= x <= max` assumptions for parameters and storage reads when the
  checked option is active, so examples need not `require` both bounds by hand.
- **`mapfree` PathSVSort flag**: optional calculus-level hardening of the
  storage copy taclets against mapping-carrying sources; today the front ends
  reject the illegal programs before any rule can see them.
- **Reject uint unary minus in the parsers** (solc compile error); today the
  shape gets its ordinary `#inBounds` range check under the checked option
  (reverting unless the operand is 0) instead of being rejected up front.
- **Ternary `CInv(storage, net, selfBalance)`**: needed only if an example ever
  wants to prove a *funded* transfer after a callback — the havoc currently
  leaves `selfBalance` unconstrained.

## Raised by the solc semantic-test ports

Found by porting the Solidity compiler's own semantic tests into
`keyext.solidity.examples/solc/` (`taclets-implementation.md`, "solc semantic-test ports").
Each has a failing example in the suite naming it, so closing the gap is observable.

- **`SolJSONParser`: self-recursive struct types.** `struct s2 { mapping(k => s2) recursive; }`
  throws an NPE in `getOrCreateMappingKeYSolidityType` and takes the whole file down at load.
  Worked around in the ports by unrolling the hierarchy.
- **Mapping members must be aliased before being indexed.** `nested.recursive[4].z` is open
  where `map[4].z` with `map = nested.recursive` closes; the member-mapping index rules need
  the same complex-receiver capture the other index families have.

✅ Fixed by the port (examples kept as regression tests, details in
`taclets-implementation.md`, "Rules added or corrected by the solc port"): `++`/`--` and
compound assignment on a local, a non-simple RHS in a compound assignment at any location,
indexing a storage alias of a primitive-element array, the ill-sorted whole-value copy into
an array or mapping element, the `?:` type bug in `SolJSONParser.parseConditional`, and
`push` leaving the appended slot symbolic instead of clearing it with `delValue`.

## Raised in priority by the TestSuite.sol migration

- **`return e;` (Tier 3).** Now on the critical path: every example in `TestSuite.sol` has to
  use a *named* return and assign to it, because no taclet consumes a `ReturnStatement`. A
  companion fix belongs in `ExpandFunctionBody`, which currently wires only the first named
  return and silently drops the rest.
- **`msg.*` / `.transfer` / `.send` in `SolJSONParser`.** ✅ Done — `parseMemberAccess`
  desugars `msg.sender`/`msg.value` to the `msgSender`/`msgValue` program variables and
  resolves `transfer`/`send` to the builtin declarations, so `.sol` function bodies with
  those forms load; the `net/` examples now call real `PiggyBankNet.sol` functions
  (`f(args)@PiggyBankNet`). Still `.key`-based: the synthesized obligations cannot carry an
  `insertCInv` rules block or a taclet option. See `net.md`.
