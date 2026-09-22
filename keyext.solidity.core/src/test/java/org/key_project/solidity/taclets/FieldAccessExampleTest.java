/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.nio.file.Path;

import org.key_project.logic.Term;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.applyNamedTacletAtTop;
import static org.key_project.solidity.testutil.SolidityExampleTests.example;
import static org.key_project.solidity.testutil.SolidityExampleTests.load;

/// Loads the user-visible `keyext.solidity.examples/fieldAccess/fieldAccess.key` showcase and
/// exercises the
/// `#fieldSymbol` infrastructure: a contract field access resolves to its registered
/// `Field`-sorted constant `Bank$balance`.
@Tag("solidityExamples")
public class FieldAccessExampleTest {

    @Test
    void resolvesFieldSymbol() throws Exception {
        Path file = example("fieldAccess/fieldAccess.key");

        KeYEnvironment env = load(file);
        Proof proof = env.getLoadedProof();
        Goal goal = proof.openGoals().head();

        applyNamedTacletAtTop(env, proof, goal, "fieldAssignStoreThenRead");

        // after: the field write became a save update on contract storage and the post
        // reads the field back via find; the field access resolved to the constant
        // Bank$balance (looked up lazily by name) wrapped in a List.
        Goal newGoal = proof.openGoals().head();
        Term after = newGoal.sequent().succedent().get(0).formula();
        String s = after.toString();
        assertTrue(s.contains("save"), "save should appear, was: " + s);
        assertTrue(s.contains("find"), "read-back find should appear, was: " + s);
        assertTrue(s.contains("Bank$balance"),
            "resolved field constant should appear, was: " + s);

        // the existing struct rule findAfterSave collapses the save-then-find round-trip,
        // so automode closes the goal: the field really does hold the written value.
        env.getProofControl().startAndWaitForAutoMode(proof);
        assertTrue(proof.closed(),
            "save-then-find round-trip should close via findAfterSave");
    }
}
