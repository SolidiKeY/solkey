/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;

import org.junit.jupiter.api.Test;

/// Diagnostic test: runs testStorageWriteAndRead with a small step limit and prints open goals.
/// Not meant to close — used to inspect proof state.
public class ProofDiagnosticTest {

    private static Path examplesDir() {
        Path path = Path.of("keyext.solidity.examples/mainFeatures");
        return Files.exists(path) ? path : Path.of("../keyext.solidity.examples/mainFeatures");
    }

    @Test
    void diagnoseOpenGoals() throws Exception {
        Path file = examplesDir().resolve("testStorageWriteAndRead.key");
        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();

        var strategySettings = proof.getSettings().getStrategySettings();
        strategySettings.setMaxSteps(30);
        strategySettings.setTimeout(-1);
        env.getProofControl().startAndWaitForAutoMode(proof);

        System.out.println("=== Proof statistics ===");
        System.out.println(proof.getStatistics());
        System.out.println("=== Open goals: " + proof.openGoals().size() + " ===");
        int i = 0;
        for (Goal g : proof.openGoals()) {
            System.out.println("--- Goal " + i + " ---");
            System.out.println(g.sequent());
            i++;
        }
    }
}
