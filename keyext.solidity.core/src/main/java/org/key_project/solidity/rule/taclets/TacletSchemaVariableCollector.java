/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.taclets;

import java.util.LinkedHashSet;
import java.util.Set;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.Taclet;
import org.key_project.prover.rules.Trigger;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.util.collection.ImmutableList;

public class TacletSchemaVariableCollector implements TacletVisitor {


    private final Set<SchemaVariable> collectedSchemaVariables = new LinkedHashSet<>();


    @Override
    public void visitFind(Term findTerm) {
        visitSyntaxElement(findTerm);
    }

    @Override
    public void visitAssumes(Sequent assumes) {
        visitSequent(assumes);
    }

    public void visitSequent(Sequent assumes) {
        for (final SequentFormula sf : assumes) {
            visitSyntaxElement(sf);
        }
    }

    @Override
    public void visitGoalTemplates(
            ImmutableList<org.key_project.prover.rules.tacletbuilder.TacletGoalTemplate> goalTemplates,
            boolean visitAddRules) {
        for (final var tgt : goalTemplates) {
            collectedSchemaVariables.addAll(tgt.addedProgVars().toSet());
            for (final QuantifiableVariable qvar : tgt.getBoundVariables()) {
                if (qvar instanceof SchemaVariable qsv) {
                    collectedSchemaVariables.add(qsv);
                }
            }

            // replacewith-part of goals
            if (tgt.replaceWith() != null) {
                visitSyntaxElement(tgt.replaceWith());
            }
            // add-part of goals
            visitSequent(tgt.sequent());

            if (visitAddRules) {
                for (final Taclet addedRule : tgt.rules()) {
                    visit(addedRule);
                }
            }
        }
    }

    @Override
    public void visitTrigger(Trigger trigger) {
        TacletVisitor.super.visitTrigger(trigger);
    }

    @Override
    public void visitVariableConditions(
            ImmutableList<? extends VariableCondition> variableConditions) {
        // no unique interface to access SVs occuring in variable conditions, hence we do nothing
        // here for the moment
    }

    public Set<SchemaVariable> getCollectedSchemaVariables() {
        return collectedSchemaVariables;
    }

    public void visitSyntaxElement(SyntaxElement visited) {
        if (visited instanceof Sequent seq) {
            // at the moment just for optimisation as iterating via
            // getChild on sequent is otherwise quadratic
            visitSequent(seq);
        }
        if (visited instanceof SchemaVariable sv) {
            collectedSchemaVariables.add(sv);
        }
        for (int i = 0; i < visited.getChildCount(); i++) {
            visitSyntaxElement(visited.getChild(i));
        }
    }
}
