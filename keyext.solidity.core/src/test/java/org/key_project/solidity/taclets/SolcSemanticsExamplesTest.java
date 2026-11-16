/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.ExampleDirs;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("solidityExamples")
public class SolcSemanticsExamplesTest {

    private static final String DIRECTORY = ExampleDirs.SEMANTICS;

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("examples")
    void solcSemanticsExampleCloses(String contract, String function) throws Exception {
        Proof proof =
            SolidityExampleTests.proveWithEstimatedBudget(DIRECTORY, contract, function);
        assertTrue(proof.closed(),
            () -> SolidityExampleTests.describeOpenGoals(contract + "." + function, proof)
                    + SolidityExampleTests.budgetHint(DIRECTORY, contract));
    }

    static Stream<Arguments> examples() throws IOException {
        return SolidityExampleTests.contractFunctions(DIRECTORY);
    }
}
