/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import java.math.BigInteger;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.jspecify.annotations.Nullable;

/// Binds two `\term int` schema variables to the min/max values of the matched program
/// element's declared Solidity integer type, making the solc range check expressible in a
/// taclet guard. The rule is inapplicable when the resolved type is not a bounded integer
/// type (or, for [Mode#SELF_SIGNED], not a signed one).
public final class TypeBoundsCondition implements VariableCondition {

    public enum Mode {
        /// `\typeBounds(v, min, max)` — bounds of the element's own declared type
        SELF,
        /// `\signedTypeBounds(v, min, max)` — as SELF, but only signed integer types qualify
        SELF_SIGNED,
        /// `\fieldTypeBounds(f, min, max)` — bounds of a matched field's declared type
        FIELD,
        /// `\elementTypeBounds(r, min, max)` — bounds of an indexed receiver's element type
        ELEMENT
    }

    private final Mode mode;
    private final ProgramSV targetSV;
    private final SchemaVariable minSV;
    private final SchemaVariable maxSV;

    public TypeBoundsCondition(Mode mode, ProgramSV targetSV, SchemaVariable minSV,
            SchemaVariable maxSV) {
        this.mode = mode;
        this.targetSV = targetSV;
        this.minSV = minSV;
        this.maxSV = maxSV;
    }

    @Override
    @Nullable
    public MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != targetSV) {
            return matchCond;
        }
        PrimitiveType primitive = resolvePrimitiveType(svSubst);
        if (primitive == null) {
            return null;
        }
        BigInteger min = primitive.minValue();
        BigInteger max = primitive.maxValue();
        if (min == null || max == null) {
            return null;
        }
        if (mode == Mode.SELF_SIGNED && !primitive.isSignedInteger()) {
            return null;
        }
        Services services = (Services) lServices;
        MatchResultInfo result =
            bind(matchCond, minSV, services.getTermBuilder().zTerm(min.toString()), lServices);
        if (result == null) {
            return null;
        }
        return bind(result, maxSV, services.getTermBuilder().zTerm(max.toString()), lServices);
    }

    private @Nullable PrimitiveType resolvePrimitiveType(SyntaxElement svSubst) {
        Type type = switch (mode) {
            case SELF, SELF_SIGNED -> svSubst instanceof SolidityProgramElement pe
                    ? StaticTypes.typeOf(pe)
                    : null;
            case FIELD -> svSubst instanceof FieldDeclaration fd
                    ? StaticTypes.unwrap(fd.getTypeReference().resolvedType())
                    : null;
            case ELEMENT -> svSubst instanceof Expression receiver
                    ? elementTypeOf(receiver)
                    : null;
        };
        return type instanceof PrimitiveType primitive ? primitive : null;
    }

    private static @Nullable Type elementTypeOf(Expression receiver) {
        Type receiverType = StaticTypes.unwrap(receiver.getType());
        return switch (receiverType) {
            case MappingType mappingType -> StaticTypes.unwrap(mappingType.valueType());
            case ArrayType arrayType -> StaticTypes.unwrap(arrayType.getElementType());
            case DynamicArrayType dynamicArrayType ->
                StaticTypes.unwrap(dynamicArrayType.getElementType());
            case null, default -> null;
        };
    }

    private static @Nullable MatchResultInfo bind(MatchResultInfo matchCond, SchemaVariable sv,
            Term value, LogicServices lServices) {
        SVInstantiations inst = (SVInstantiations) matchCond.getInstantiations();
        Object existing = inst.getInstantiation(sv);
        if (existing == null) {
            return matchCond.setInstantiations(inst.add(sv, value, lServices));
        }
        return value.equals(existing) ? matchCond : null;
    }

    @Override
    public String toString() {
        String trigger = switch (mode) {
            case SELF -> "\\typeBounds";
            case SELF_SIGNED -> "\\signedTypeBounds";
            case FIELD -> "\\fieldTypeBounds";
            case ELEMENT -> "\\elementTypeBounds";
        };
        return trigger + "(" + targetSV.name() + ", " + minSV.name() + ", " + maxSV.name() + ")";
    }
}
