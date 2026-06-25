/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.OperatorSV;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.rule.matching.inst.GenericSortCondition;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.matching.inst.SortException;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;

import org.jspecify.annotations.Nullable;

/// Variable condition that enforces a given generic sort to be instantiated with the sort of a
/// program expression a schema variable is instantiated with
public final class SolidityTypeToSortCondition implements VariableCondition {
    private final OperatorSV exprOrTypeSV;
    private final GenericSort sort;

    public SolidityTypeToSortCondition(OperatorSV exprOrTypeSV, GenericSort sort) {
        this.exprOrTypeSV = exprOrTypeSV;
        this.sort = sort;

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
    @Nullable
    public MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != exprOrTypeSV) {
            return matchCond;
        }

        final var inst = (SVInstantiations) matchCond.getInstantiations();
        Services services = (Services) lServices;
        Sort type;

        if (svSubst instanceof Term t) {
            type = t.sort();
        } else if (svSubst instanceof Type st) {
            type = services.getSolidityInfo().getKeYSolidityType(st).getSort();
        } else if (svSubst instanceof Expression expr) {
            type = services.getSolidityInfo().getKeYSolidityType(expr.getType())
                    .getSort();
        } else {
            return null;
        }
        try {
            return matchCond.setInstantiations(
                inst.add(GenericSortCondition.createIdentityCondition(sort, type), lServices));
        } catch (SortException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "\\hasSort(" + exprOrTypeSV.name() + ", " + sort.name() + ")";
    }
}
