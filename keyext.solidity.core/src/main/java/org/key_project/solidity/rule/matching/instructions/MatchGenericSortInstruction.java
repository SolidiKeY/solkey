/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.matching.instructions;

import org.jspecify.annotations.Nullable;
import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.solidity.logic.GenericArgument;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.rule.matching.inst.GenericSortCondition;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.matching.inst.SortException;

public class MatchGenericSortInstruction implements MatchInstruction {
    private final GenericSort genericSortOfOp;

    public MatchGenericSortInstruction(GenericSort sort) {
        this.genericSortOfOp = sort;
    }

    /// Matches the generic sort of this instruction's parameter against the given
    /// sort. If a match is possible the resulting match conditions are returned otherwise
    /// `null` is returned.
    ///
    /// @param dependingSortToMatch the [Sort] of the concrete function to be matched
    /// @param matchConditions the [MatchResultInfo] accumulated so far
    /// @return <code>null</code> if failed the resulting match conditions otherwise the resulting
    /// [MatchResultInfo]
    private MatchResultInfo matchSorts(Sort dependingSortToMatch, MatchResultInfo matchConditions,
            LogicServices services) {
        // This restriction has been dropped for free generic sorts to prove taclets correct
        // assert !(s2 instanceof GenericSort)
        // : "Sort s2 is not allowed to be of type generic.";
        MatchResultInfo result;
        final GenericSortCondition c =
            GenericSortCondition.createIdentityCondition(genericSortOfOp, dependingSortToMatch);
        try {
            final SVInstantiations instantiations =
                (SVInstantiations) matchConditions.getInstantiations();
            return matchConditions.setInstantiations(instantiations.add(c, services));
        } catch (SortException e) {
            return null;
        }
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo mc,
            LogicServices services) {
        if (actualElement instanceof GenericArgument(Sort sort)) {
            return matchSorts(sort, mc, services);
        }
        return null;
    }
}
