/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast;

import org.key_project.logic.Name;
import org.key_project.solidity.logic.ast.abstractions.PrimitiveType;
import org.key_project.solidity.logic.ast.abstractions.Type;
import org.key_project.solidity.logic.ast.expressions.operators.*;

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
            case AddOperator addOperator -> uint256;
            case AndOperator andOperator -> bool;
            case DivOperator divOperator -> uint256;
            case EqualOperator equalOperator -> bool;
            case ExponentialOperator exponentialOperator -> uint256;
            case GreaterEqualOperator greaterEqualOperator -> uint256;
            case GreaterOperator greaterOperator -> uint256;
            case LessEqualOperator lessEqualOperator -> uint256;
            case LessOperator lessOperator -> uint256;
            case ModOperator modOperator -> uint256;
            case MultiplicationOperator multiplicationOperator -> uint256;
            case OrOperator orOperator -> bool;
            case SubtractionOperator subtractionOperator -> uint256;
            case UnequalOperator unequalOperator -> bool;
        };
    }
}
