/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.references.FieldReference;

public class NonSimpleExpressionSVSort extends ProgramSVSort {

    private enum Filter {
        ANY, PRIMITIVE
    }

    private static final Map<String, ProgramSVSort> PARAMETERIZED_SORTS = new HashMap<>();

    private final Filter filter;

    public NonSimpleExpressionSVSort() {
        this(new Name("NonSimpleExpression"), Filter.ANY);
    }

    private NonSimpleExpressionSVSort(Name name, Filter filter) {
        super(name);
        this.filter = filter;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        if (!(pe instanceof Expression expression))
            return false;
        if (ProgramSVSort.SIMPLE_EXPRESSION.canStandFor(pe, services))
            return false;
        if (filter == Filter.ANY)
            return true;
        if (isPathShaped(pe))
            return false;
        return unwrap(expression.getType()) instanceof PrimitiveType;
    }

    private static boolean isPathShaped(SolidityProgramElement pe) {
        return pe instanceof FieldReference || pe instanceof MemberExp
                || pe instanceof IndexExpression
                || (pe instanceof FunctionCallExpression call && PathSVSort.isNoArgPush(call));
    }

    private static Type unwrap(Type type) {
        if (type instanceof KeYSolidityType keyType && keyType.getSolidityType() != null) {
            return keyType.getSolidityType();
        }
        return type;
    }

    @Override
    public ProgramSVSort createInstance(String parameter) {
        ProgramSVSort cached = PARAMETERIZED_SORTS.get(parameter);
        if (cached != null) {
            return cached;
        }
        if (!"primitive".equals(parameter.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                "Unknown NonSimpleExpression sort flag '" + parameter
                    + "' (expected 'primitive')");
        }
        ProgramSVSort result = new NonSimpleExpressionSVSort(
            new Name("NonSimpleExpression[" + parameter + "]"), Filter.PRIMITIVE);
        PARAMETERIZED_SORTS.put(parameter, result);
        return result;
    }
}
