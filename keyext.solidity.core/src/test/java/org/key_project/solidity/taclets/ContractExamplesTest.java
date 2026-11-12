/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.init.SolidityProblemSynthesizer;
import org.key_project.solidity.testutil.ExampleDirs;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/// Checks the contracts of `keyext.solidity.examples/contracts/solidikey/`, the solidiKeY example
/// contracts with their specification carried in `@custom:key` natspec tags, and of
/// `contracts/real-world/`, published contracts specified the same way, and of
/// `contracts/real-world-plain/`, the same programs with their specification written as
/// `require`s (preconditions, invariants on entry) and `assert`s (postconditions, invariants on
/// exit) under `@custom:key box`, except in `open/key-spec/`, which keeps as `@custom:key` clauses
/// those with no Solidity form.
///
/// Every `.sol` in the directories is enumerated and every function an obligation can be
/// generated for is checked, so a new contract joins the suite by being written — the contract
/// name is taken from the file name. The functions in [#KNOWN_OPEN] need an invariant
/// automode cannot establish, including quantified invariants and known storage tails. Each case
/// must load and leave its proof open, so an improvement that closes one turns the entry stale.
/// The cases in [#TOO_SLOW] run out of steps only after minutes, so they are skipped.
@Tag("solidityExamples")
public class ContractExamplesTest {

    private static final List<String> DIRECTORIES =
        List.of(ExampleDirs.SOLIDIKEY, ExampleDirs.REAL_WORLD, ExampleDirs.REAL_WORLD_PLAIN,
            ExampleDirs.REAL_WORLD_PLAIN_UNPROVABLE, ExampleDirs.REAL_WORLD_PLAIN_KEY_SPEC,
            ExampleDirs.REAL_WORLD_PLAIN_SOLIDIKEY);

    private static final Set<String> KNOWN_OPEN = Set.of(
        "contracts/solidikey/MultiAuction.placeOrIncreaseBid",
        "contracts/solidikey/MultiAuction.withdraw",
        "contracts/solidikey/MultiAuction.myTest",
        "contracts/solidikey/ArrayCopyShapes.fixedToDynamicCopyTakesSourceLength",
        "contracts/solidikey/ArrayCubeDFD.popPushAtDepth1ResetsRemovedElement",
        "contracts/solidikey/ArrayCubeDFD.popPushAtDepth3ResetsRemovedElement",
        "contracts/solidikey/ArrayCubeDFD.pushReturnAliasSharesWrites",
        "contracts/solidikey/ArrayCubeDFD.storageAssignmentCopiesDeeply",
        "contracts/solidikey/ArrayMatrixConversions.dynamicDynamicShrinkClearsReusedRow",
        "contracts/solidikey/ArrayMatrixConversions.fixedRowsToDynamicInner",
        "contracts/solidikey/ArrayMatrixDD.popPushAtDepth2ResetsRemovedElement",
        "contracts/solidikey/ArrayMatrixDD.pushReturnAliasSharesWrites",
        "contracts/solidikey/ArrayMatrixDF.pushReturnAliasSharesWrites",
        "contracts/solidikey/FixedLengths.emptyPushFixedElementReadsZero",
        "contracts/solidikey/FixedLengths.emptyPushReadsZero",
        "contracts/solidikey/FixedLengths.fixedCopyIsIndependent",
        "contracts/real-world-plain/Ownable.constructor",
        "contracts/real-world-plain/open/unprovable/ArrayRemoveByShifting.remove",
        "contracts/real-world-plain/open/unprovable/MultiAuction.placeOrIncreaseBid",
        "contracts/real-world-plain/open/unprovable/MultiAuction.withdraw",
        "contracts/real-world-plain/open/unprovable/MultiAuction.myTest");

    private static final Set<String> TOO_SLOW = Set.of();

    @ParameterizedTest(name = "{0}/{1}.{2}")
    @MethodSource("examples")
    void contractExampleCloses(String directory, String contract, String function)
            throws Exception {
        assumeFalse(TOO_SLOW.contains(directory + "/" + contract + "." + function));
        Proof proof =
            SolidityExampleTests.proveWithEstimatedBudget(directory, contract, function);
        String name = directory + "/" + contract + "." + function;
        if (KNOWN_OPEN.contains(name)) {
            assertFalse(proof.closed(), name
                + " closed; remove it from KNOWN_OPEN");
        } else {
            assertTrue(proof.closed(),
                () -> SolidityExampleTests.describeOpenGoals(contract + "." + function, proof)
                        + SolidityExampleTests.budgetHint(directory, contract));
        }
    }

    static Stream<Arguments> examples() throws IOException {
        Stream.Builder<Arguments> args = Stream.builder();
        for (String directory : DIRECTORIES) {
            for (String contract : SolidityExampleTests.solContracts(directory)) {
                SolidityProblemSynthesizer
                        .provableFunctions(contractSource(directory, contract), contract)
                        .stream()
                        .sorted()
                        .forEach(function -> args.add(Arguments.of(directory, contract, function)));
            }
        }
        return args.build();
    }

    private static Path contractSource(String directory, String contract) {
        return SolidityExampleTests.example(directory + "/" + contract + ".sol");
    }
}
