/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;

import org.junit.jupiter.api.Test;

import static org.key_project.solidity.testutil.SolidityExampleTests.TEST_SUITE_CONTRACT;
import static org.key_project.solidity.testutil.SolidityExampleTests.load;
import static org.key_project.solidity.testutil.SolidityExampleTests.testSuite;

/// Diagnostic test: runs testStorageWriteAndRead with a small step limit and prints open goals.
/// Not meant to close — used to inspect proof state.
public class ProofDiagnosticTest {

    @Test
    void diagnoseOpenGoals() throws Exception {
        KeYEnvironment env =
            load(testSuite(), TEST_SUITE_CONTRACT, "testStorageWriteAndRead");
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
