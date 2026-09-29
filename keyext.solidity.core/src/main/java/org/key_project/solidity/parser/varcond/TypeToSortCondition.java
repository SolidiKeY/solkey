/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MemoryReferenceTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.rule.matching.inst.GenericSortCondition;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.matching.inst.SortException;

import org.jspecify.annotations.Nullable;

/// Binds a generic sort to a sort derived from the instantiation of one schema variable.
public abstract class TypeToSortCondition implements VariableCondition {
    protected final SchemaVariable sv;
    protected final GenericSort sort;

    protected TypeToSortCondition(SchemaVariable sv, GenericSort sort) {
        this.sv = sv;
        this.sort = sort;
    }

    protected abstract @Nullable Sort sortOf(SyntaxElement svSubst, Services services);

    protected abstract String keyword();

    @Override
    public final @Nullable MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != sv) {
            return matchCond;
        }
        Sort type = sortOf(svSubst, (Services) lServices);
        if (type == null) {
            return null;
        }
        SVInstantiations inst = (SVInstantiations) matchCond.getInstantiations();
        try {
            return matchCond.setInstantiations(
                inst.add(GenericSortCondition.createIdentityCondition(sort, type), lServices));
        } catch (SortException e) {
            return null;
        }
    }

    protected static @Nullable Sort payloadSort(Services services, @Nullable Type type,
            boolean memoryPayload) {
        Type unwrapped = StaticTypes.unwrap(type);
        if (unwrapped == null) {
            return null;
        }
        KeYSolidityType keyType = services.getSolidityInfo().getKeYSolidityType(unwrapped);
        if (keyType == null) {
            return null;
        }
        return memoryPayload && MemoryReferenceTypes.isReferenceType(unwrapped)
                ? services.getTheoryInfo().getMemoryLDT().getIdentitySort()
                : keyType.getSort();
    }

    @Override
    public String toString() {
        return keyword() + "(" + sv.name() + ", " + sort.name() + ")";
    }
}
