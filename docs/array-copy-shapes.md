# Shape-directed copy and reset: a compiler-based proof proposal

`keyext.solidity.examples/contracts/ArrayCopyShapes.sol` contains executable witnesses.
All seven functions close with the existing SolKey rules and pass on the EVM with solc
`0.8.34+commit.80d5c536`. Three related regressions are in `TestSuite.sol`. These support a
generalization of the copy/reset model. The implementation keeps `save`, `delAt`,
`delNode`, and `delNodeFixed`; `C_T` and `R_T` below are mathematical notation rather than
new constructors. Fixed-array reset guards and the reference reset selector's old-length
comparison have been removed; complex storage push sources are captured before the update.
The argument below is a mathematical proof outline, not a machine-checked compiler theorem.

## What the compiler generates

The generated Yul for `big = small`, where `small` is `uint[3]` and `big` is `uint[5]`,
calls a fixed-target resize helper with `newLen = 3`. That helper reads the old target length
as the constant `5`, clears slots for indices `3` and `4`, and does not store a new length.
The copy helper then writes three source elements. A dynamic-target resize helper instead
writes the new length to storage before clearing the old tail.

For `uint[2][2] -> uint[3][3]`, the compiler first clears the destination's third row by
calling the clear helper for `uint[3]`. It then copies the first two rows recursively with
the `uint[2] -> uint[3]` helper. Each row copy clears its third element. Length getters
return constants at both fixed dimensions. Storage-to-storage copy helpers return immediately
when their source and destination slots are equal.

The generated operations come from Solidity's
[YulUtilFunctions.cpp at v0.8.34](https://github.com/argotorg/solidity/blob/v0.8.34/libsolidity/codegen/YulUtilFunctions.cpp):
`copyArrayToStorageFunction`, `copyValueArrayToStorageFunction`, `resizeArrayFunction`,
`clearStorageArrayFunction`, and `clearStorageStructFunction`. The legacy code generator
also delegates array copies to these Yul helpers through
[ArrayUtils.cpp](https://github.com/argotorg/solidity/blob/v0.8.34/libsolidity/codegen/ArrayUtils.cpp).

## Definitions and scope

Use finite, nonrecursive shapes of uint256 values, arrays, and structs. Whole-value copies
containing mappings are excluded because Solidity rejects them. Mapping preservation belongs
to the reset lemma. Packed elementary types and bytes/string require additional layout lemmas.

```text
R_T(x)       = reset the ordinary data of node x according to target shape T
C_T(d, s)    = copy source node s into destination node d according to target shape T
L_T(d, s)    = k       if T = fixed(k, E)
              len(s)  if T = dynamic(E)
```

For scalar leaves, `R_uint(x) = 0` and `C_uint(d, s) = s`. Resetting a fixed array keeps
its declared length and resets its valid children; resetting a dynamic array clears its old
valid children and sets its length to zero. Struct resets recurse through nonmapping members.
Resetting a whole mapping preserves its entries; deleting an individual mapping key resets
that key's value according to its value shape.

Array copy uses old nodes `d` and `s`, with `m = len(d)` and `n = len(s)`:

```text
len(C_T(d, s)) = L_T(d, s)

C_T(d, s)[i] = C_E(d[i], s[i])  if 0 <= i < n
              R_E(d[i])        if n <= i < m
              d[i]            otherwise
```

For a fixed target, type compatibility requires `n <= k`. For structs, copy recursively
member by member. The source element shape may differ from the target element shape, as in
`uint[2][2] -> uint[3][3]`; `C_E` uses the destination's shape and the source's own length.

The theorem is about decoded Solidity observations, including cleared former elements that
can be exposed by a later push. It is not equality of compiler temporaries or arbitrary raw
storage slots.

## Proposed refinement theorem

Let `decode_T(S, q)` interpret EVM storage `S` at destination location `q` using shape `T`.
Fixed lengths come from `T`, not from an `sload` of a length cell. Let `D` and `A` be the
destination and source nodes decoded in the pre-state `S0`.

For a well-typed copy with disjoint source and destination storage footprints, valid source
lengths, and successful execution of the compiler-generated code:

```text
decode_T(exec(copyCode, S0), q) = C_T(D, A)
```

Disjointness includes nested array payloads, not just unequal root slots. Decoding assumes
the usual absence of storage-address collisions. Identical source and destination of the same
type form a separate case: the compiler's equality check returns without modifying storage,
and `C_T(D, D) = D` for well-formed `D`.

Partial overlap, dangling-reference writes, raw assembly mutations, and failure due to gas or
resource bounds are outside this initial theorem. A rule claiming termination needs those
failure conditions modelled separately. The pre-state-source equation should not be assumed
for arbitrary partially overlapping aggregates without a further proof.

## Proof by structural induction and loop invariants

First prove the reset lemma by induction on shape. At a uint256 leaf, the generated store
writes zero. For a fixed array, its clear loop clears exactly its declared valid elements;
the decoded length stays constant. For a dynamic array, resize-to-zero clears its former
valid elements and stores zero in the length slot. Struct clearing recurses through members
and skips mappings, so mapping contents are a frame of the reset.

Then prove the copy theorem by induction on destination shape. The scalar case is one
load/store. The struct case follows by the induction hypothesis for its independent members.
For arrays, the resize helper establishes the required length and resets `[n, m)` when
`n < m`. It leaves the destination prefix `[0, n)` available for the copy loop.

Let `D0` and `A0` be ghost snapshots of the original nodes. At copy-loop index `t`, use:

```text
0 <= t <= n
len(dst) = L_T(D0, A0)

forall i in [0, t):  dst[i] = C_E(D0[i], A0[i])
forall i in [t, n):  dst[i] = D0[i]
forall i in [n, m):  dst[i] = R_E(D0[i])

source = A0
```

Empty intervals contribute no condition. The invariant holds at `t = 0` after resize.
Iteration `t` applies the induction hypothesis to child `t`; disjoint storage footprints
preserve `A0` and the other children. Incrementing `t` maintains the invariant. The variant
`n - t` decreases, so at `t = n` every copied child has the specified value and the reset
tail is unchanged. This is precisely `C_T(D0, A0)`.

The nested example illustrates two applications of the same argument:

```text
src shape: fixed(2, fixed(2, uint))
dst shape: fixed(3, fixed(3, uint))

source = [[11, 12], [21, 22]]
result = [[11, 12, 0], [21, 22, 0], [0, 0, 0]]
```

This makes fixed-to-fixed copying an instance of the general theorem. Length preservation
comes from decoding the destination shape; tail clearing comes from the reset lemma.

Shape preservation follows from the same induction:

```text
wf(T, D0) and wf(U, A0) and compatible(U, T)
    imply wf(T, C_T(D0, A0))
```

For fixed arrays, the result length is the target's declared length. Copied children satisfy
the target element shape by the copy induction hypothesis, and reset children satisfy it by
the reset lemma. For dynamic arrays, the source length is nonnegative and the same child
arguments apply. Structs follow member by member. This is the property a generalized copy
taclet must establish at the function's `wellformed(storage)` postcondition.

## What this establishes about empty push

The generated ordinary-array `array_push_zero` helper increments the length and returns the
new element's location; it does not clear that element. The compiler relies on unused ordinary
storage having been initialized or cleared by previous operations. The reset/copy induction
explains that guarantee for defined executions
starting from the constructor's empty storage; mapping contents are exempt.

The implemented logical push makes the default-value guarantee explicit: it resets the
appended nonmapping node with `delAt`, then saves the incremented length. The returned-reference
push uses the same update, so fixed lengths and nested mappings are preserved. A whole mapping
element is not reset. This proves the formerly open fixed-element zero read on symbolic entry
storage and also proves defaults through returned struct and dynamic-array references.

For a clean unused node `x`, resetting it is observationally the identity. Under that premise,
the logical update refines the compiler's length-only implementation. The concrete compiler
also allows dangling writes that break this premise; Solidity documents those executions as
[undefined behavior](https://docs.soliditylang.org/en/latest/types.html#dangling-references-to-storage-array-elements).
`StorageDanglingOpen` retains four current-compiler witnesses that intentionally stay open
under the portable reset/push rules. The refinement claim is for defined Solidity execution,
not for preserving compiler-specific dangling-reference data.

## Reproduce the observations

```bash
./run-key.sh keyext.solidity.examples/contracts/ArrayCopyShapes.sol --open-goals -m 20000
./run-key.sh keyext.solidity.examples/contracts/ArrayCopyShapes.sol --solc
```

To inspect the project's pinned compiler output after its soljson has been downloaded:

```bash
node <<'JS'
const fs = require('fs');
const solc = require('./keyext.solidity.core/build/soljson/soljson-v0.8.34+commit.80d5c536.js');
const compile = solc.cwrap('solidity_compile', 'string', ['string', 'number', 'number']);
const file = 'keyext.solidity.examples/contracts/ArrayCopyShapes.sol';
const input = {
    language: 'Solidity',
    sources: { 'ArrayCopyShapes.sol': { content: fs.readFileSync(file, 'utf8') } },
    settings: { viaIR: true, optimizer: { enabled: false },
        outputSelection: { '*': { '*': ['ir'] } } }
};
const output = JSON.parse(compile(JSON.stringify(input), 0, 0));
for (const error of output.errors || []) process.stderr.write(error.formattedMessage);
if ((output.errors || []).some(error => error.severity === 'error')) process.exit(1);
fs.writeFileSync('/tmp/ArrayCopyShapes.ir.yul',
    output.contracts['ArrayCopyShapes.sol'].ArrayCopyShapes.ir);
JS
```

Inspect `resize_array`, `copy_array_to_storage`, `clear_storage_array`, and `array_push_zero`
functions in that output. They contain the operations used in the proof outline above.
