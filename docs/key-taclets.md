# KeY Taclets for Solidity

This is the short authoring guide for Solidity program taclets. Use
`docs/taclets-implementation.md` for current feature status and
`docs/storage.md` for the storage calculus.

## Where Rules Live

Solidity program taclets live in:

```text
keyext.solidity.core/src/main/resources/org/key_project/solidity/proof/rules/solidityProgramRules.key
```

They are loaded by `standardSolidityRules.key` after the generic logic,
update, list, memory, and struct rules. Put Solidity statement rules in
`\rules(programRules:Solidity)`.

## Rule Shape

Most program rules follow this shape:

```key
ruleName {
    \schemaVar \formula post;
    \schemaVar \program Path[storage,simple,global] gp;
    \schemaVar \program SimpleExpression se;

    \find(\modality{#mod}{c# s#gp = s#se; #c}\endmodality(post))
    \replacewith({storage := save(storage, gp, se)}
        \modality{#mod}{c# #c}\endmodality(post))
    \heuristics(simplify_prog)
};
```

Important syntax:

- Use `\schemaVar` inside each rule for local schema variables.
- Use `\modality{#mod}{c# ... #c}\endmodality(post)` to match the active
  Solidity statement in its context.
- Inside program syntax, reference schema variables as `s#name`.
- Use `\replacewith` either to emit an update before the remaining modality or
  to rewrite the active program to simpler statements.
- Add `\heuristics(simplify_prog)` for normal symbolic execution rules.

## Schema Variables

Prefer precise program sorts so rules stay disjoint:

- `Variable` for stack/value targets; a variable carries no value-mode flag —
  keep rules disjoint via the field/path side instead: `Path[...,primitive]` /
  `Path[...,reference]` type-kind flags, `Path[...,primitiveElement]` /
  `Path[...,referenceElement]` element-kind flags on indexed receivers, and
  `SimpleExpression[primitive]` / `NonSimpleExpression[primitive]` on
  expressions (mappings count as reference throughout). Accessed members carry
  no flag: a `Field` rule discriminates on the member's declared type through
  the bound of the generic sort its `\hasFieldSort` / `\hasMemoryFieldSort`
  varcond binds (`alphaPrim \extends Prim` admits only value-typed members,
  `alphaId \extends Identity` only reference-typed ones).
- `Variable[storage]` for local storage aliases.
- `Variable[memory]` for local memory references.
- `Path[storage,simple,global]` for contract storage roots.
- `Path[storage,simple]` for simple storage roots or aliases.
- `Path[storage,complex]` for member/index paths that need unfolding.
- `StoragePath`, `SimpleStoragePath`, `MemoryPath`, and related path sorts when
  the exact path kind is the point of the rule.
- `SimpleExpression`, `NonSimpleExpression`, `Expression`, `Field`, and `Type`
  for statement pieces.

### Naming conventions

Use the canonical names from `docs/storage.md`/`docs/memory.md` — every taclet
of the same shape uses the same name for the same role:

| Role | Name |
|---|---|
| Postcondition formula | `post` |
| Simple / nonsimple / arbitrary expression | `se` (`se1`, `se2`) / `nse` / `e` (`e1`, `e2`) |
| Simple index expression | `i` |
| Stack variable read target | `v` |
| Assignment target (arbitrary / nonsimple) | `lhs` / `nlhs` |
| Global storage root | `gp` |
| Simple / nonsimple storage path | `sp` (`sp1`, `sp2`) / `nsp` |
| Local storage alias variable | `lp` |
| Simple / nonsimple memory path | `mp` / `nmp` |
| Field (second field) | `a` (`b`) |
| Fresh captured value temp and its type | `pv`, `pvType` |
| Type of a fresh path alias | `aliasType` |
| Declared type in value declarations | `varType` |
| Address in transfer rules (`net(a)`) | `a` |

Matched program schema variables can be used directly in the term positions of
`\replacewith`/`\add`: the engine lowers the matched AST piece to its logic
form automatically (storage paths become `List` terms, fields become `Field`
constants, simple expressions become value terms). Write `save(storage,
gp, se)` directly — no bridging `\term` variable is needed. (Only in
`\find`/`\assumes` term positions are program schema variables not allowed;
there the `\sameAsTerm(programPart, termPart)` varcond still bridges them.)

## Varconds

Use `\newTypeOf(freshVar, source)` and `\newTypeOf(aliasType, source)` when a
rule synthesizes a fresh program variable or declaration with the same Solidity
type as another expression/path.

Example unfold rule:

```key
\find(\modality{#mod}{c# s#nsp.s#a = s#se; #c}\endmodality(post))
\varcond(\newTypeOf(sp, nsp), \newTypeOf(aliasType, nsp))
\replacewith(\modality{#mod}{c# s#aliasType storage s#sp = s#nsp;
                              s#sp.s#a = s#se; #c}\endmodality(post))
```

## Storage Rule Pattern

Follow the storage calculus order:

1. Unfold nonsimple RHS parts into fresh simple variables.
2. Unfold nonsimple LHS/path parts into fresh storage aliases.
3. Emit the semantic update, usually with `save(storage, path, value)` or
   `find<[int]>(storage, path)`.

Do not collapse simple/complex or storage/memory cases just because the surface
syntax looks similar. `delete carol.age`, `alice.age`, `acc.balance`, and
`values[i]` can need different rule shapes depending on path kind and data
location.

## Examples and Verification

Add runnable taclet examples as functions of:

```text
keyext.solidity.examples/TestSuite.sol
```

There are no `.key` problem files — the loader synthesizes the obligation
`\<{ f()@TestSuite; }\>(true)` for each function, so the specification is written in the body.
Use small examples that force the new rule, for example:

```solidity
function storageRootReadWrite() public {
    age = 34;
    uint r = age;
    assert(r == 34);
}
```

An example that needs an assumption states it with `require` and is tagged `/// @custom:key
box`, which makes `require` an assumption instead of an obligation. Full conventions:
`keyext.solidity.examples/README.md`.

Verify individual examples with the Solidity CLI:

```bash
./run-key.sh keyext.solidity.examples/TestSuite.sol <function>
```

For the taclet example set, use the focused harnesses:

```bash
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.taclets.TacletStarterExamplesTest"
./gradlew :keyext.solidity.core:test --tests "org.key_project.solidity.taclets.PaperTestExamplesTest"
```

Avoid using the full legacy `RulesTest` suite as the first acceptance gate for
new taclet examples; it can fail for unrelated older examples. It now lives in
the CI-only examples group — run it via
`./gradlew :keyext.solidity.core:testSolidityExamples --tests "*RulesTest"`.
