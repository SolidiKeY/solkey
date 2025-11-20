/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof;

import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentChangeInfo;
import org.key_project.prover.strategy.NewRuleListener;
import org.key_project.solidity.rule.IBuiltInRuleApp;
import org.key_project.util.collection.ImmutableList;

public class BuiltInRuleAppIndex {
    public BuiltInRuleAppIndex(BuiltInRuleIndex builtInRules) {
    }

    public void sequentChanged(Goal goal, SequentChangeInfo sci, NewRuleListener newRuleListener) {
        throw new RuntimeException("Not implemented");
    }

    public ImmutableList<IBuiltInRuleApp> getBuiltInRule(Goal g, PosInOccurrence pos) {
        throw new RuntimeException("Not implemented");
    }

    public void reportRuleApps(NewRuleListener l, Goal goal) {
        throw new RuntimeException("Not implemented");
    }

    public void setNewRuleListener(NewRuleListener newRuleListener) {
        throw new RuntimeException("Not implemented");
    }

    public BuiltInRuleAppIndex copy() {
        throw new RuntimeException("Not implemented");
    }

    public void scanApplicableRules(Goal goal, NewRuleListener newRuleListener) {
        throw new RuntimeException("Not implemented");
    }
}
