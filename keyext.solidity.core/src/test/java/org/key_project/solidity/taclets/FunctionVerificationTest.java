/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.nio.file.Path;

import org.key_project.logic.Term;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.io.ProblemLoaderException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.testutil.SolidityExampleTests.KEEP_TIMEOUT;
import static org.key_project.solidity.testutil.SolidityExampleTests.example;
import static org.key_project.solidity.testutil.SolidityExampleTests.loadAndProveFunction;
import static org.key_project.solidity.testutil.SolidityExampleTests.loadFunction;

/// Verifies a Solidity function directly from a `.sol` file, without a hand-written `.key`
/// problem file: the proof obligation `[{ body }] true` is synthesized in memory, so the
/// leading `require` acts as a precondition and every `assert` is an obligation.
public class FunctionVerificationTest {

    private static Path solFile() {
        return example("taclets/FunctionVerification.sol");
    }

    @Test
    void loadCreatesBoxProofObligation() throws ProblemLoaderException {
        KeYEnvironment env = loadFunction(solFile(), "FunctionVerification", "deposit");
        Proof proof = env.getLoadedProof();
        assertNotNull(proof, "loading a .sol function must create a proof");
        assertEquals("FunctionVerification::deposit", proof.name().toString());

        var sequent = proof.root().sequent();
        assertEquals(0, sequent.antecedent().size());
        assertEquals(1, sequent.succedent().size());
        Term formula = sequent.succedent().get(0).formula();
        assertInstanceOf(SModality.class, formula.op(),
            "the proof obligation must be a modality formula");
        assertEquals(SModality.SolidityModalityKind.BOX, ((SModality) formula.op()).kind(),
            "require/assert semantics needs the box modality");
    }

    @Test
    void functionWithRequireAndAssertCloses() throws ProblemLoaderException {
        Proof proof =
            loadAndProveFunction(solFile(), "FunctionVerification", "deposit", 10000,
                KEEP_TIMEOUT);
        assertTrue(proof.closed(),
            "deposit should verify: " + proof.openGoals().size() + " goals remain");
    }

    @Test
    void contractMayBeOmittedWhenUnambiguous() throws ProblemLoaderException {
        KeYEnvironment env = loadFunction(solFile(), null, "deposit");
        assertNotNull(env.getLoadedProof());
        assertEquals("deposit", env.getLoadedProof().name().toString());
    }

    @Test
    void unknownFunctionReportsAvailableOnes() {
        var e = assertThrows(ProblemLoaderException.class,
            () -> loadFunction(solFile(), "FunctionVerification", "withdraw"));
        assertTrue(rootMessage(e).contains("withdraw"),
            "error should name the missing function: " + rootMessage(e));
        assertTrue(rootMessage(e).contains("deposit"),
            "error should list the available functions: " + rootMessage(e));
    }

    @Test
    void unknownContractReportsAvailableOnes() {
        var e = assertThrows(ProblemLoaderException.class,
            () -> loadFunction(solFile(), "NoSuchContract", "deposit"));
        assertTrue(rootMessage(e).contains("NoSuchContract"),
            "error should name the missing contract: " + rootMessage(e));
        assertTrue(rootMessage(e).contains("FunctionVerification"),
            "error should list the available contracts: " + rootMessage(e));
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return String.valueOf(cur.getMessage());
    }
}
