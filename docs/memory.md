# Memory Rules - Summary for Agent Consumption

A compact, macro-free summary of Solidity memory behavior in the calculus.
This is the memory-side companion to `storage.md`. Storage rules and storage
paths are out of scope except where cross-domain copies are mentioned.

## 1. Core Model

Memory is an identity-indexed heap, not a tree-shaped value.

- `memory` is the heap variable.
- A memory variable such as `carol` stores an identity, for example
  `idC(r, nil)`.
- A nested memory field such as `carol.account` is also identity-valued when
  the field has reference type. Reading it produces a compound identity such
  as `idC(r, account)`.
- Primitive fields such as `carol.age` or `carol.account.balance` are heap
  cells read through an identity.

The important consequence is aliasing:

    Account memory acc = carol.account;
    acc.balance = 100;

After this, `carol.account.balance` is also `100`, because `acc` and
`carol.account` denote the same memory identity.

## 2. Memory Primitives

The Maude model uses these operations:

- `mtMem` / `emptyMemory` - empty memory.
- `add(mem, id)` - allocate a fresh root identity.
- `read(mem, id, field)` - read one field from one identity.
- `readR(mem, id, fields)` - recursively read through a field path.
- `write(mem, id, field, value)` - write one field of one identity. `value` has
  sort `MemValue`, the supersort of everything storable in memory: `Identity`
  (references) and `Prim` (`int`, `bool`, contract sorts). `Struct` and `List`
  are not storable in memory — structs enter it only via `copySt`, whose last
  argument is `Struct`-sorted.
- `delete(mem, id)` - reset the identity subtree to defaults.
- `erase(mem)` - normalize identity-copy writes used by lazy copy semantics.
- `copySt(mem, id, st)` - install a lazy storage-to-memory view.
- `copyMem(st, mem, id)` - install a lazy memory-to-storage view.

Default reads:

    read(add(mem, r), idC(r, path), primitiveField) = 0
    read(add(mem, r), idC(r, path), identityField)  = idC(r, path identityField)

So freshly allocated memory behaves as if primitive fields are defaulted and
reference fields point to their corresponding compound identity.

Recursive reads follow identities:

    readR(mem, carol, account balance)
      = read(mem, read(mem, carol, account), balance)

## 3. Rule Strategy

Memory statements follow the same three-step strategy as storage statements:

1. Unfold the RHS.
   Capture nonsimple right-hand subexpressions into fresh temporaries.
2. Unfold the LHS.
   Capture nonsimple memory paths or indices into fresh aliases.
3. Emit the update.
   Once the statement is simple, produce the heap or variable update.

Declarations with an initializer are decomposed first: `memoryLocalDeclInitDrop`
rewrites `T memory m = x;` to `m = x;` and registers `m` as a program variable
(the `memory` keyword in the pattern is matched against the declared variable's
data location; siblings `localValueDeclInitDrop` / `storageLocalDeclInitDrop`
handle the other locations). Only
the bare declaration `T memory m;` performs a fresh allocation (§4). The drop
rules strictly decrease the number of initialized declarations, so they compose
with the storage termination measure (`storage.md` §9).

Memory arrays are fixed-length after allocation. They have bounds branches for
index access, but no mappings and no `push` or `pop`.

## 4. Simple Memory Declaration

Solidity:

    Person memory carol;

Effect:

    new(memory, r)
    carol := idC(r, nil)
    memory := add(memory, r)

Fresh memory allocation creates a root identity. Its primitive fields read as
defaults, and its reference fields read as compound identities.

Example:

    Person memory carol;
    uint age = carol.age;              // 0
    Account memory acc = carol.account; // idC(r, account)

## 5. Root Assignment Aliases

Solidity:

    Person memory carol;
    Person memory david;
    david.age = 40;
    carol = david;
    carol.age = 41;

Effect:

    carol := david
    memory := write(memory, carol, age, 41)

Because `carol` is rebound to David's identity, both names observe the same
heap cell:

    david.age == 41
    carol.age == 41

Root memory assignment is not a deep copy.

## 6. Field Read Aliases

Solidity:

    Person memory carol;
    Account memory carolAcc = carol.account;
    carolAcc.balance = 100;

Steps:

    carol := idC(r, nil)
    carolAcc := read(memory, carol, account)
             = idC(r, account)
    memory := write(memory, carolAcc, balance, 100)

Therefore:

    carol.account.balance == 100

For identity-valued memory fields, a field read aliases the nested identity.
For primitive fields, a field read returns the primitive heap value.

## 7. Deep Field Write

Solidity:

    carol.account.balance = 10;

The receiver `carol.account` is captured first:

    Account memory acc = carol.account;
    acc.balance = 10;

Then the heap update is emitted:

    acc := read(memory, carol, account)
    memory := write(memory, acc, balance, 10)

Equivalently:

    memory := write(memory, read(memory, carol, account), balance, 10)

## 8. Deep Field Read

Solidity:

    v = carol.account.balance;

The receiver is captured first:

    Account memory acc = carol.account;
    v = acc.balance;

Then:

    acc := read(memory, carol, account)
    v := read(memory, acc, balance)

Equivalently:

    v := read(memory, read(memory, carol, account), balance)

## 9. Field Assignment Between Memory References

Solidity:

    carol.account = david.account;
    carol.account.balance = 60;

The RHS field read is split so the identity is captured:

    Account memory pv = david.account;
    carol.account = pv;

Then:

    pv := read(memory, david, account)
    memory := write(memory, carol, account, pv)

After this, `carol.account` and `david.account` denote the same account
identity. The later write through `carol.account.balance` is visible through
`david.account.balance`.

This is a shallow aliasing assignment, not a structural copy.

## 10. Delete

Memory delete resets the selected memory identity or primitive cell to
defaults. Existing aliases to old identities remain valid.

### Root Delete

Solidity:

    Person memory carol;
    Person memory carolAlias = carol;
    carol.age = 33;
    delete carol;

Effect:

    carol := fresh/default identity

The root variable is rebound. `carol.age` reads as `0`, but `carolAlias.age`
still reads as `33`.

### Identity Field Delete

Solidity:

    Person memory carol;
    Account memory acc = carol.account;
    acc.balance = 100;
    delete carol.account;

Effect:

    carol.account := fresh/default account identity

Now:

    carol.account.balance == 0
    acc.balance == 100

The delete freshens the selected field; it does not mutate aliases that already
pointed at the old account identity.

### Primitive Field Delete

Solidity:

    carol.age = 20;
    delete carol.age;

Effect:

    memory := write(memory, carol, age, 0)

## 11. Memory Arrays

Memory arrays have fixed length after allocation:

    uint[] memory xs = new uint[](4);

Index reads and writes use the index as a field selector, usually written
`at(i)` in the model:

    xs[i] = 33;
    v = xs[i];

Effects:

    memory := write(memory, xs, at(i), 33)
    v := read(memory, xs, at(i))

Index evaluation follows the usual RHS-before-LHS discipline:

    xs[++i] = makeValue();

The value-producing RHS is captured before the indexed LHS update fires.

Arrays of structs still alias through identity-valued elements:

    Token[] memory carolTokens = new Token[](4);
    Token[] memory davidTokens = new Token[](4);

    davidTokens[0] = makeToken(7);
    carolTokens[1] = davidTokens[0];
    Token memory tok = carolTokens[1];
    tok.value = 9;

Then:

    carolTokens[1].value == 9

If `delete carolTokens[1]` runs afterward, the array slot is reset/freshened,
but `tok.value` remains `9` because `tok` still points to the old identity.

## 12. Storage to Memory

Storage-to-memory declaration copies allocate a fresh memory identity and
install a lazy storage view:

    Person memory carol = alice;

Effect shape:

    new(memory, r)
    carol := idC(r, nil)
    memory := copySt(memory, r, select(storage, alice))

Reads through `carol` delegate to the storage snapshot installed by `copySt`:

    read(copySt(mem, r, st), idC(r, path), identityField)
      = idC(r, path identityField)

    read(copySt(mem, r, st), idC(r, path), primitiveField)
      = find(st, path primitiveField)

The copy is independent from later storage writes in the source program state:

    Person memory carol = alice;
    alice.age = 30;
    v = carol.age; // old copied value

## 13. Memory to Storage

Memory-to-storage assignment stores a storage struct view of a memory identity:

    alice = carol;

Effect shape:

    storage := save(storage, alice, copyMem(emptyStruct, memory, carol))

For fields:

    alice.account = carol.account;

Effect shape:

    storage := save(storage, alice account,
                    copyMem(emptyStruct, memory, read(memory, carol, account)))

The lazy `copyMem` view delegates storage reads back to memory:

    find(copyMem(st, memory, id), path primitiveField)
      = readR(memory, id, path primitiveField)

Use the eager variant only when a fully materialized struct is required.

## 14. Common Agent Pitfalls

- Do not model memory structs as deep value copies.
- Root memory assignment aliases by rebinding the target variable.
- Memory field reads of reference type alias by returning an identity.
- Primitive reads return primitive values, not identities.
- Writing a primitive field mutates the heap cell behind an identity.
- Writing an identity field changes which identity that field points to.
- Deleting a memory root rebinds that variable; old aliases keep the old data.
- Deleting an identity-valued field freshens that field; old aliases remain.
- Memory arrays have length and bounds checks, but no mappings, `push`, or
  `pop`.
- Cross-domain storage-memory assignments are handled by `copySt` and
  `copyMem`, not by ordinary single-location memory rules.
