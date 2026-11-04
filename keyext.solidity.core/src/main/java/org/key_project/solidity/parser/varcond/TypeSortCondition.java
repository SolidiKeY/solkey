/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.logic.sort.TypeSorts;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.jspecify.annotations.Nullable;

public final class TypeSortCondition extends TypeToSortCondition {
    public enum Mode {
        TYPE("\\hasTypeSort"), ELEMENT("\\hasElementTypeSort"), STATIC("\\hasStaticSort");

        private final String keyword;

        Mode(String keyword) {
            this.keyword = keyword;
        }
    }

    private final Mode mode;

    public TypeSortCondition(ProgramSV sv, GenericSort sort, Mode mode) {
        super(sv, sort);
        this.mode = mode;
    }

    @Override
    protected @Nullable Sort sortOf(SyntaxElement svSubst, Services services) {
        Type type = switch (svSubst) {
            case FieldDeclaration field -> field.getTypeReference().resolvedType();
            case Expression expression -> expression.getType();
            default -> null;
        };
        type = StaticTypes.unwrap(type);
        if (mode == Mode.ELEMENT) {
            type = StaticTypes.elementTypeOf(type);
        }
        if (type == null) {
            return null;
        }
        if (mode != Mode.STATIC) {
            return TypeSorts.of(type, services);
        }
        if (!TypeSorts.isStructured(type)) {
            return payloadSort(services, type, false);
        }
        Sort sort = TypeSorts.of(type, services);
        return TypeSorts.containsFixed(sort, services) ? sort : TypeSorts.structSort(services);
    }

    @Override
    protected String keyword() {
        return mode.keyword;
    }
}
