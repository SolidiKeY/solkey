/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.loadAndProve;

/// Exercises the user-visible pre-licenciate-paper taclet starters. These examples live in the
/// filesystem examples module, not in the test resources scanned by [RulesTest].
///
/// Each problem calls a single function of `keyext.solidity.examples/TestSuite.sol` in a modality.
/// The `net-*` family still inlines its program, because `msg.*` and `.transfer` are not yet
/// supported by the solc-JSON parsing path.
public class TacletStarterExamplesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void tacletStarterExampleCloses(String name, Path file) throws Exception {
        Proof proof = loadAndProve(file, 10000, SolidityExampleTests.KEEP_TIMEOUT);
        assertTrue(proof.closed(),
            () -> name + " should close; open goals: " + proof.openGoals().size()
                + "; first open goal: " + proof.openGoals().head().sequent());
    }

    static Stream<Arguments> examples() throws IOException {
        return SolidityExampleTests.exampleProblems("taclets");
    }
}
