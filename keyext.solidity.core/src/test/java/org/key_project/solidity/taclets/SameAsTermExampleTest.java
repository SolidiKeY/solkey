/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.nio.file.Files;
import java.nio.file.Path;

import org.key_project.logic.PosInTerm;
import org.key_project.logic.Term;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.rule.TacletApp;
import org.key_project.util.collection.ImmutableList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Loads the user-visible `keyext.solidity.examples/fieldAccess/sameAsTerm.key` showcase and checks
/// that the
/// `\sameAsTerm` variable condition binds the term schema variable to the program field access:
/// applying the rule introduces the field constant `Bank$balance`, and the store-then-read
/// round-trip then closes.
public class SameAsTermExampleTest {

    private static Path example() {
        Path p = Path.of("keyext.solidity.examples/fieldAccess/sameAsTerm.key");
        return Files.exists(p) ? p
                : Path.of("../keyext.solidity.examples/fieldAccess/sameAsTerm.key");
    }

    @Test
    void sameAsTermBindsAndCloses() throws Exception {
        Path file = example();
        assertTrue(Files.exists(file), "example must exist: " + file.toAbsolutePath());

        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        Goal goal = proof.openGoals().head();

        SequentFormula sf = goal.sequent().succedent().get(0);
        PosInOccurrence pos = new PosInOccurrence(sf, PosInTerm.getTopLevel(), false);
        ImmutableList<TacletApp> apps = env.getProofControl().getFindTaclet(goal, pos);
        TacletApp app = null;
        for (TacletApp a : apps) {
            if (a.taclet().name().toString().equals("fieldWriteThenReadViaTerm")) {
                app = a;
                break;
            }
        }
        assertNotNull(app, "fieldWriteThenReadViaTerm should be applicable");
        app = app.setPosInOccurrence(pos, proof.getServices());
        assertTrue(app.complete(), "rule should be complete once positioned");

        goal.apply(app);

        // \sameAsTerm bound s to the conversion of fr, so the field constant now appears
        Goal newGoal = proof.openGoals().head();
        Term after = newGoal.sequent().succedent().get(0).formula();
        assertTrue(after.toString().contains("Bank$balance"),
            "the term schema variable should have been bound to the field constant, was: " + after);

        env.getProofControl().startAndWaitForAutoMode(proof);
        assertTrue(proof.closed(), "the store-then-read round-trip should close");
    }
}
