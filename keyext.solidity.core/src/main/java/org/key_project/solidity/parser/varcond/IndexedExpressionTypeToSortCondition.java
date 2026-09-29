/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.jspecify.annotations.Nullable;

/// Binds a generic sort to the element/value type of an indexed storage receiver.
public final class IndexedExpressionTypeToSortCondition extends TypeToSortCondition {
    private final boolean memoryPayload;

    public IndexedExpressionTypeToSortCondition(ProgramSV receiverSV, GenericSort sort) {
        this(receiverSV, sort, false);
    }

    public IndexedExpressionTypeToSortCondition(ProgramSV receiverSV, GenericSort sort,
            boolean memoryPayload) {
        super(receiverSV, sort);
        this.memoryPayload = memoryPayload;
    }

    @Override
    protected @Nullable Sort sortOf(SyntaxElement svSubst, Services services) {
        if (!(svSubst instanceof Expression receiver)) {
            return null;
        }
        Type elementType = StaticTypes.elementTypeOf(StaticTypes.unwrap(receiver.getType()));
        return elementType == null ? null : payloadSort(services, elementType, memoryPayload);
    }

    @Override
    protected String keyword() {
        return memoryPayload ? "\\hasMemoryElementSort" : "\\hasElementSort";
    }
}
