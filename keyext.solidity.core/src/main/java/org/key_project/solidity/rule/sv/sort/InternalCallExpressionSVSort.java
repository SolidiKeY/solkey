/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.rule.metaconstruct.ExpandFunctionBody;

final class InternalCallExpressionSVSort extends ProgramSVSort {
    InternalCallExpressionSVSort() {
        super(new Name("InternalCallExpression"));
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        if (!(pe instanceof FunctionCallExpression call)
                || !(call.getFunctionExp() instanceof FunctionReference reference)) {
            return false;
        }
        FunctionDeclaration function = reference.getReferencedDeclaration();
        return function != null && ExpandFunctionBody.inlinable(function)
                && function.getReturnParameters().size() == 1
                && function.getReturnParameters().get(0)
                        .getDataLocation() == DataLocation.Storage;
    }
}
