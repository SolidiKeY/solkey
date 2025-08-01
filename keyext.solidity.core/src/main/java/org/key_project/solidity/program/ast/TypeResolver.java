/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.operators.*;

/** This class resolves the type of a reference or expression */
public class TypeResolver {

    private final SolidityModel solidityModel;

    public TypeResolver(SolidityModel solidityModel) {
        this.solidityModel = solidityModel;
    }

    public Type resolveTypeByName(Name typeName) {
        return solidityModel.getType(typeName);
    }

    public Type resolve(BinaryOperator binOp) {
        Type uint256 = PrimitiveType.getPrimitiveType("uint256");
        Type bool = PrimitiveType.getPrimitiveType("bool");
        return switch (binOp) {
            case AddOperator ignored -> uint256;
            case AndOperator ignored -> bool;
            case DivOperator ignored -> uint256;
            case EqualOperator ignored -> bool;
            case ExponentialOperator ignored -> uint256;
            case GreaterEqualOperator ignored -> uint256;
            case GreaterOperator ignored -> uint256;
            case LessEqualOperator ignored -> uint256;
            case LessOperator ignored -> uint256;
            case ModOperator ignored -> uint256;
            case MultiplicationOperator ignored -> uint256;
            case OrOperator ignored -> bool;
            case SubtractionOperator ignored -> uint256;
            case UnequalOperator ignored -> bool;
            default -> throw new IllegalStateException("Unexpected value: " + binOp);
        };
    }
}
