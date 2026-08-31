/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.SolidityDLTheory;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

import org.jspecify.annotations.Nullable;

/// Produces the per-type range predicate of a matched program element's declared Solidity
/// integer type: `#inBounds(v, e)` becomes e.g. `inUint8(e)` when `v` is a `uint8` variable.
/// The predicate symbols are declared by [org.key_project.solidity.theory.IntLDT]; their
/// meaning is given by the choice-guarded expansion taclets of
/// [org.key_project.solidity.proof.init.InBoundsTacletGenerator], mirroring Java KeY's
/// `inInt`/`expandInInt` design. When the target's type is not a bounded integer type (e.g. a
/// program variable carrying only the KeY sort `int`), the guard degenerates to `true`, i.e.
/// unbounded mathematical integers.
public final class MetaInBounds extends AbstractTermTransformer {

    public enum Mode {
        /// `#inBounds(v, e)` — bounds of the element's own declared type
        SELF,
        /// `#inBoundsField(f, e)` — bounds of a matched field's declared type
        FIELD,
        /// `#inBoundsElem(r, e)` — bounds of an indexed receiver's element type
        ELEMENT
    }

    private final Mode mode;

    public MetaInBounds(Mode mode, String name) {
        super(new Name(name), 2, SolidityDLTheory.FORMULA);
        this.mode = mode;
    }

    @Override
    public Term transform(Term term, SVInstantiations svInst, Services services) {
        throw new UnsupportedOperationException(
            name() + " needs the uninstantiated taclet term to resolve the target's type");
    }

    @Override
    public Term transform(Term schemaTerm, Term term, SVInstantiations svInst,
            Services services) {
        Type type = resolveType(schemaTerm.sub(0), svInst);
        Function predicate =
            type == null ? null : services.getTheoryInfo().getIntLDT().getInBounds(type);
        if (predicate == null) {
            return services.getTermBuilder().tt();
        }
        return services.getTermBuilder().func(predicate, term.sub(1));
    }

    private @Nullable Type resolveType(Term targetSchemaTerm, SVInstantiations svInst) {
        Object target = targetSchemaTerm.op() instanceof SchemaVariable sv
                ? svInst.getInstantiation(sv)
                : targetSchemaTerm.op();
        if (target instanceof Term instantiationTerm) {
            target = instantiationTerm.op();
        }
        return switch (mode) {
            case SELF -> target instanceof SolidityProgramElement pe
                    ? StaticTypes.typeOf(pe)
                    : null;
            case FIELD -> target instanceof FieldDeclaration fd
                    ? StaticTypes.unwrap(fd.getTypeReference().resolvedType())
                    : null;
            case ELEMENT -> target instanceof Expression receiver
                    ? elementTypeOf(receiver)
                    : null;
        };
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
}
