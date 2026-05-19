/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.matching.instructions;

import org.jspecify.annotations.Nullable;
import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.logic.op.BoundVariable;
import org.key_project.solidity.logic.op.LogicVariable;
import org.key_project.solidity.rule.matching.inst.MatchConditions;
import org.key_project.solidity.rule.sv.VariableSV;


public class MatchVariableSVInstruction
        extends MatchSchemaVariableInstruction {
    protected MatchVariableSVInstruction(VariableSV op) {
        super(op);
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo mCond,
            LogicServices services) {
        var mc = (MatchConditions) mCond;
        final Term actualTerm = (Term) actualElement;
        if (actualTerm.op() instanceof LogicVariable lv) {
            final Term bvTerm = (Term) mc.getInstantiations().getInstantiation(op);
            if (bvTerm == null || !(bvTerm.op() instanceof BoundVariable)) {
                return null;
            }
            var tableBV = mc.logicVariableTable().getBoundVariable(lv.getIndex());
            if (tableBV == null || !tableBV.sort().equals(lv.sort())) {
                return null;
            }
            return mc;
        }
        return null;
    }
}
