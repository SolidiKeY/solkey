/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.strategy.feature;

import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PIOPathIterator;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.feature.BinaryFeature;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.solidity.logic.op.IfThenElse;

import org.jspecify.annotations.NonNull;

/// Returns zero iff the position of a rule application is not inside the then- or else-branch
/// of a conditional term or formula; the condition itself is allowed.
public class NotInConditionalBranchFeature extends BinaryFeature {
    public static final Feature INSTANCE = new NotInConditionalBranchFeature();

    private NotInConditionalBranchFeature() {}

    @Override
    protected <Goal extends ProofGoal<@NonNull Goal>> boolean filter(RuleApp app,
            PosInOccurrence pos, Goal goal, MutableState mState) {
        assert pos != null : "Feature is only applicable to rules with find";
        final PIOPathIterator it = pos.iterator();
        while (it.next() != -1) {
            if (it.getSubTerm().op() instanceof IfThenElse && it.getChild() != 0) {
                return false;
            }
        }
        return true;
    }
}
