/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.matching.inst;

import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.instantiation.SVInstantiations;

public class MatchConditions extends MatchResultInfo {

    public static final MatchConditions EMPTY_MATCHCONDITIONS =
        new MatchConditions(
            org.key_project.solidity.rule.matching.inst.SVInstantiations.EMPTY_SVINSTANTIATIONS);

    public MatchConditions(SVInstantiations pInstantiations) {
        super(pInstantiations);
    }

    @Override
    public org.key_project.solidity.rule.matching.inst.SVInstantiations getInstantiations() {
        return (org.key_project.solidity.rule.matching.inst.SVInstantiations) super.getInstantiations();
    }

    @Override
    public MatchConditions setInstantiations(
            org.key_project.prover.rules.instantiation.SVInstantiations p_instantiations) {
        if (instantiations == p_instantiations) {
            return this;
        } else {
            return new MatchConditions(p_instantiations);
        }
    }
}
