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

/// Loads the `keyext.solidity.examples/freshVar/freshTemp.key` showcase: a rule introduces a fresh
/// program variable with `\new` and executes two assignments one at a time through a context block,
/// proving `q = 42`. Exercises ProgramContextAdder (per-statement execution) and the fresh-variable
/// mechanism together (closes via automode).
public class FreshTempExampleTest {

    private static Path example() {
        Path p = Path.of("keyext.solidity.examples/freshVar/freshTemp.key");
        return Files.exists(p) ? p
                : Path.of("../keyext.solidity.examples/freshVar/freshTemp.key");
    }

    @Test
    void freshTempExampleCloses() throws Exception {
        Path file = example();
        assertTrue(Files.exists(file), "example must exist: " + file.toAbsolutePath());

        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        env.getProofControl().startAndWaitForAutoMode(proof);

        assertTrue(proof.closed(),
            "\\new + context-block proof should close; open goals: " + proof.openGoals().size());
    }
}
