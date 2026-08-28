/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.proveTestSuiteFunction;

/// Runs the end-to-end showcase functions — the `test*` functions of
/// `keyext.solidity.examples/TestSuite.sol`.
///
/// There are no `.key` problem files. The loader synthesizes the obligation for each function —
/// a call in a modality with postcondition `true` — and the in-body `assert(a == b)` statements
/// carry the proof obligations (per `docs/require-assert.md`). The focused one-rule-each
/// functions run in [TacletStarterExamplesTest].
public class PaperTestExamplesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void paperTestExampleCloses(String function) throws Exception {
        Proof proof = proveTestSuiteFunction(function, 50000, 30000);
        assertTrue(proof.closed(),
            () -> function + " should close; open goals: " + proof.openGoals().size()
                + "; first open goal: " + proof.openGoals().head().sequent());
    }

    static Stream<Arguments> examples() throws IOException {
        return SolidityExampleTests.testSuiteFunctions(name -> name.startsWith("test"));
    }
}
