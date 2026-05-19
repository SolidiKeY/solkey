/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule;

import org.key_project.prover.rules.Taclet;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.rule.matching.inst.MatchConditions;
import org.key_project.solidity.rule.taclets.SolRewriteTaclet;

public class UninstantiatedNoPosTacletApp extends NoPosTacletApp {
    UninstantiatedNoPosTacletApp(SolTaclet taclet) {
        super(taclet);
    }

    @Override
    protected MatchConditions setupMatchConditions(PosInOccurrence pos, Services services) {
        if (taclet() instanceof SolRewriteTaclet rwt) {
            return rwt.checkPrefix(pos,
                MatchConditions.EMPTY_MATCHCONDITIONS);
        }

        return MatchConditions.EMPTY_MATCHCONDITIONS;
    }
}
