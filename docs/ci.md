# CI gates and test groups

CI enforces three gates. **Two of them are off in a normal build**, so
`./gradlew classes` and `./gradlew test` can both pass while CI fails.

| Gate | CI job | Local command |
|---|---|---|
| All three at once | — | `./gradlew -DENABLE_NULLNESS=true ciGates` |
| Formatting | `CodeQuality / formatting` | `./gradlew spotlessApply` |
| Nullness | `CodeQuality / checkerFramework` | `./gradlew -DENABLE_NULLNESS=true :keyext.solidity.core:compileTestJava` |
| Module tests | `Solidity / test` | `./gradlew :keyext.solidity.core:test` |

## CI-only test groups

Not part of `ciGates` — the two slow proof suites are split off by JUnit tag so
the local gate stays fast, and the GUI module has its own job.

| CI job | Local command | Content |
|---|---|---|
| `Solidity / examples` | `./gradlew :keyext.solidity.core:testSolidityExamples` | `RulesTest` (`.key` problems), `NetExamplesTest`, `SolcSemanticsExamplesTest`, `TacletCoverageTest`, the one-example showcases, the `solc/*.sol` half of `SolidityRuntimeExecutionTest` |
| `Solidity / rule-generalization` | `./gradlew :keyext.solidity.core:testRuleGeneralization` | `RuleGeneralizationTest` |
| `Solidity / gui` | `./gradlew :keyext.solidity.gui:test` | the `keyext.solidity.gui` tests (headless) |

A `--tests` filter naming a class of a CI-only group must go on **that group's
task** — on `test` the tag exclusion leaves zero matches and Gradle fails with
"No tests found".

## `ciGates`

Defined in the root `build.gradle`. It runs all three gates in one invocation
and reports **every** failing gate rather than stopping at the first. Three
things to know:

- It uses `spotlessCheck`, so fix formatting with `spotlessApply`.
- It refuses to start without `-DENABLE_NULLNESS=true`, because the checker is
  wired in at Gradle *configuration* time and a run without the flag would be a
  false green.
- It reaches only `keyext.solidity.core` plus its `api` dependencies, so edits
  elsewhere in the repo still need the repo-wide
  `./gradlew -DENABLE_NULLNESS=true compileTestJava` that
  `CodeQuality / checkerFramework` runs.

## Test output

A local `test` run prints failures only. Pass `-PverboseTests` (or set `CI`) to
get the per-test progress lines, skipped-test warnings and captured standard
streams that the CI logs carry.

## The JDK caveat

The build sets `sourceCompatibility = 21` but **no Java toolchain**, so it
compiles with whatever JDK is on `PATH` while CI always uses 21. On a newer JDK
the Checker Framework prints `The Checker Framework is tested with JDK 8, 11,
17, and 21` — a green local nullness gate on JDK 25 is therefore not a
guarantee of a green `CodeQuality / checkerFramework`. Use a JDK 21 on `PATH`
when the nullness result matters.

## The nullness checker

It activates only under `-DENABLE_NULLNESS=true`, and it is scoped to
`org.key_project.solidity.program.ast` (`-AonlyDefs` in
`keyext.solidity.core/build.gradle`). **Only edits under `program/ast/` can trip
it** — skip it for work confined to `strategy/`, `rule/`, `speclang/`,
`parser/`, and so on.

Nearly every failure is a `@Nullable` value reaching a `@NonNull` parameter or
return. The checker does not refine a nullable field across two calls, so hoist
it into a local first:

```java
// rejected — getTypeReference() is called twice, so the guard does not refine the second read
if (fd.getTypeReference().getReferencedType() != null) {
    return fd.getTypeReference().getReferencedType();
}

// accepted
Type referencedType = fd.getTypeReference().getReferencedType();
if (referencedType != null) {
    return referencedType;
}
```

`MemoryReferenceTypes.asMemoryReferenceType` is the reference example: hoist to
a local, return the unchanged input when null. Prefer that over widening a
parameter or field to `@Nullable`.
