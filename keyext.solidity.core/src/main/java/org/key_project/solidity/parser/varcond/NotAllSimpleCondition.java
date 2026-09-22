/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.rule.VariableConditionAdapter;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;

import org.jspecify.annotations.Nullable;

/// Holds unless both schema variables are instantiated with simple program elements.
///
/// A capture taclet that hoists every constituent of `recv[idx] = rhs` at once matches a receiver
/// and an index of any simplicity, so without a guard it would also match the fully simple
/// `sp[se] = rhs` and re-fire on its own output forever. This condition is that guard: it admits
/// the match exactly when there is still something left to capture.
public class NotAllSimpleCondition extends VariableConditionAdapter {
    private final SchemaVariable receiver;
    private final SchemaVariable index;

    public NotAllSimpleCondition(SchemaVariable receiver, SchemaVariable index) {
        this.receiver = receiver;
        this.index = index;
    }

    @Override
    public boolean check(SchemaVariable var, SyntaxElement instCandidate, SVInstantiations instMap,
            Services services) {
        SyntaxElement receiverInst = instantiationOf(receiver, var, instCandidate, instMap);
        SyntaxElement indexInst = instantiationOf(index, var, instCandidate, instMap);
        if (receiverInst == null || indexInst == null) {
            return true;
        }
        return !(isSimplePath(receiverInst, services) && isSimpleExpression(indexInst, services));
    }

    private static @Nullable SyntaxElement instantiationOf(SchemaVariable wanted,
            SchemaVariable var, SyntaxElement instCandidate, SVInstantiations instMap) {
        return var == wanted ? instCandidate : instMap.getInstantiation(wanted);
    }

    private static boolean isSimplePath(SyntaxElement element, Services services) {
        return element instanceof SolidityProgramElement pe
                && (ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(pe, services)
                        || ProgramSVSort.SIMPLE_MEMORY_PATH.canStandFor(pe, services));
    }

    private static boolean isSimpleExpression(SyntaxElement element, Services services) {
        return element instanceof SolidityProgramElement pe
                && ProgramSVSort.SIMPLE_EXPRESSION.canStandFor(pe, services);
    }

    @Override
    public String toString() {
        return "\\notAllSimple(" + receiver + ", " + index + ")";
    }
}
