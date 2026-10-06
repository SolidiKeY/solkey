/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.statement.FunctionBodyStatement;
import org.key_project.solidity.rule.metaconstruct.ExpandFunctionBody;

final class InternalCallSVSort extends ProgramSVSort {

    InternalCallSVSort() {
        super(new Name("InternalCall"));
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return !(pe instanceof FunctionBodyStatement)
                && ExpandFunctionBody.asFunctionBody(pe) != null;
    }
}
