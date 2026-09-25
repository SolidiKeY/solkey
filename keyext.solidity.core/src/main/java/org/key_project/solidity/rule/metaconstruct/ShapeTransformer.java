/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.TypedField;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

public class ShapeTransformer extends AbstractTermTransformer {
    public ShapeTransformer() {
        super(new Name("#shapeOf"), 1);
    }

    @Override
    public Term transform(Term term, SVInstantiations svInst, Services services) {
        Term subject = term.sub(0);
        Type type = switch (subject.op()) {
            case ProgramVariable pv -> TypedField.unwrap(pv.getType());
            case TypedField field -> field.type();
            default -> null;
        };
        return type == null ? services.getTermBuilder().func(shape("leaf", services))
                : shapeOf(type, services);
    }

    private static Term shapeOf(Type type, Services services) {
        TermBuilder tb = services.getTermBuilder();
        return switch (type) {
            case ArrayType array -> tb.func(shape("fixedArr", services), tb.zTerm(array.length()),
                shapeOf(TypedField.unwrap(array.getElementType()), services));
            case DynamicArrayType array -> tb.func(shape("dynArr", services),
                shapeOf(TypedField.unwrap(array.getElementType()), services));
            default -> tb.func(shape("leaf", services));
        };
    }

    private static Function shape(String name, Services services) {
        Function function = services.getNamespaces().functions().lookup(new Name(name));
        if (function == null) {
            throw new IllegalStateException("Shape constructor " + name + " is not declared");
        }
        return function;
    }
}
