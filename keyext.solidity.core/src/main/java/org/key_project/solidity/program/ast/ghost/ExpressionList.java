/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.ghost;


import java.util.List;
import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class ExpressionList implements SolidityProgramElement {
    public @NonNull ImmutableArray<@NonNull Expression> getExpressions() {
        return expressions;
    }

    private final @NonNull ImmutableArray<@NonNull Expression> expressions;

    public ExpressionList(List<@NonNull Expression> expressions) {
        this.expressions = new ImmutableArray<>(expressions);
    }

    @Override
    @NonNull
    public SyntaxElement getChild(int n) {
        return Objects.requireNonNull(expressions.get(n));
    }

    @Override
    public int getChildCount() {
        return expressions.size();
    }

    @Override
    public void visit(Visitor v) {
    }


}
