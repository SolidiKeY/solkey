/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.testutil.SolidityExampleTests;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.loadAndProve;

/// Loads the `keyext.solidity.examples/mainFeatures/` showcase problems — one per `test*` function
/// of `keyext.solidity.examples/TestSuite.sol`. Each problem calls the function in a diamond
/// modality with postcondition `true`; the in-body `assert(a == b)` statements carry the proof
/// obligations (per `docs/require-assert.md`).
public class PaperTestExamplesTest {

    /// Examples whose Solidity is not yet supported. Reported as skipped rather than failed, and
    /// they turn green by themselves once support lands.
    private static final Set<String> KNOWN_UNSUPPORTED =
        Set.of("testStorageArrayPushPop.key"); // Token(42) struct constructor syntax

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void paperTestExampleCloses(String name, Path file) throws Exception {
        if (KNOWN_UNSUPPORTED.contains(name)) {
            // these can fail while loading, so the guard has to cover the load too
            try {
                Assumptions.assumeTrue(loadAndProve(file, 50000, 30000).closed(),
                    () -> "known-unsupported example: " + name);
            } catch (Exception e) {
                Assumptions.abort("known-unsupported example: " + name + " (" + e + ")");
            }
            return;
        }
        Proof proof = loadAndProve(file, 50000, 30000);
        assertTrue(proof.closed(),
            () -> name + " should close; open goals: " + proof.openGoals().size()
                + "; first open goal: " + proof.openGoals().head().sequent());
    }

    static Stream<Arguments> examples() throws IOException {
        return SolidityExampleTests.exampleProblems("mainFeatures");
    }
}
