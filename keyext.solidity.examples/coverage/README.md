# Taclet coverage proofs

`TacletCoverageTest` (the CI-only examples group) requires every taclet of the Solidity rule
files to be applied by some proof: a `taclets/` function in automode, or a saved proof here
for the taclets no such function reaches. Each `NAME.key` is a problem whose proof applies the
taclet `NAME`. It is either a logic lemma, for a taclet whose function symbols the program rules
never produce (`headDefinition`, `precOfInt`, `applySkip3`, …), or a `taclets/` obligation
the synthesizer cannot state: one where automode prefers a competing rule
(`localDeclPostdecrement`), or a diamond `transfer` or `send`.
`NAME.proof` beside it is the saved proof the test replays. After adding a `.key` or changing
the rules, regenerate the proofs with

```bash
./gradlew :keyext.solidity.core:testSolidityExamples --tests "*TacletCoverageTest" \
    -Dorg.key_project.solidity.taclets.TacletCoverageTest.update=true
```

A new taclet that a `taclets/` function can exercise gets a function there instead.
