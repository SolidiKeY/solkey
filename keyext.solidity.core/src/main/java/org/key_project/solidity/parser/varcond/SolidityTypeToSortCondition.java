/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.OperatorSV;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;

import org.jspecify.annotations.Nullable;

/// Variable condition that enforces a given generic sort to be instantiated with the sort of a
/// program expression a schema variable is instantiated with
public final class SolidityTypeToSortCondition extends TypeToSortCondition {

    public SolidityTypeToSortCondition(OperatorSV exprOrTypeSV, GenericSort sort) {
        super(exprOrTypeSV, sort);
        if (!checkSortedSV(exprOrTypeSV)) {
            throw new RuntimeException("Expected a program schemavariable for expressions");
        }
    }

    public static boolean checkSortedSV(final OperatorSV exprOrTypeSV) {
        final Sort svSort = exprOrTypeSV.sort();
        return svSort == ProgramSVSort.EXPRESSION || svSort == ProgramSVSort.SIMPLE_EXPRESSION
                || svSort == ProgramSVSort.NON_SIMPLE_EXPRESSION || svSort == ProgramSVSort.TYPE
                || svSort instanceof ProgramSVSort || exprOrTypeSV.arity() == 0;
    }

    @Override
    protected @Nullable Sort sortOf(SyntaxElement svSubst, Services services) {
        return switch (svSubst) {
            case Term t -> t.sort();
            case Type st -> payloadSort(services, st, false);
            case Expression expr -> payloadSort(services, expr.getType(), false);
            default -> null;
        };
    }

    @Override
    protected String keyword() {
        return "\\hasSort";
    }
}
