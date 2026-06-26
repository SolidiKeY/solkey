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
import org.key_project.solidity.program.ast.SolidityInfo;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MemoryReferenceTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.rule.matching.inst.GenericSortCondition;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.matching.inst.SortException;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.jspecify.annotations.Nullable;

/// Binds a generic sort to the Solidity type of a matched member field.
public final class FieldExpressionTypeToSortCondition implements VariableCondition {
    private final ProgramSV fieldSV;
    private final GenericSort sort;
    private final boolean memoryPayload;

    public FieldExpressionTypeToSortCondition(ProgramSV fieldSV, GenericSort sort) {
        this(fieldSV, sort, false);
    }

    public FieldExpressionTypeToSortCondition(ProgramSV fieldSV, GenericSort sort,
            boolean memoryPayload) {
        this.fieldSV = fieldSV;
        this.sort = sort;
        this.memoryPayload = memoryPayload;
    }

    @Override
    @Nullable
    public MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != fieldSV) {
            return matchCond;
        }

        SVInstantiations inst = (SVInstantiations) matchCond.getInstantiations();
        if (!(svSubst instanceof FieldDeclaration fd)) {
            return null;
        }

        Services services = (Services) lServices;
        Type fieldType = fd.getTypeReference().referencedType;
        if (fieldType == null && fd.getTypeReference().typeName != null) {
            fieldType = SolidityInfo.getPrimitiveType(fd.getTypeReference().typeName.toString());
        }
        KeYSolidityType keyType = fieldType == null ? null
                : services.getSolidityInfo().getKeYSolidityType(fieldType);
        if (keyType == null) {
            return null;
        }

        Sort type = memoryPayload && MemoryReferenceTypes.isReferenceType(fieldType)
                ? services.getTheoryInfo().getMemoryLDT().getIdentitySort()
                : keyType.getSort();
        try {
            return matchCond.setInstantiations(
                inst.add(GenericSortCondition.createIdentityCondition(sort, type), lServices));
        } catch (SortException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        String condition = memoryPayload ? "\\hasMemoryFieldSort" : "\\hasFieldSort";
        return condition + "(" + fieldSV.name() + ", " + sort.name() + ")";
    }
}
