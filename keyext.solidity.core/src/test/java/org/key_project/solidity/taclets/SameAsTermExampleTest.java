/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;


import org.key_project.logic.Term;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.proof.Proof;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.applyTacletAtTop;
import static org.key_project.solidity.testutil.SolidityExampleTests.example;
import static org.key_project.solidity.testutil.SolidityExampleTests.load;

/// Loads the user-visible `keyext.solidity.examples/fieldAccess/sameAsTerm.key` showcase and checks
/// that the
/// `\sameAsTerm` variable condition binds the term schema variable to the program field access:
/// applying the rule introduces the field constant `Bank$balance`, and the store-then-read
/// round-trip then closes.
@Tag("solidityExamples")
public class SameAsTermExampleTest {

    @Test
    void sameAsTermBindsAndCloses() throws Exception {
        KeYEnvironment env = load(example("fieldAccess/sameAsTerm.key"));
        Proof proof = env.getLoadedProof();

        Term after = applyTacletAtTop(env, "fieldWriteThenReadViaTerm");
        assertTrue(after.toString().contains("Bank$balance"),
            "the term schema variable should have been bound to a List containing the field constant, was: "
                + after);

        env.getProofControl().startAndWaitForAutoMode(proof);
        assertTrue(proof.closed(), "the store-then-read round-trip should close");
    }
}
