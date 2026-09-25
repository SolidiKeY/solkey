/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ParametricFunctionInstance;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.rule.VariableConditionAdapter;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.theory.StructLDT;

/// `\initialStorage(sv)` holds when `sv` is instantiated with the program variable `storage` or
/// a chain of `selectSt` reads from it: a node of the initial storage no rule has written.
public class InitialStorageCondition extends VariableConditionAdapter {
    private final SchemaVariable struct;
    private final boolean negated;

    public InitialStorageCondition(SchemaVariable struct, boolean negated) {
        this.struct = struct;
        this.negated = negated;
    }

    @Override
    public boolean check(SchemaVariable var, SyntaxElement instCandidate, SVInstantiations instMap,
            Services services) {
        if (var != struct) {
            return true;
        }
        boolean initial = instCandidate instanceof Term term && isSelectChainFromStorage(term);
        return initial != negated;
    }

    private static boolean isSelectChainFromStorage(Term term) {
        Term current = term;
        while (!(current.op() instanceof ProgramVariable pv
                && pv.name().equals(StructLDT.STORAGE_NAME))) {
            if (!(current.op() instanceof ParametricFunctionInstance select)
                    || !select.getBase().name().toString().equals("selectSt")
                    || current.arity() != 2) {
                return false;
            }
            current = current.sub(0);
        }
        return true;
    }

    @Override
    public String toString() {
        return (negated ? "\\not " : "") + "\\initialStorage(" + struct.name() + ")";
    }
}
