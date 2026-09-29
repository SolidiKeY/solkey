/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the ports of the solc compiler's own semantic tests
/// (`keyext.solidity.examples/solc/`), which cross-check the calculus against an externally
/// authored description of Solidity semantics.
///
/// Every `.sol` beside the directory is enumerated and every function an obligation can be
/// generated for is proved, so a new example joins the suite by being written — the contract
/// name is taken from the file name.
///
/// Seven examples started out red and were fixed by the rule and parser changes the port
/// prompted; they stay here as regression tests. The gaps they found are listed in
/// `keyext.solidity.examples/solc/README.md`.
@Tag("solidityExamples")
public class SolcSemanticsExamplesTest {

    private static final String DIRECTORY = "solc";

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("examples")
    void solcSemanticsExampleCloses(String contract, String function) throws Exception {
        Path sol = SolidityExampleTests.example(DIRECTORY + "/" + contract + ".sol");
        Proof proof = SolidityExampleTests.proveFunction(sol, contract, function, 50000, 30000);
        assertTrue(proof.closed(),
            () -> SolidityExampleTests.describeOpenGoals(contract + "." + function, proof));
    }

    static Stream<Arguments> examples() throws IOException {
        return SolidityExampleTests.contractFunctions(DIRECTORY);
    }
}
