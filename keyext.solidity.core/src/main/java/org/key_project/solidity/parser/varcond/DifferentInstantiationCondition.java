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
            return inst2 == null || differentInstantiations(inst2, instCandidate);
        } else if (var == var2) {
            final SyntaxElement inst1 = svInst.getInstantiation(var1);
            return inst1 == null || differentInstantiations(inst1, instCandidate);
        } else {
            return true;
        }
    }

    private boolean differentInstantiations(SyntaxElement inst1, SyntaxElement inst2) {
        if (inst1 == inst2) {
            return false;
        }

        ProgramVariable var1 = null;
        ProgramVariable var2 = null;

        if (inst1 instanceof ProgramVariable) {
            var1 = (ProgramVariable) inst1;
        }
        if (inst2 instanceof ProgramVariable) {
            var2 = (ProgramVariable) inst2;
        }
        boolean hasConstant = false;
        if (var1 == null && inst1 instanceof Term t) {
            if (t.op() instanceof ProgramVariable pv) {
                var1 = pv;
            } else if (t.arity() != 0) {
                return false;
            }
        }
        if (var2 == null && inst2 instanceof Term t) {
            if (t.op() instanceof ProgramVariable pv) {
                var2 = pv;
            } else if (t.arity() != 0) {
                return false;
            }
        }

        if (var1 instanceof ProgramVariable && var2 instanceof ProgramVariable) {
            return var1 != var2;
        } else if (var1 == null && var2 == null) {
            return false;
        } else if (inst1 instanceof Term && inst2 instanceof Term) {
            return false;
        } else if (inst1 instanceof Term || inst2 instanceof Term) {
            return true; // one of them is a constant
        } else {
            return false; // safe out
        }
    }

    @Override
    public String toString() {
        return "\\different(" + var1.name() + ", " + var2.name() + ")";
    }
}
