/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

import org.jspecify.annotations.Nullable;

/// Variable condition `\pathField(deepPath, fieldName)` extracting the field identifier from
/// a [MemberExp].
///
/// For a [MemberExp] like `alice.account.balance`, the field is the identifier `balance`.
///
/// This condition instantiates the `fieldName` schema variable with the right (field) expression
/// of the member expression.
public final class PathFieldCondition implements VariableCondition {
    private final SchemaVariable deepPathSV;
    private final SchemaVariable fieldSV;

    public PathFieldCondition(SchemaVariable deepPathSV, SchemaVariable fieldSV) {
        this.deepPathSV = deepPathSV;
        this.fieldSV = fieldSV;
    }

    @Override
    @Nullable
    public MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != deepPathSV && var != fieldSV) {
            return matchCond;
        }
        final SVInstantiations inst = (SVInstantiations) matchCond.getInstantiations();

        // Resolve the deep path
        final Object deepInst = var == deepPathSV ? svSubst : inst.getInstantiation(deepPathSV);
        if (deepInst == null) {
            // deepPath not yet matched; defer
            return matchCond;
        }
        if (!(deepInst instanceof SolidityProgramElement pe)) {
            return null;
        }

        // Extract the field from the deep path (only MemberExp has a field)
        if (!(pe instanceof MemberExp member)) {
            return null;
        }
        final SyntaxElement fieldElement = member.getRightExp();
        if (fieldElement == null) {
            return null;
        }
        // The field must be a SolidityProgramElement (typically FieldDeclaration)
        if (!(fieldElement instanceof SolidityProgramElement field)) {
            return null;
        }

        // Check or bind the field schema variable
        final Object fieldInst = var == fieldSV ? svSubst : inst.getInstantiation(fieldSV);
        if (fieldInst == null) {
            // fieldSV is still free: bind it
            return matchCond.setInstantiations(inst.add(fieldSV, field, lServices));
        }
        // Check that existing instantiation matches
        return field.equals(fieldInst) ? matchCond : null;
    }

    @Override
    public String toString() {
        return "\\pathField(" + deepPathSV + ", " + fieldSV + ")";
    }
}
