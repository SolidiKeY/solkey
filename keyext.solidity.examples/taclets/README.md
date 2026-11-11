# Taclet examples

One contract per file, one function per example, grouped by theme:

| File | Covers |
|---|---|
| `Expressions.sol` | operators, literals, conditionals and local variables |
| `RequireAssert.sol` | `require` and `assert` under box and diamond |
| `Storage.sol` | storage roots, struct fields, aliases, deletes and mappings |
| `StorageArrays.sol` | storage array indices, `push`, `pop`, fixed lengths and array copies |
| `Memory.sol` | memory values and copies between memory and storage |
| `EvaluationOrder.sol` | evaluation order of operands, receivers and indices |
| `Calls.sol` | internal calls, returns, tuples, parameters and file-level declarations |
| `Modifiers.sol` | modifiers and their placeholder continuations |
| `Loops.sol` | loops, unwound or with an invariant |
| `Payments.sol` | `transfer`, `send`, low-level calls and `try` |

The contract is named after its file, and a function name is unique across the directory, so a
test names an example by its function alone (`SolidityExampleTests.proveTacletFunction`). Each
file declares only the state variables, structs and helpers its functions use. There are no
`.key` problem files: the loader reads the contract and synthesizes one obligation per
function, so the whole specification lives in the Solidity body and every test program is real
Solidity, type-checked by `solc` on load. `docs/taclets-implementation.md` has the synthesized
obligation's exact shape.

A new example goes into the file of its theme; a new file joins every suite by being written.

## Runtime cross-checking

`SolidityRuntimeExecutionTest` compiles every file here and every `../semantics/*.sol` with
solc, deploys each contract on an in-process Besu EVM, and executes every provable function — a
closed proof must not hit a failing `assert` (Panic 0x01) when actually run. A parameterized
function runs with the values its leading `require` pins (an equality, or the tightest bound of
a range). Three kinds of case are skipped rather than failed:

- a box-tagged function whose `require` reverts on the fresh all-zero storage — vacuous at
  runtime, exactly as the box modality treats it;
- a parameterized function whose leading requires do not determine a value for every parameter;
- entries of the test's `KNOWN_DIVERGENT` set: examples proved with KeY's unbounded
  integers that panic under the EVM's checked arithmetic (currently none).

## Writing an example

Every function is `public` and returns nothing. Mark it `pure` or `view` when it touches no
storage or only reads it, so solc compiles the file without warnings; `./run-key.sh FILE.sol
--solc` lists what is left. A function may take arguments: the loader
declares one unconstrained program variable per parameter and passes them as the call's
arguments, so the function must be box-tagged and pin the values its asserts rely on in one
conjoined `require(x == 5 && y == 7)` — it plays the role of the old `.key` precondition
`x = 5 & y = 7`. Storage bounds stay in their own ordered requires (see below), since each one
guards the evaluation of the next.

**What the test observes** goes in the body as `assert`. A value the postcondition talks about
is bound to a local first, since every function returns nothing (internal helpers may return
values, and tuples of them, with `return`):

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

Array lengths themselves need no `require`: the `lenInv(storage)` every
obligation assumes makes `0 <= arr.length` available (`docs/storage.md` §8b), which
is what proves the untagged `storagePopUnknownLength`, `storagePushLengthPositive`
and `storagePushReadBack` on an otherwise unknown storage.

## Directives

| Natspec tag | Effect |
|---|---|
| `/// @custom:key box` | box modality — `require` becomes an assumption |
| `/// @custom:key skip` | no obligation for this function |
| `/// @custom:key invariant e` | on the contract: a conjunct of the contract invariant `CInv` |
| `/// @custom:key requires e` | assumed before the call (the old `only_if`) |
| `/// @custom:key ensures e` | proved after the call (the old `on_success`) |
| `/// @custom:key assignable …` | accepted and ignored: bodies are inlined whole |
| `/// @custom:key invariant e` | directly above a loop: its loop invariant (`docs/taclets-implementation.md`, "Loops") |
| `/// @custom:key decreases e` | directly above a loop: its variant, needed under the diamond |

`@custom:` is solc's extension prefix; any other tag is rejected as invalid documentation. A
tag starts a line and a clause runs to the next tag, so a long clause may continue on the next
`///` line. The last four make a function *specified* — see `../contracts/README.md` for
what that means and for the expression language.

## Known gaps

Three shapes have no `assert` form and are not covered by any example:

- **"this always reverts"** — was `\[{ … }\](false)`. `require`'s box false-branch and
  out-of-bounds array access used to be checked this way.
- **The `net` payment ledger** (`docs/net.md`) — needs a `\rules` block to define `CInv`
  and an obligation that books `msg.value`, which the plain `assert`-style obligation does
  not carry. Covered by the `.key` problems of `net/` (see `../net/README.md` below) and,
  since the `@custom:key` clauses exist, by the specified contracts of `../contracts/`, whose
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

