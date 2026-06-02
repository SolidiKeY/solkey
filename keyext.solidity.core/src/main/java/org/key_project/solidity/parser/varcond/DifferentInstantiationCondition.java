/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.rule.VariableConditionAdapter;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

public class DifferentInstantiationCondition extends VariableConditionAdapter {
    private final SchemaVariable var1, var2;

    public DifferentInstantiationCondition(SchemaVariable var1, SchemaVariable var2) {
        this.var1 = var1;
        this.var2 = var2;
    }

    @Override
    public boolean check(SchemaVariable var, SyntaxElement instCandidate, SVInstantiations svInst,
            Services services) {
        if (var == var1) {
            final SyntaxElement inst2 = svInst.getInstantiation(var2);
            return inst2 == null || !equalInst(inst2, instCandidate);
        } else if (var == var2) {
            final SyntaxElement inst1 = svInst.getInstantiation(var1);
            return inst1 == null || !equalInst(inst1, instCandidate);
        } else {
            return true;
        }
    }

    private boolean equalInst(SyntaxElement inst1, SyntaxElement inst2) {
        if (inst1 == inst2) {
            return true;
        }

        if (inst1.getClass() != inst2.getClass()) {
            if (inst1 instanceof ProgramVariable pv && inst2 instanceof Term t) {
                return pv == t.op();
            } else if (inst2 instanceof ProgramVariable pv && inst1 instanceof Term t) {
                return pv == t.op();
            }
            return false;
        } else {
            return inst1.equals(inst2);
        }

    }

    @Override
    public String toString() {
        return "\\different(" + var1 + ", " + var2 + ")";
    }
}
