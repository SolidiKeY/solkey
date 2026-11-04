/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

import org.jspecify.annotations.Nullable;

public final class FixedLengthReadCondition implements VariableCondition {
    private final SchemaVariable receiver;
    private final SchemaVariable field;
    private final SchemaVariable length;
    private final boolean negated;

    public FixedLengthReadCondition(SchemaVariable receiver, SchemaVariable field,
            SchemaVariable length, boolean negated) {
        this.receiver = receiver;
        this.field = field;
        this.length = length;
        this.negated = negated;
    }

    @Override
    public @Nullable MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != receiver && var != field) {
            return matchCond;
        }
        SVInstantiations inst = (SVInstantiations) matchCond.getInstantiations();
        Object receiverInst = var == receiver ? instCandidate : inst.getInstantiation(receiver);
        Object fieldInst = var == field ? instCandidate : inst.getInstantiation(field);
        if (receiverInst == null || fieldInst == null) {
            return matchCond;
        }
        ArrayType fixed = fixedLength(receiverInst, fieldInst);
        if (negated) {
            return fixed == null ? matchCond : null;
        }
        if (fixed == null) {
            return null;
        }
        if (inst.getInstantiation(length) != null) {
            return matchCond;
        }
        Services services = (Services) lServices;
        return matchCond.setInstantiations(
            inst.add(length, services.getTermBuilder().zTerm(fixed.length()), services));
    }

    private static @Nullable ArrayType fixedLength(Object receiver, Object field) {
        if (!(field instanceof FieldDeclaration declaration)
                || !"size".equals(declaration.name().toString())
                || !(receiver instanceof Expression expression)) {
            return null;
        }
        return StaticTypes.unwrap(expression.getType()) instanceof ArrayType fixed ? fixed : null;
    }

    @Override
    public String toString() {
        return (negated ? "\\not" : "") + "\\fixedLengthRead(" + receiver.name() + ", "
            + field.name() + ", " + length.name() + ")";
    }
}
