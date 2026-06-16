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
import org.key_project.solidity.program.ast.expressions.operators.AssignExpression;
import org.key_project.solidity.program.ast.expressions.operators.Operator;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.FunctionBodyStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.rule.TacletApp;
import org.key_project.util.collection.ImmutableList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// End-to-end test for the *result-value* connection of function-body expansion: a `.key`
/// problem loads a stateful contract via `\programSource`, places `r = getBalance()@Bank;`
/// (a [FunctionBodyStatement] with a result variable) inside a modality, and the
/// `functionBodyExpand` taclet rewrites it to the inlined body, ending with the assignment
/// `r = <named-return>;` that connects the function's return value to the surrounding `r`.
public class FunctionBodyResultExpandTest {

    private static Block modalityProgram(Term formula) {
        assertInstanceOf(SModality.class, formula.op(), "succedent formula must be a modality");
        SolidityBlock sb = ((SModality) formula.op()).programBlock();
        return (Block) sb.program();
    }

    @Test
    void expandsBodyAndConnectsResultVariable() throws Exception {
        URL res = getClass().getClassLoader()
                .getResource("org/key_project/solidity/functionbody/bankExpand.key");
        assertNotNull(res, "test resource bankExpand.key must exist");
        Path file = Path.of(res.toURI());

        KeYEnvironment env = KeYEnvironment.load(file);
        Proof proof = env.getLoadedProof();
        Goal goal = proof.openGoals().head();

        // before: the modality program is the (not yet inlined) function-body statement
        SequentFormula sf = goal.sequent().succedent().get(0);
        Block before = modalityProgram(sf.formula());
        FunctionBodyStatement fbs =
            assertInstanceOf(FunctionBodyStatement.class, before.getStatements().get(0),
                "modality should start with a function-body statement");
        assertNotNull(fbs.getResultVar(), "the call should carry a result variable");

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
        expandApp = expandApp.setPosInOccurrence(pos, proof.getServices());
        assertTrue(expandApp.complete(), "functionBodyExpand should be complete once positioned");

        ImmutableList<Goal> result = goal.apply(expandApp);
        assertNotNull(result);

        // after: the placeholder is gone and the last statement is the result assignment
        Goal newGoal = proof.openGoals().head();
        Block after = modalityProgram(newGoal.sequent().succedent().get(0).formula());

        for (Statement st : after.getStatements()) {
            assertEquals(false, st instanceof FunctionBodyStatement,
                "no function-body statement should remain after expansion");
        }

        Statement last = after.getStatements().get(after.getStatements().size() - 1);
        ExpressionStatement assignStmt = assertInstanceOf(ExpressionStatement.class, last,
            "last statement should be the result assignment r = <return>;");
        AssignExpression assign = assertInstanceOf(AssignExpression.class,
            assignStmt.getExpression(), "result assignment should be an assignment expression");
        assertEquals(Operator.COPY_ASSIGN, assign.getOperator(), "result assignment uses '='");
        assertEquals(fbs.getResultVar(), assign.getLeft(),
            "left-hand side of the result assignment is the call's result variable");
    }
}
