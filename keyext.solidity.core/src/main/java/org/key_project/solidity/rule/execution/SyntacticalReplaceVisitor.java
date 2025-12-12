/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.execution;

import org.key_project.logic.Term;
import org.key_project.logic.Visitor;
import org.key_project.prover.rules.Rule;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

public class SyntacticalReplaceVisitor implements Visitor<Term> {
    protected final PosInOccurrence applicationPosInOccurrence;
    protected final SVInstantiations svInst;
    protected final Goal goal;
    protected final Rule rule;
    protected final RuleApp ruleApp;
    protected final Services services;

    private Term computedResult = null;
    /// the termbuilder used to construct terms
    protected final TermBuilder tb;

    public SyntacticalReplaceVisitor(PosInOccurrence applicationPosInOccurrence,
            SVInstantiations svInst, Goal goal, Rule rule, RuleApp ruleApp, Services services) {
        this.applicationPosInOccurrence = applicationPosInOccurrence;
        this.svInst = svInst;
        this.goal = goal;
        this.rule = rule;
        this.ruleApp = ruleApp;
        this.services = services;
        this.tb = services.getTermBuilder();
    }

    public Term getTerm() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void visit(Term visited) {
        throw new RuntimeException("Not implemented yet");
    }
}
