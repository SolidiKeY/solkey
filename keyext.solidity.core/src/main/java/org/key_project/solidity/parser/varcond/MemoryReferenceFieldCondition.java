/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.program.ast.SolidityInfo;
import org.key_project.solidity.program.ast.abstractions.MemoryReferenceTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.jspecify.annotations.Nullable;

/// Checks whether a matched field stores a reference payload in memory.
public final class MemoryReferenceFieldCondition implements VariableCondition {
    private final ProgramSV fieldSV;
    private final boolean negated;

    public MemoryReferenceFieldCondition(ProgramSV fieldSV, boolean negated) {
        this.fieldSV = fieldSV;
        this.negated = negated;
    }

    @Override
    @Nullable
    public MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices services) {
        if (var != fieldSV) {
            return matchCond;
        }
        if (!(svSubst instanceof FieldDeclaration fd)) {
            return null;
        }

        Type fieldType = fd.getTypeReference().referencedType;
        if (fieldType == null && fd.getTypeReference().typeName != null) {
            fieldType = SolidityInfo.getPrimitiveType(fd.getTypeReference().typeName.toString());
        }
        boolean reference = fieldType != null && MemoryReferenceTypes.isReferenceType(fieldType);
        return reference != negated ? matchCond : null;
    }

    @Override
    public String toString() {
        return (negated ? "\\not " : "") + "\\isMemoryReferenceField(" + fieldSV.name() + ")";
    }
}
