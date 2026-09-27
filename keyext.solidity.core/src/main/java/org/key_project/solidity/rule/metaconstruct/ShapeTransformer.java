/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.TypedField;
import org.key_project.solidity.program.ast.StaticTypes;
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
            case ProgramVariable pv -> StaticTypes.unwrap(pv.getType());
            case TypedField field -> field.type();
            default -> null;
        };
        return type == null ? services.getTermBuilder().func(services.requireFunction("leaf"))
                : shapeOf(type, services);
    }

    private static Term shapeOf(Type type, Services services) {
        TermBuilder tb = services.getTermBuilder();
        return switch (type) {
            case ArrayType array ->
                tb.func(services.requireFunction("fixedArr"), tb.zTerm(array.length()),
                    shapeOf(StaticTypes.unwrap(array.getElementType()), services));
            case DynamicArrayType array -> tb.func(services.requireFunction("dynArr"),
                shapeOf(StaticTypes.unwrap(array.getElementType()), services));
            default -> tb.func(services.requireFunction("leaf"));
        };
    }
}
