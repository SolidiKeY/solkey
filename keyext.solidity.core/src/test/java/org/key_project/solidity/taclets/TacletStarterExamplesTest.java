/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Proof;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Exercises the user-visible pre-licenciate-paper taclet starters. These examples live in the
/// filesystem examples module, not in the test resources scanned by [RulesTest].
public class TacletStarterExamplesTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("examples")
    void tacletStarterExampleCloses(String name, Path file) throws Exception {
        Proof proof = loadAndRun(file);
        assertTrue(proof.closed(),
            () -> name + " should close; open goals: " + proof.openGoals().size()
                + "; first open goal: " + proof.openGoals().head().sequent());
    }

    private static Proof loadAndRun(Path file) throws Exception {
        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        var strategySettings = proof.getSettings().getStrategySettings();
        strategySettings.setMaxSteps(10000);
        env.getProofControl().startAndWaitForAutoMode(proof);
        return proof;
    }

    static Stream<Arguments> examples() {
        return Stream.of(
            example("storage-root-read-write.key"),
            example("storage-root-copy-source.key"),
            example("storage-root-multiple-writes.key"),
            example("storage-root-disjoint.key"),
            example("storage-field-global-age.key"),
            example("storage-field-write-read.key"),
            example("storage-field-disjoint-roots.key"),
            example("storage-field-decomposition.key"),
            example("storage-field-deep-write-read.key"),
            example("storage-field-deep-value.key"),
            example("storage-index-root-array.key"),
            example("storage-index-multiple-writes.key"),
            example("storage-field-disjoint-fields.key"),
            example("storage-index-decomposition.key"),
            example("storage-matrix-write-read.key"),
            example("storage-alias-write-balance.key"),
            example("storage-alias-write-token.key"),
            example("storage-alias-rebind-original.key"),
            example("storage-alias-rebind-alias.key"),
            example("storage-local-decl-skip.key"),
            example("storage-field-read-bind-local.key"),
            example("storage-field-read-store-root.key"),
            example("storage-root-preincrement.key"),
            example("storage-root-postincrement.key"),
            example("storage-root-predecrement.key"),
            example("storage-root-preincrement-assign.key"),
            example("storage-root-postincrement-assign.key"),
            example("storage-root-postdecrement-assign.key"),
            example("storage-field-preincrement.key"),
            example("storage-field-postincrement-assign.key"));
    }

    private static Arguments example(String name) {
        Path path = examplesDir().resolve(name);
        assertTrue(Files.exists(path), "example must exist: " + path.toAbsolutePath());
        return Arguments.of(name, path);
    }

    private static Path examplesDir() {
        Path path = Path.of("keyext.solidity.examples/taclets");
        return Files.exists(path) ? path : Path.of("../keyext.solidity.examples/taclets");
    }
}
