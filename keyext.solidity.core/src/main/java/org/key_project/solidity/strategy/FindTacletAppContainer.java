/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.strategy;

import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.rule.NoPosTacletApp;

import org.jspecify.annotations.Nullable;

public class FindTacletAppContainer extends TacletAppContainer {
    public FindTacletAppContainer(NoPosTacletApp app, @Nullable PosInOccurrence pos,
            RuleAppCost cost, Goal goal, long localAge) {
        super(app, cost, goal.getTime());
    }

    @Override
    protected boolean isStillApplicable(Goal p_goal) {
        throw new RuntimeException("Not implemented yet");
    }
}
