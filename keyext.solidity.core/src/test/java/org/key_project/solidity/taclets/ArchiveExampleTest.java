/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Proof;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Loads the user-visible `keyext.solidity.examples/functionBody/archive.key` showcase: a contract
/// function that
/// copies one field into another is inlined via function-body expansion and the resulting field
/// relation is proved (closes via automode).
public class ArchiveExampleTest {

    private static Path example() {
        Path p = Path.of("keyext.solidity.examples/functionBody/archive.key");
        return Files.exists(p) ? p
                : Path.of("../keyext.solidity.examples/functionBody/archive.key");
    }

    @Test
    void archiveExampleCloses() throws Exception {
        Path file = example();
        assertTrue(Files.exists(file), "example must exist: " + file.toAbsolutePath());

        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        env.getProofControl().startAndWaitForAutoMode(proof);

        assertTrue(proof.closed(),
            "archive() field-copy proof should close; open goals: " + proof.openGoals().size());
    }
}
