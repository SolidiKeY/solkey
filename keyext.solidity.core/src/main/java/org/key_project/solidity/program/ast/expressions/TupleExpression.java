/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

public class TupleExpression extends SolidityExpression {
    private final @NonNull ImmutableArray<Expression> expressions;

    public TupleExpression(Type type, List<Expression> expressions) {
        super(type);
        this.expressions = new ImmutableArray<>(expressions);
    }

    public TupleExpression(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        List<Expression> exprList = Objects.requireNonNull(children.removeFirstOccurrence(List.class));
        this.expressions = new ImmutableArray<>(exprList);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (0 <= n && n < expressions.size())
            return Objects.requireNonNull(expressions.get(n));
        throw new IndexOutOfBoundsException("Not 0 <= " + n + " < " + expressions.size());
    }

    @Override
    public int getChildCount() {
        return expressions.size();
    }

    public @NonNull Expression getExpression(int n) {
        return Objects.requireNonNull(expressions.get(n));
    }

    @Override
    public String toString() {
        return "[" + expressions.stream().map(Objects::toString).collect(Collectors.joining(", "))
            + "]";
    }

    public void visit(Visitor v) {
        v.performActionOnTupleExpression(this);
    }
}
