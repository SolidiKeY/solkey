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
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.literals.Literal;

public class SimpleExpressionSVSort extends ProgramSVSort {

    /// Mirrors [ProgramVariableSVSort.Filter]: `ANY` admits every simple expression,
    /// `NON_STORAGE_LOCAL` (`SimpleExpression[name=value]`) excludes storage-qualified locals and
    /// memory reference aliases. Those are path/identity references, not ordinary stack values.
    public enum Filter {
        ANY, STORAGE_LOCAL, NON_STORAGE_LOCAL
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
        // Literals (BoolLiteral, Uint256Literal, etc.) — never storage paths.
        if (pe instanceof Literal) {
            return filter != Filter.STORAGE_LOCAL;
        }

        if (pe instanceof ProgramVariable pv) {
            return switch (filter) {
                case ANY -> true;
                case STORAGE_LOCAL -> pv.getDataLocation() == DataLocation.Storage;
                case NON_STORAGE_LOCAL -> pv.getDataLocation() != DataLocation.Storage
                        && pv.getDataLocation() != DataLocation.Memory;
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
        Filter f = switch (parameter.toLowerCase(Locale.ROOT)) {
            case "storage", "storage.local" -> Filter.STORAGE_LOCAL;
            case "non-storage", "value" -> Filter.NON_STORAGE_LOCAL;
            default -> throw new IllegalArgumentException(
                "Unknown SimpleExpression sort flag '" + parameter
                    + "' (expected 'storage' or 'value')");
        };
        ProgramSVSort result = new SimpleExpressionSVSort(
            new Name("SimpleExpression[name=" + parameter + "]"), f);
        PARAMETERIZED_SORTS.put(parameter, result);
        return result;
    }
}
