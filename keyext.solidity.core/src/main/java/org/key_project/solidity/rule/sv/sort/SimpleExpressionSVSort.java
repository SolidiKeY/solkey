/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.expressions.literals.Literal;

public class SimpleExpressionSVSort extends ProgramSVSort {

    /// `ANY` admits every simple expression. `PRIMITIVE` (`SimpleExpression[primitive]`)
    /// restricts matching to expressions of primitive static type: literals and primitive-typed
    /// variables. Storage aliases and memory references carry struct/array/mapping types and are
    /// therefore excluded. `REFERENCE` (`SimpleExpression[reference]`) is its exact complement:
    /// the non-primitive variables, and no literal.
    public enum Filter {
        ANY, PRIMITIVE, REFERENCE
    }

    private static final Map<String, ProgramSVSort> PARAMETERIZED_SORTS = new HashMap<>();

    private final Filter filter;

    public SimpleExpressionSVSort() {
        this(new Name("SimpleExpression"), Filter.ANY);
    }

    private SimpleExpressionSVSort(Name name, Filter filter) {
        super(name);
        this.filter = filter;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        if (pe instanceof Literal) {
            return filter != Filter.REFERENCE;
        }

        if (pe instanceof ProgramVariable pv) {
            return switch (filter) {
                case ANY -> true;
                case PRIMITIVE -> pv.getType() instanceof PrimitiveType;
                case REFERENCE -> !(pv.getType() instanceof PrimitiveType);
            };
        }

        return false;
    }

    @Override
    public ProgramSVSort createInstance(String parameter) {
        ProgramSVSort cached = PARAMETERIZED_SORTS.get(parameter);
        if (cached != null) {
            return cached;
        }
        Filter parsed = switch (parameter.toLowerCase(Locale.ROOT)) {
            case "primitive" -> Filter.PRIMITIVE;
            case "reference" -> Filter.REFERENCE;
            default -> throw new IllegalArgumentException(
                "Unknown SimpleExpression sort flag '" + parameter
                    + "' (expected 'primitive' or 'reference')");
        };
        ProgramSVSort result = new SimpleExpressionSVSort(
            new Name("SimpleExpression[" + parameter + "]"), parsed);
        PARAMETERIZED_SORTS.put(parameter, result);
        return result;
    }
}
