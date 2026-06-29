# Taclet Ideas (Backlog)

Constructs that `Solidity.g4` parses but that have **no symbolic-execution
taclet yet**, ordered simple → complex. This is a scratch backlog of *ideas*,
not a spec. For each item: the grammar rule, the program shape, and a one-line
note on the intended sequent transformation. Implement against the conventions
in `docs/key-taclets.md`; storage/memory shapes follow `docs/storage.md`.

Already done (for reference, do **not** re-add): local assign/decl, `+`,
`+=`, `==`, `assert`, `revert`, `++`/`--`, `delete`, and the storage/memory
read/write/copy/push/pop family. See `docs/taclets-implementation.md`.

## Tier 1 — Pure expression evaluation (easiest)

These mirror the existing `addition_unfold_*` / `boolEqualityAssignment`
rules: capture non-simple operands into fresh value locals, then assign the
logic-level result of the operator to the target var. Pattern to copy:
`v = se1 ⊕ se2;` ⇝ `{v := t1 ⊕ t2}`.

- **Arithmetic** (`BinaryOp`): `-`, `*`, `/`, `%`, `**`. One unfold+assign
  rule each, reusing `intHeader`/`intDiv` operators. `/` and `%` need a
  `den != 0` revert branch.
- **Relational** (`BinaryOp`): `!=`, `<`, `>`, `<=`, `>=`. Twins of `==`:
  `v = se1 < se2;` ⇝ `{v := \if(t1 < t2)\then(TRUE)\else(FALSE)}`.
- **Logical** (`BinaryOp`/`UnaryPrefix`): `&&`, `||`, `!`. Bool-typed twins of
  the relational rules; `&&`/`||` ideally short-circuit (sequence the rhs).
- **Unary prefix** (`UnaryPrefix`): `-x`, `+x`, `~x`. `v = -x;` ⇝ `{v := -t}`.
- **Bitwise** (`BinaryOp`): `&`, `|`, `^`, `<<`, `>>`. Need bitwise LDT
  operators; lower priority until int bit-ops are modelled.

## Tier 2 — Remaining compound assignments

Parsed but unimplemented (`storage.md` §7 desugars these pre-Step-3). Clone the
`+=` family (`storageRootAddAssign` / `…Field…` / `…Index…` + `_unfold_leftFst`)
for each operator at root/field/index level:

- `-=`, `*=`, `/=`, `%=` (`/=`, `%=` add a `den != 0` branch).
- `&=`, `|=`, `^=`, `<<=`, `>>=` (gated on Tier-1 bitwise support).

## Tier 3 — Control flow

- **`ifStatement`** (`if (c) s1 else s2`): the keystone rule. Evaluate the
  guard to a bool term, then split into `c ⇒ ⟨s1⟩φ` and `¬c ⇒ ⟨s2⟩φ`
  (else-less variant: second branch is `¬c ⇒ ⟨⟩φ`). Port KeY's
  `ifElseSplit`. Unblocks most real contract bodies.
- **`returnStatement`** (`return e;`): bind the function's named return value
  and discard the rest of the block. Pairs with `functionBodyStatement`
  inlining (`ExpandFunctionBody`).
- **`uncheckedStatement`** (`unchecked { … }`): execute the block under a flag
  that switches arithmetic to wrapping (no overflow revert branch). Initially
  just strip the wrapper and reuse the checked rules.
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
  `memoryArrayDeclFreshAlloc`).
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
  beyond the storage/memory heaps.
