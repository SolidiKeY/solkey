/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.pp.PosInSequent;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.rule.TacletApp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModalityTacletTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void offersModalityRuleForTheModalityFormula() throws Exception {
        Path file = Path.of("keyext.solidity.examples/functionBody/archive.key");
        if (!Files.exists(file))
            file = Path.of("../keyext.solidity.examples/functionBody/archive.key");
        KeYEnvironment<?> env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        ProofContext context = new ProofContext();
        SequentView view = new SequentView(context);
        context.setProof(env, proof);

        String s = view.renderedText();
        int off = s.indexOf("archive"); // inside the modality program
        PosInSequent pis = view.posAt(off);
        Goal goal = proof.openGoals().head();
        List<TacletApp> apps = view.applicableTaclets(goal, pis.getPosInOccurrence());
        List<String> names = apps.stream().map(a -> a.taclet().name().toString()).toList();
        assertTrue(names.contains("functionBodyExpand"),
            "the modality formula should offer functionBodyExpand; got: " + names);
    }
}
