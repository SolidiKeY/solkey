/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityInfo;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.expressions.literals.Literal;
import org.key_project.solidity.program.ast.references.ContractReference;
import org.key_project.solidity.program.ast.references.FieldReference;

final class ExternalCallSVSort extends ProgramSVSort {

    ExternalCallSVSort() {
        super(new Name("ExternalCall"));
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return pe instanceof FunctionCallExpression call
                && call.getFunctionExp() instanceof MemberExp member
                && member.getRightExp() instanceof FunctionDeclaration function
                && SolidityInfo.getBuiltinFunctionDeclaration(function.name()) != function
                && isInert(member.getLeftExp())
                && call.getArguments().stream().allMatch(ExternalCallSVSort::isInert);
    }

    private static boolean isInert(Expression e) {
        return switch (e) {
            case Literal ignored -> true;
            case ProgramVariable ignored -> true;
            case FieldReference ignored -> true;
            case MemberExp member -> !(member.getRightExp() instanceof FunctionDeclaration)
                    && isInert(member.getLeftExp());
            case FunctionCallExpression conversion -> conversion
                    .getFunctionExp() instanceof ContractReference
                    && conversion.getArguments().stream().allMatch(ExternalCallSVSort::isInert);
            default -> false;
        };
    }
}
