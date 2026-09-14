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
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;

public class ExpressionSVSort extends ProgramSVSort {

    /// `ANY` admits every expression. `PRIMITIVE` (`Expression[primitive]`) admits the
    /// expressions a write rule may capture into a plain value temporary in one step: those of
    /// primitive static type that are not *complex* paths. Literals, primitive variables,
    /// operator expressions and bare contract roots qualify; `p.age` and `a[i]` do not, because
    /// a complex path must have its own receiver resolved by the read unfolds first, and
    /// capturing it wholesale would race with them.
    public enum Filter {
        ANY, PRIMITIVE
    }

    private static final Map<String, ProgramSVSort> PARAMETERIZED_SORTS = new HashMap<>();

    private final Filter filter;

    public ExpressionSVSort() {
        this(new Name("Expression"), Filter.ANY);
    }

    private ExpressionSVSort(Name name, Filter filter) {
        super(name);
        this.filter = filter;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        if (!(pe instanceof Expression expression)) {
            return false;
        }
        if (filter == Filter.ANY) {
            return true;
        }
        return !isComplexPath(pe)
                && StaticTypes.unwrap(expression.getType()) instanceof PrimitiveType;
    }

    private static boolean isComplexPath(SolidityProgramElement pe) {
        return pe instanceof MemberExp || pe instanceof IndexExpression
                || (pe instanceof FunctionCallExpression call && PathSVSort.isNoArgPush(call));
    }

    @Override
    public ProgramSVSort createInstance(String parameter) {
        ProgramSVSort cached = PARAMETERIZED_SORTS.get(parameter);
        if (cached != null) {
            return cached;
        }
        if (!"primitive".equals(parameter.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                "Unknown Expression sort flag '" + parameter + "' (expected 'primitive')");
        }
        ProgramSVSort result =
            new ExpressionSVSort(new Name("Expression[" + parameter + "]"), Filter.PRIMITIVE);
        PARAMETERIZED_SORTS.put(parameter, result);
        return result;
    }
}
