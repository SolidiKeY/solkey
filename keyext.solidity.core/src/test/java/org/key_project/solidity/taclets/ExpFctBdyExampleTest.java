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

/// Loads the `keyext.solidity.examples/functionBody/expFctBdy.key` showcase: a call
/// `res = expFctBdy(a, b)@C` is inlined via function-body expansion (parameter bindings, body
/// and result assignment), its net effect is executed and the resulting field/result relation
/// (`res = 3` and field `f = a`) is proved (closes via automode).
public class ExpFctBdyExampleTest {

    private static Path example() {
        Path p = Path.of("keyext.solidity.examples/functionBody/expFctBdy.key");
        return Files.exists(p) ? p
                : Path.of("../keyext.solidity.examples/functionBody/expFctBdy.key");
    }

    @Test
    void expFctBdyExampleCloses() throws Exception {
        Path file = example();
        assertTrue(Files.exists(file), "example must exist: " + file.toAbsolutePath());

        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        env.getProofControl().startAndWaitForAutoMode(proof);

        assertTrue(proof.closed(),
            "expFctBdy expansion proof should close; open goals: " + proof.openGoals().size());
    }
}
