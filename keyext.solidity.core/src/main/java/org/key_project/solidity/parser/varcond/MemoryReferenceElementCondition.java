/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.jspecify.annotations.Nullable;

import static org.key_project.solidity.program.ast.abstractions.MemoryReferenceTypes.isReferenceType;

/// Checks whether an indexed receiver stores reference payloads in memory.
public final class MemoryReferenceElementCondition implements VariableCondition {
    private final ProgramSV receiverSV;
    private final boolean negated;

    public MemoryReferenceElementCondition(ProgramSV receiverSV, boolean negated) {
        this.receiverSV = receiverSV;
        this.negated = negated;
    }

    @Override
    @Nullable
    public MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices services) {
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
        boolean reference =
            elementType != null && isReferenceType(elementType);
        return reference != negated ? matchCond : null;
    }

    @Override
    public String toString() {
        return (negated ? "\\not " : "") + "\\isMemoryReferenceElement("
            + receiverSV.name() + ")";
    }
}
