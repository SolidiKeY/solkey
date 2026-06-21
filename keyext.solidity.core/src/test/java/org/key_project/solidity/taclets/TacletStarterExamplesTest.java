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
        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        var strategySettings = proof.getSettings().getStrategySettings();
        strategySettings.setMaxSteps(10000);
        env.getProofControl().startAndWaitForAutoMode(proof);

        assertTrue(proof.closed(),
            () -> name + " should close; open goals: " + proof.openGoals().size()
                + "; first open goal: " + proof.openGoals().head().sequent());
    }

    static Stream<Arguments> examples() {
        return Stream.of(
            example("storage-root-read-write.key"),
            example("storage-root-copy-source.key"),
            example("storage-root-paths.key"),
            example("storage-field-decomposition.key"),
            example("storage-field-global-age.key"),
            example("storage-field-deep-value.key"),
            example("storage-index-decomposition.key"),
            example("storage-index-root-array.key"),
            example("storage-index-root-mapping.key"),
            example("storageRulesExamples.key"));
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
