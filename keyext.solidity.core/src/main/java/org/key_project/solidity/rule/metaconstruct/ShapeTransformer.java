/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.TypedField;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.proof.init.StorageShapes;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

public class ShapeTransformer extends AbstractTermTransformer {
    public ShapeTransformer() {
        super(new Name("#shapeOf"), 1);
    }

    @Override
    public Term transform(Term term, SVInstantiations svInst, Services services) {
        Term subject = term.sub(0);
        Type type = switch (subject.op()) {
            case ProgramVariable pv -> StaticTypes.unwrap(pv.getType());
            case TypedField field -> field.type();
            default -> null;
        };
        String contract = subject.op() instanceof TypedField field ? contractOf(field) : null;
        return type == null
                ? services.getTermBuilder().func(services.getNamespaces().requireFunction("leaf"))
                : shapeOf(type, contract, services);
    }

    private static String contractOf(TypedField field) {
        String name = field.name().toString();
        int end = name.indexOf('$');
        return end < 0 ? null : name.substring(0, end);
    }

    private static Term shapeOf(Type type, String contract, Services services) {
        TermBuilder tb = services.getTermBuilder();
        final NamespaceSet namespaces = services.getNamespaces();
        return switch (type) {
            case ArrayType array ->
                tb.func(namespaces.requireFunction("fixedArr"), tb.zTerm(array.length()),
                    shapeOf(StaticTypes.unwrap(array.getElementType()), contract, services));
            case DynamicArrayType array -> tb.func(namespaces.requireFunction("dynArr"),
                shapeOf(StaticTypes.unwrap(array.getElementType()), contract, services));
            case MappingType mapping -> tb.func(namespaces.requireFunction("mapOf"),
                shapeOf(StaticTypes.unwrap(mapping.valueType()), contract, services));
            case StructDeclaration struct -> structShape(struct, contract, services);
            default -> tb.func(namespaces.requireFunction("leaf"));
        };
    }

    private static Term structShape(StructDeclaration struct, String contract,
            Services services) {
        TermBuilder tb = services.getTermBuilder();
        final NamespaceSet namespaces = services.getNamespaces();
        Function constant = contract == null ? null
                : namespaces.functions().lookup(new Name(StorageShapes
                        .shapeConstantName(contract, struct.name().toString())));
        return constant == null ? tb.func(namespaces.requireFunction("leaf")) : tb.func(constant);
    }
}
