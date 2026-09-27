/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.jspecify.annotations.Nullable;

/// Binds a generic sort to the Solidity type of a matched member field.
public final class FieldExpressionTypeToSortCondition extends TypeToSortCondition {
    private final boolean memoryPayload;

    public FieldExpressionTypeToSortCondition(ProgramSV fieldSV, GenericSort sort) {
        this(fieldSV, sort, false);
    }

    public FieldExpressionTypeToSortCondition(ProgramSV fieldSV, GenericSort sort,
            boolean memoryPayload) {
        super(fieldSV, sort);
        this.memoryPayload = memoryPayload;
    }

    @Override
    protected @Nullable Sort sortOf(SyntaxElement svSubst, Services services) {
        if (!(svSubst instanceof FieldDeclaration fd)) {
            return null;
        }
        return payloadSort(services, fd.getTypeReference().resolvedType(), memoryPayload);
    }

    @Override
    protected String keyword() {
        return memoryPayload ? "\\hasMemoryFieldSort" : "\\hasFieldSort";
    }
}
