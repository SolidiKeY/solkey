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

/// Loads the user-visible `keyext.solidity.examples/fieldAccess/fieldAccess.key` showcase and
/// exercises the
/// `#fieldSymbol` infrastructure: a contract field access resolves to its registered
/// `Field`-sorted constant `Bank$balance`.
public class FieldAccessExampleTest {

    private static Path exampleDir() {
        // tests run with the module directory as working directory
        Path p = Path.of("keyext.solidity.examples/fieldAccess/fieldAccess.key");
        if (Files.exists(p)) {
            return p;
        }
        return Path.of("../keyext.solidity.examples/fieldAccess/fieldAccess.key");
    }

    @Test
    void resolvesFieldSymbol() throws Exception {
        Path file = exampleDir();
        assertTrue(Files.exists(file), "example file must exist: " + file.toAbsolutePath());

        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        Goal goal = proof.openGoals().head();

        SequentFormula sf = goal.sequent().succedent().get(0);
        PosInOccurrence pos = new PosInOccurrence(sf, PosInTerm.getTopLevel(), false);
        ImmutableList<TacletApp> apps = env.getProofControl().getFindTaclet(goal, pos);
        TacletApp app = null;
        for (TacletApp a : apps) {
            if (a.taclet().name().toString().equals("fieldAssignStoreThenRead")) {
                app = a;
                break;
            }
        }
        assertNotNull(app, "fieldAssignStoreThenRead should be applicable at the modality");
        app = app.setPosInOccurrence(pos, proof.getServices());
        assertTrue(app.complete(), "rule should be complete once positioned");

        goal.apply(app);

        // after: the field write became a storeSt update on contract storage and the post
        // reads the field back via selectSt; the field access resolved to the constant
        // Bank$balance (looked up lazily by name).
        Goal newGoal = proof.openGoals().head();
        Term after = newGoal.sequent().succedent().get(0).formula();
        String s = after.toString();
        assertTrue(s.contains("storeSt"), "store should appear, was: " + s);
        assertTrue(s.contains("selectSt"), "read-back select should appear, was: " + s);
        assertTrue(s.contains("Bank$balance"),
            "resolved field constant should appear, was: " + s);

        // the existing struct rule selectIntOnStore collapses the store-then-read round-trip,
        // so automode closes the goal: the field really does hold the written value.
        env.getProofControl().startAndWaitForAutoMode(proof);
        assertTrue(proof.closed(),
            "store-then-read round-trip should close via selectIntOnStore");
    }
}
