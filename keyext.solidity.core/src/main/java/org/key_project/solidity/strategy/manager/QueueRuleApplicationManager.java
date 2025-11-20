/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.strategy.manager;

import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.RuleApplicationManager;
import org.key_project.solidity.proof.Goal;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;

public class QueueRuleApplicationManager implements RuleApplicationManager<Goal> {
    @Override
    public void clearCache() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public RuleApp peekNext() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public RuleApp next() {
        throw new RuntimeException("Not implemented");

    }

    @Override
    public RuleApplicationManager<@NonNull Goal> copy() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setGoal(@NonNull Goal p_goal) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void ruleAdded(RuleApp rule, PosInOccurrence pos) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void rulesAdded(ImmutableList<? extends RuleApp> rule, PosInOccurrence pos) {
        throw new RuntimeException("Not implemented");
    }
}
