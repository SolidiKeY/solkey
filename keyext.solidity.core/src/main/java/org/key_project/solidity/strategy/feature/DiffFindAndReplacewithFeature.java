/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.strategy.feature;

import org.key_project.logic.Term;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.rule.TacletApp;
import org.key_project.solidity.rule.taclets.RewriteTacletGoalTemplate;
import org.key_project.solidity.rule.taclets.SolRewriteTaclet;

/// Binary feature that returns zero iff the replacewith- and find-parts of a Taclet are matched to
/// different terms.
public class DiffFindAndReplacewithFeature extends BinaryTacletAppFeature {
    /// the single instance of this feature
    public static final Feature INSTANCE = new DiffFindAndReplacewithFeature();

    private DiffFindAndReplacewithFeature() {}

    @Override
    protected boolean filter(TacletApp app, PosInOccurrence pos, Goal goal, MutableState mState) {
        assert pos != null && app.rule() instanceof SolRewriteTaclet
                : "Feature is only applicable to rewrite taclets";

        var rwt = (SolRewriteTaclet) app.rule();
        for (var template : rwt.goalTemplates()) {
            final Term replaceWith = ((RewriteTacletGoalTemplate) template).replaceWith();
            if (replaceWith.equals(pos.subTerm())) {
                return false;
            }
        }
        return true;
    }
}
