/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.taclets;

import org.key_project.logic.Term;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.rules.Taclet;
import org.key_project.prover.rules.Trigger;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.tacletbuilder.TacletGoalTemplate;
import org.key_project.prover.sequent.Sequent;
import org.key_project.util.collection.ImmutableList;

public interface TacletVisitor {
    /// visits all parts of the provided taclet: assumes, find, variable conditions, goal templates,
    /// rulesets and triggers but does not descend into the rules added by taclet goal templates
    default void visit(Taclet taclet) {
        visit(taclet, false);
    }

    /// visits all parts of the provided taclet: assumes, find, variable conditions, goal templates,
    /// rulesets and triggers
    default void visit(Taclet taclet, boolean visitAddRules) {
        visitAssumes(taclet.assumesSequent());
        if (taclet instanceof SolFindTaclet findTaclet) {
            visitFind(findTaclet.find());
        }
        visitVariableConditions(taclet.getVariableConditions());
        visitGoalTemplates(taclet.goalTemplates(), visitAddRules);
        visitRuleSets(taclet.getRuleSets());
        visitTrigger(taclet.getTrigger());
    }

    /// implement visiting
    default void visitTrigger(Trigger trigger) {
    }

    default void visitRuleSets(ImmutableList<RuleSet> rulesets) {
    }

    default void visitVariableConditions(
            ImmutableList<? extends VariableCondition> variableConditions) {
    }

    default void visitGoalTemplates(ImmutableList<TacletGoalTemplate> goalTemplates,
            boolean visitAddRules) {
    }

    default void visitAssumes(Sequent assumes) {
    }

    default void visitFind(Term findTerm) {
    }
}
