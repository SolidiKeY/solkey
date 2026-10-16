/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.statement.*;

final class NormalizableLoopSVSort extends ProgramSVSort {
    NormalizableLoopSVSort() {
        super(new Name("NormalizableLoop"));
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return pe instanceof LoopStatement loop
                && (!(loop instanceof WhileStatement) || containsJump(loop.getBody()));
    }

    private static boolean containsJump(SyntaxElement element) {
        if (element instanceof FunctionFrame) {
            return false;
        }
        if (element instanceof BreakStatement || element instanceof ContinueStatement
                || element instanceof ReturnStatement) {
            return true;
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            if (containsJump(element.getChild(i))) {
                return true;
            }
        }
        return false;
    }
}
