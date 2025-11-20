/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.strategy;

import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.util.collection.ImmutableList;

public abstract class RuleAppContainer {
    public RuleAppContainer(RuleApp pApp, RuleAppCost pCost) {
    }

    public RuleApp getRuleApp() {
        return null;
    }


    protected RuleAppCost getCost() {
        return null;
    }

    public abstract RuleApp completeRuleApp(ProofGoal<?> p_goal);

    public abstract ImmutableList<RuleAppContainer> createFurtherApps(ProofGoal<?> p_goal);

}
