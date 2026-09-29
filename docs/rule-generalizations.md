# Rule Generalizations

Many taclets in `solidityProgramRules.key` are instances of one pattern that differs only by
operator. For example, `storageRootAddAssign`, `storageRootSubAssign` and
`storageRootMulAssign` differ only in `+`/`-`/`*`. Such a group is stated once, as a
`// generalization:` comment above its first taclet, and `RuleGeneralizationTest` keeps the
comment exact.

## The comment

```
    // generalization:
    //     storageRoot⟨name⟩Assign {
    //         \schemaVar \formula post;
    //         \schemaVar \program Path[storage,simple,global] gsp;
    //         \schemaVar \program SimpleExpression se;
    //         \find(\modality{#mod}{c# s#gsp ⟨0⟩= s#se; #c}\endmodality(post))
    //         \replacewith({storage := save(storage, gsp, find<[int]>(storage, gsp) ⟨0⟩ se)}
    //             \modality{#mod}{c# #c}\endmodality(post))
    //         \heuristics(simplify_expression)
    //     };
    //     taclet                ⟨0⟩
    //     storageRootAddAssign  +
    //     storageRootSubAssign  -
    //     storageRootMulAssign  *
    storageRootAddAssign {
```

The comment has a **template**, then a table: a header row naming the columns (`taclet ⟨0⟩ ⟨1⟩
…`) and **one row per taclet** giving its name and the value of each placeholder. `⟨name⟩` stands for the part of the name that varies.

## How it is computed

The test builds the comment from the listed taclets alone:

1. Split each body into tokens: identifiers, single characters, and whitespace collapsed to one
   space.
2. Align the bodies on their common tokens, using the longest common block first, then
   recursively on each side.
3. Each stretch where the bodies disagree becomes a placeholder. A stretch that would be empty
   in some row, or would start or end with a space, is merged with the nearest other stretch.
   This is how `++s#gsp` / `s#gsp++` becomes one column.
4. Stretches that take the same value in every row share a placeholder, so a
   `Path[storage,…]` / `Path[memory,…]` difference is a single `storage` / `memory` column
   wherever it occurs.

Filling the template with each row must give back that taclet's body, up to whitespace. The
test also fails when the comment in the file is not exactly the computed one, or when a taclet
is listed twice. A taclet that drifts away from its group therefore shows up as a changed
comment: a new column, or new values in one.

When some operators of a construct differ structurally, they get their own comment. Examples:
`/=` and `%=` revert on a zero divisor, array-indexed writes have bounds goals that mapping-indexed
writes lack, and pre- and post-increment bind the result in different orders.

## Running and updating

```bash
./gradlew :keyext.solidity.core:testRuleGeneralization
./gradlew :keyext.solidity.core:testRuleGeneralization \
    -Dorg.key_project.solidity.taclets.RuleGeneralizationTest.update=true
```

The first command checks every comment. The group runs in the `Solidity / rule-generalization`
CI job; the common `:keyext.solidity.core:test` task excludes it by tag. The second rewrites
every comment in the source file from its list of taclets and reports itself as skipped.

To start a group, or to add a taclet to one, list the names under the marker and run the update:

```
    // generalization:
    //     fooAddRule
    //     fooSubRule
```
