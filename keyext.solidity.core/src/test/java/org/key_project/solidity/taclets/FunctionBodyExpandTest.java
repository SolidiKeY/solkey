/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.taclets;

import java.net.URL;
import java.nio.file.Path;

import org.key_project.logic.PosInTerm;
import org.key_project.logic.Term;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.solidity.control.KeYEnvironment;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.FunctionBodyStatement;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.rule.TacletApp;
import org.key_project.util.collection.ImmutableList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// End-to-end test: a `.key` problem loads a Solidity contract via `\programSource`, places a
/// `withdraw(a)@Contract;` call (a [FunctionBodyStatement]) inside a modality, and the
/// `functionBodyExpand` taclet declared in the same file rewrites it to the inlined function body.
public class FunctionBodyExpandTest {

    private static Block modalityProgram(Term formula) {
        assertInstanceOf(SModality.class, formula.op(), "succedent formula must be a modality");
        SolidityBlock sb = ((SModality) formula.op()).programBlock();
        return (Block) sb.program();
    }

    @Test
    void expandsFunctionBodyInsideModality() throws Exception {
        URL res = getClass().getClassLoader()
                .getResource("org/key_project/solidity/functionbody/withdrawExpand.key");
        assertNotNull(res, "test resource withdrawExpand.key must exist");
        Path file = Path.of(res.toURI());

        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        Goal goal = proof.openGoals().head();

        // before: the modality program is the (not yet inlined) function-body statement
        SequentFormula sf = goal.sequent().succedent().get(0);
        Block before = modalityProgram(sf.formula());
        assertInstanceOf(FunctionBodyStatement.class, before.getStatements().get(0),
            "modality should start with a function-body statement");

        // find and apply the functionBodyExpand taclet at the modality formula
        PosInOccurrence pos = new PosInOccurrence(sf, PosInTerm.getTopLevel(), false);
        ImmutableList<TacletApp> apps = env.getProofControl().getFindTaclet(goal, pos);
        TacletApp expandApp = null;
        for (TacletApp app : apps) {
            if (app.taclet().name().toString().equals("functionBodyExpand")) {
                expandApp = app;
                break;
            }
        }
        assertNotNull(expandApp, "functionBodyExpand should be applicable at the modality");

        // the index returns position-less apps; fix the application position
        expandApp = expandApp.setPosInOccurrence(pos, proof.getServices());
        assertTrue(expandApp.complete(), "functionBodyExpand should be complete once positioned");

        ImmutableList<Goal> result = goal.apply(expandApp);
        assertNotNull(result);

        // after: the function-body statement is replaced by the parameter declaration(s)
        // followed by the inlined body block
        Goal newGoal = proof.openGoals().head();
        Block after =
            modalityProgram(newGoal.sequent().succedent().get(0).formula());

        assertTrue(after.getStatements().size() >= 2,
            "expanded program should contain parameter declarations and the body");
        assertInstanceOf(DeclarationStatement.class, after.getStatements().get(0),
            "first statement should be the inlined parameter declaration");
        assertInstanceOf(Block.class, after.getStatements().get(after.getStatements().size() - 1),
            "last statement should be the inlined function body block");
        // and the function-body placeholder is gone
        for (var st : after.getStatements()) {
            assertEquals(false, st instanceof FunctionBodyStatement,
                "no function-body statement should remain after expansion");
        }
    }
}
