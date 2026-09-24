# Taclet Ideas (Backlog)

Constructs that `Solidity.g4` parses but that have **no symbolic-execution
taclet yet**, ordered simple → complex. This is a scratch backlog of *ideas*,
not a spec. For each item: the grammar rule, the program shape, and a one-line
note on the intended sequent transformation. Implement against the conventions
in `docs/key-taclets.md`; storage/memory shapes follow `docs/storage.md`.

What is already implemented is **not** listed here — see
`docs/taclets-implementation.md` for that, and do not re-add it. In short: all
pure expression evaluation except bitwise, all arithmetic compound assignments
at storage/local/memory targets, `++`/`--`, `delete`, `if`/`if`-`else`, the
storage and memory read/write/copy/push/pop families, the capture partition,
and the `net` payment model.

## Tier 1 — Pure expression evaluation

- **Bitwise** (`BinaryOp`): `&`, `|`, `^`, `<<`, `>>` (and `~x`). Deferred —
  needs bitwise LDT operators; lower priority until int bit-ops are modelled.
  `+x` is skipped for good (Solidity ≥0.5 removed it; Java KeY has no rule).

## Tier 2 — Remaining compound assignments

- `&=`, `|=`, `^=`, `<<=`, `>>=`. Deferred — gated on Tier-1 bitwise support.

## Tier 3 — Control flow

- **`returnStatement`** (`return e;`): bind the function's named return value
  and discard the rest of the block. Pairs with `functionBodyStatement`
  inlining (`ExpandFunctionBody`).
- **`uncheckedStatement`** (`unchecked { … }`): with the unbounded-integer
  calculus arithmetic never reverts, so the wrapper carries no semantics — a
  rule can drop the block marker and execute the body as-is. Real wrapping
  semantics would only matter together with a bounded/checked integer model
  (see the Tier-5 entry below).
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
- **Bounded/checked integer semantics** (solc ≥ 0.8 overflow reverts): the
  calculus treats integers as unbounded mathematical integers, so the
  overflow-revert examples of `keyext.solidity.examples/unprovable/` are
  unprovable. A sound model would guard every arithmetic taclet with a
  per-type range check that reverts out of range (Java KeY's
  `inInt`/`expandInInt` structure), plus in-range PO antecedents for
  parameters and storage reads so examples need not `require` both bounds by
  hand. Done for array lengths: `sizeNotNegative` gives every storage
  length cell `0 <=` unconditionally (`docs/storage.md` §8b). Still open:
  `0 <=` for `uint` value cells (needs per-contract layout knowledge the
  calculus does not have), upper bounds (`< 2^256`), and parameters.
- **Address/payable builtins & globals** (`msg.sender`, `msg.value`,
  `block.*`, `.balance`, `.transfer`): require an environment/ledger model
  beyond the storage/memory heaps. Ordered implementation plan: `docs/net.md`.
  First slice done — `net` ledger, `msg.sender`/`msg.value`, and
  `transfer` in both callback semantics, now with the EVM balance check
  against `selfBalance` (see `docs/taclets-implementation.md` "Payments").
  Still open: `send`, `call{value:}`, `block.*`, and `address(this).balance`
  reading `selfBalance` in the parsers.

## Refinements to implemented rules

Edge cases of already-supported constructs (see `docs/taclets-implementation.md`):

- **Whole-struct write from a struct *value*** (`alice = pVal;`, vs. the
  supported root-to-root `alice = bob;`): needs Step-1 unfolding for struct
  constructors / memory-struct sources.
- **Reject uint unary minus in the parsers** (solc compile error); today the
  shape is executed as plain `neg` on the unbounded logic int instead of being
  rejected up front.
- **Ternary `CInv(storage, net, selfBalance)`**: needed only if an example ever
  wants to prove a *funded* transfer after a callback — the havoc currently
  leaves `selfBalance` unconstrained.

## Raised by the solc semantic-test ports

Found by porting the Solidity compiler's own semantic tests into
`keyext.solidity.examples/solc/` (`taclets-implementation.md`, "solc semantic-test ports").
Each has a failing example in the suite naming it, so closing the gap is observable.

- **Mapping members must be aliased before being indexed.** `nested.recursive[4].z` is open
  where `map[4].z` with `map = nested.recursive` closes; the member-mapping index rules need
  the same complex-receiver capture the other index families have.

## Raised by the mapping-index probe

Found by pushing mapping indexing into its odd corners (nested mappings, arrays of mappings,
storage pointers, memory-valued keys). Everything else in that sweep closes, except the
non-integer key crash in `docs/bugs.md` and the gap below.

- **An assignment used as an expression.** `balances[balances[1] = 2] = 7;` captures the index
  correctly and then gets stuck on `u = (balances[1] = 2);` — no rule consumes an assignment in
  value position. Related to `return e;` (Tier 3): both are expression forms the calculus only
  handles as statements.

## Raised in priority by the TestSuite.sol migration

- **`return e;` (Tier 3).** Now on the critical path: every example in `TestSuite.sol` has to
  use a *named* return and assign to it, because no taclet consumes a `ReturnStatement`. A
  companion fix belongs in `ExpandFunctionBody`, which currently wires only the first named
  return and silently drops the rest.

## Schema-variable sort cleanups (from the naming pass)

Names were aligned with their sorts across `solidityProgramRules.key` (`gp` →
`gsp`, `lp` → `lsv`, `mp` → `mv`, the over-general `Path[storage] sp` /
`Path[memory] mp` → `path` / `mpath`, the transfer receiver `a` → `sadr`).

A second pass then renamed the single letters whose spelling said nothing
about their role: the index expression `i` → `ie`, the field selector `a` →
`fld` (and the source field of the one two-field copy, `b` → `srcFld`), the
if-branches `s0` / `s1` → `thenStm` / `elseStm`, and the ternary arms `e1` /
`e2` → `thenExpr` / `elseExpr`. Five taclets that used `se` in an *index*
position were renamed to `ie` at the same time, so the name again follows the
role. `fld` also ends the shadowing of the global `\term Field a` declared in
`memoryRules.key` and `structRules.key`. The dead `mpath` row was dropped from
the convention table in `key-taclets.md` and the undocumented `lv` added.

`v` was deliberately kept: it is a program variable, and the bare letter is
the established name for one.  Note it still shadows the global `\term
MemValue v` / `\term StValue v` of `memoryRules.key` and `structRules.key`,
as `a` did before the rename.

Two sort-level oddities were found and deliberately left alone, because fixing
them changes what the taclets match:

- **`nlhs` is declared `Path[complex,primitive]`** in the four
  `..._unfold_rightSndResult` taclets — no data-area flag, so it matches
  storage *and* memory complex paths. Every other complex-path schema variable
  pins the area. Decide whether the cross-area match is intended, and if not,
  split the taclets by area.
- **`mv` is declared at two sorts**, `Variable[memory]` and
  `Path[memory,simple]`, in taclets of the same shape. Both match exactly one
  thing — a memory `ProgramVariable` — so the name is right either way, but
  one of the two declarations should win.

## Gaps left by the flat write decomposition

The three write rules (`docs/storage.md` §5) cover every `op(recv, i) = rhs`
shape. Two neighbouring statement forms are not yet as general:

- **A non-simple index under a compound assignment or an inc/dec**
  (`a[j++] += x`, `++a[f()]`) still has no capture rule: those unfolds take a
  `SimpleExpression` index, so the statement goes stuck. An impure *receiver*
  (`persons[i++].age += i`) is handled — the receiver unfolds snapshot the
  right-hand side first, exactly as Rule 1 does. Either add an index-capture
  rule per compound family, or desugar `a[i] += x` to `a[i] = a[i] + x` first.
- **Writing a storage reference into a memory location** (`mv.a = sp`,
  `mv[i] = sp`) has no rule, and neither has writing a *complex* memory
  reference into a storage location outside the receiver-capture path. A
  storage-to-memory element copy is a deep copy (`copySt`) and needs its own
  terminal, not just a capture.
