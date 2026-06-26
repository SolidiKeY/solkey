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
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.MemoryReferenceTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.rule.matching.inst.GenericSortCondition;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.matching.inst.SortException;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.jspecify.annotations.Nullable;

/// Binds a generic sort to the element/value type of an indexed storage receiver.
public final class IndexedExpressionTypeToSortCondition implements VariableCondition {
    private final ProgramSV receiverSV;
    private final GenericSort sort;
    private final boolean memoryPayload;

    public IndexedExpressionTypeToSortCondition(ProgramSV receiverSV, GenericSort sort) {
        this(receiverSV, sort, false);
    }

    public IndexedExpressionTypeToSortCondition(ProgramSV receiverSV, GenericSort sort,
            boolean memoryPayload) {
        this.receiverSV = receiverSV;
        this.sort = sort;
        this.memoryPayload = memoryPayload;
    }

    @Override
    @Nullable
    public MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != receiverSV) {
            return matchCond;
        }
        if (!(svSubst instanceof Expression receiver)) {
            return null;
        }

        Type elementType = switch (receiver.getType()) {
            case MappingType mappingType -> mappingType.valueType();
            case ArrayType arrayType -> arrayType.getElementType();
            case DynamicArrayType dynamicArrayType -> dynamicArrayType.getElementType();
            default -> null;
        };
        if (elementType == null) {
            return null;
        }

        Services services = (Services) lServices;
        KeYSolidityType keyType = services.getSolidityInfo().getKeYSolidityType(elementType);
        if (keyType == null) {
            return null;
        }

        Sort type = memoryPayload && MemoryReferenceTypes.isReferenceType(elementType)
                ? services.getTheoryInfo().getMemoryLDT().getIdentitySort()
                : keyType.getSort();
        SVInstantiations inst = (SVInstantiations) matchCond.getInstantiations();
        try {
            return matchCond.setInstantiations(
                inst.add(GenericSortCondition.createIdentityCondition(sort, type), lServices));
        } catch (SortException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        String condition = memoryPayload ? "\\hasMemoryElementSort" : "\\hasElementSort";
        return condition + "(" + receiverSV.name() + ", " + sort.name() + ")";
    }
}
