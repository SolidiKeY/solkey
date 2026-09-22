/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast;

import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;

import org.jspecify.annotations.Nullable;

/// Resolves the static Solidity type of a program element, unwrapping [KeYSolidityType]
/// and stepping through index expressions to the element/value type.
public final class StaticTypes {
    private StaticTypes() {}

    public static @Nullable Type typeOf(SolidityProgramElement pe) {
        if (!(pe instanceof Expression expression)) {
            return null;
        }
        if (pe instanceof IndexExpression index) {
            Type baseType = typeOf(index.getLeftExp());
            if (baseType instanceof DynamicArrayType arrayType) {
                return unwrap(arrayType.getElementType());
            }
            if (baseType instanceof ArrayType arrayType) {
                return unwrap(arrayType.getElementType());
            }
            if (baseType instanceof MappingType mappingType) {
                return unwrap(mappingType.valueType());
            }
        }
        return unwrap(expression.getType());
    }

    public static @Nullable Type unwrap(@Nullable Type type) {
        if (type instanceof KeYSolidityType keyType) {
            Type solidityType = keyType.getSolidityType();
            if (solidityType != null) {
                return solidityType;
            }
        }
        return type;
    }
}
