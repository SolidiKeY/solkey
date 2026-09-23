# Rule Generalizations

Many taclets in `solidityProgramRules.key` are instances of one pattern that differs only by
operator. For example, `storageRootAddAssign`, `storageRootSubAssign` and
`storageRootMulAssign` are the same rule with `+=`/`+` swapped for `-=`/`-` and `*=`/`*`. Such a
group is stated once, as a `// generalization:` comment above its first taclet, and
`RuleGeneralizationTest` checks that the claim is true.

## The comment

```
    // generalization:
    //     storageRoot⟨name⟩Assign {
    //         \schemaVar \formula post;
    //         \schemaVar \program Path[storage,simple,global] gsp;
    //         \schemaVar \program SimpleExpression se;
    //         \find(\modality{#mod}{c# s#gsp ⟨0⟩ s#se; #c}\endmodality(post))
    //         \replacewith({storage := save(storage, gsp, find<[int]>(storage, gsp) ⟨1⟩ se)}
    //             \modality{#mod}{c# #c}\endmodality(post))
    //         \heuristics(simplify_expression)
    //     };
    //     taclet                ⟨0⟩  ⟨1⟩
    //     storageRootAddAssign  +=   +
    //     storageRootSubAssign  -=   -
    //     storageRootMulAssign  *=   *
    storageRootAddAssign {
```

The comment holds a **template** taclet and a **table** below it:

- The template name contains `⟨name⟩` once; each listed taclet name must match it (`Add` in
  `storageRootAddAssign`).
- The template body uses the placeholders `⟨0⟩ … ⟨n-1⟩`, which are exactly the table header's
  columns after `taclet`.
- Each row names a taclet and gives the value of every placeholder. Columns are separated by two
  or more spaces, so a value may contain single spaces.

## How verification works

For every row, the test fills the template with the row's values, collapses whitespace in the
result and in the named taclet's body, and requires the two to be identical. Any difference
beyond the placeholders fails with a pointer to the first divergence: a changed guard, a
reordered update or a different heuristic. The test also checks that each comment is well
formed and that no taclet is listed in two comments. A self-test injects a divergence into a
fabricated taclet, so a checker that passes everything vacuously also fails.

When some operators of a construct differ structurally, they get their own comment instead of
stretching a shared one. Examples: `/=` and `%=` wrap the effect in `\if(se != 0) … \else(revert)`;
array-indexed terminals add the `inBounds` / `outOfBounds` goal pair that mapping-indexed terminals
lack; pre- and post-increment bind the result in different orders.

## Running the check

```bash
./gradlew :keyext.solidity.core:testRuleGeneralization
```

Each comment is a test container named `line N: <template name>`. It holds a `well formed` test
and one test per row, named after the taclet. The group runs in the `Solidity /
rule-generalization` CI job; the common `:keyext.solidity.core:test` task excludes it by tag.

## Adding a taclet

- **To an existing comment:** write the taclet by copying a listed one and swapping the operator
  tokens, then add its row to the table.
- **A new pattern:** put a `// generalization:` comment above its first taclet. For the template,
  copy that taclet's body and replace the operator-specific parts with placeholders.
