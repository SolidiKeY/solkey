# Solidity Examples

`TestSuite.sol` holds the taclet examples. There are no `.key` problem files beside it: the
loader reads the contract, and for each function synthesizes

```
\programSource "<abs path>/TestSuite.sol";
\problem { \<{ someFunction()@TestSuite; }\>(true) }
```

so the whole specification lives in the Solidity body and every test program is real Solidity,
type-checked by `solc` on load.

## Running

```bash
./run-key.sh keyext.solidity.examples/TestSuite.sol                    # every function
./run-key.sh keyext.solidity.examples/TestSuite.sol testSimpleAssert   # one function
./gradlew :keyext.solidity.core:test --tests "*TacletStarterExamplesTest"
./gradlew :keyext.solidity.core:test --tests "*PaperTestExamplesTest"
```

`TacletStarterExamplesTest` runs the focused one-rule-each functions, `PaperTestExamplesTest`
the end-to-end `test*` ones. Both enumerate the contract, so a new function joins the suite by
being written — nothing has to be registered.

Check the contract with `solc --ast-compact-json keyext.solidity.examples/TestSuite.sol` before
running a suite.

## Writing an example

Every function is `public`, takes no arguments and returns nothing.

**What the test observes** goes in the body as `assert`. A value the postcondition talks about
is bound to a local first — `return e;` is not supported by the calculus:

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

### Directives

| Natspec tag | Effect |
|---|---|
| `/// @custom:key box` | box modality — `require` becomes an assumption |

`@custom:` is solc's extension prefix; any other tag is rejected as invalid documentation.

## Known gaps

Three shapes have no `assert` form and are not covered by any example:

- **"this always reverts"** — was `\[{ … }\](false)`. `require`'s box false-branch and
  out-of-bounds array access used to be checked this way.
- **The `net` payment ledger** (`docs/net.md`) — needs `\rules` blocks to define `CInv`,
  `\withOptions transferSemantics:withCallback`, and `msg.*` / `.transfer`, which
  `SolJSONParser` does not parse.
- **Whole-subtree equality** — `find<[int]>(storage, cons2(matrix, at(0))) = find<[int]>(storage,
  cons1(values))` compares two storage subtrees; Solidity cannot state it, and reading
  `matrix[0].length` back does not discharge. `storageIndexCopysourceAfterPush` therefore only
  checks that the copy-after-push executes.

Two smaller ones, worked around in place with a comment:

- a negative literal directly inside an assert condition (`assert(r == -5)`) does not
  discharge; bind it first (`int expected = -5;`);
- a popped slot is out of bounds, so `pop` clearing it is only observable after pushing again.

## Other directories

`fieldAccess/`, `functionBody/` and `newVariable/` still use `.key` problems — they exercise
loader and taclet-application details rather than program rules.
