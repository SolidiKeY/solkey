/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.taclets.builder;


import org.key_project.logic.Term;
import org.key_project.prover.rules.ApplicationRestriction;
import org.key_project.prover.rules.TacletApplPart;
import org.key_project.prover.rules.tacletbuilder.TacletGoalTemplate;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.rule.taclets.SolRewriteTaclet;

public class RewriteTacletBuilder<T extends SolRewriteTaclet> extends FindTacletBuilder<T> {
    public RewriteTacletBuilder<T> setApplicationRestriction(
            ApplicationRestriction p_applicationRestriction) {
        applicationRestriction = p_applicationRestriction;
        return this;
    }


    /* for information flow purposes; TODO: find better solution */
    protected boolean surviveSmbExec;

    public void setSurviveSmbExec(boolean b) {
        surviveSmbExec = b;
    }


    /// sets the _find_ of the Taclet that is to build to the given term.
    ///
    /// @return this RewriteTacletBuilder
    public RewriteTacletBuilder<T> setFind(Term findTerm) {
        checkContainsFreeVarSV(findTerm, this.getName(), "find term");
        find = findTerm;
        return this;
    }

    /// builds and returns the RewriteTaclet that is specified by former set... / add... methods. If
    /// no name is specified then an Taclet with an empty string name is build. No specifications
    /// for
    /// variable conditions, goals or heuristics imply that the corresponding parts of the Taclet
    /// are
    /// empty. No specification for the if-sequent is represented as a sequent with two empty
    /// semisequents. No specification for the interactive or recursive flags imply that the flags
    /// are not set. No specified find part causes an TacletBuilderException. Throws an
    /// TacletBuilderException if a bound SchemaVariable occurs more than once in if and find or an
    /// InvalidPrefixException if the building of the Taclet Prefix fails.
    @SuppressWarnings("unchecked")
    public T getRewriteTaclet(Services services) {
        if (find == null) {
            throw new TacletBuilder.TacletBuilderException(this, "No find part specified");
        }
        checkBoundInIfAndFind();
        TacletPrefixBuilder prefixBuilder = new TacletPrefixBuilder(this, services);
        prefixBuilder.build();
        SolRewriteTaclet t = new SolRewriteTaclet(name,
            (Term) find,
            new TacletApplPart(ifseq, applicationRestriction, varsNew, varsNotFreeIn,
                varsNewDependingOn, variableConditions),
            goals, ruleSets, attrs, prefixBuilder.getPrefixMap(),
            choices, surviveSmbExec, tacletAnnotations, noFreeVarIns);
        // t.setOrigin(origin);
        return (T) t;
    }

    /// adds a new goal descriptions to the goal descriptions of the Taclet. the TacletGoalTemplate
    /// must not be an AntecSuccTacletGoalTemplate, otherwise an illegal argument exception is
    /// thrown.
    @Override
    public void addTacletGoalTemplate(TacletGoalTemplate goal) {
        if (goal instanceof AntecSuccTacletGoalTemplate) {
            throw new IllegalArgumentException(
                "Tried to add a AntecSucc" + "GoalTemplate to a Rewrite" + "Taclet");
        }

        goals = goals.prepend(goal);
    }

    /// builds and returns the Taclet that is specified by former set... / add... methods. If no
    /// name is specified then a Taclet with an empty string name is build. No specifications for
    /// variable conditions, goals or heuristics imply that the corresponding parts of the Taclet
    /// are empty. No specification for the assumes-sequence is represented as a sequent with two empty
    /// semisequences. No specification for the interactive or recursive flags imply that the flags
    /// are not set.
    /// @throws IllegalStateException if no find is specified.
    @Override
    public T getTaclet(Services services) {
        return getRewriteTaclet(services);
    }
}
