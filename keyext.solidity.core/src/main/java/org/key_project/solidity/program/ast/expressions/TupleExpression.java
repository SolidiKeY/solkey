/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;



public class TupleExpression extends SolidityExpression {
    private final ImmutableArray<Expression> expressions;

    public TupleExpression(Type type, List<Expression> expressions) {
        super(type);
        this.expressions = new ImmutableArray<>(expressions);
    }

    public TupleExpression(ExtList children, Type type) {
        super(type);
        List<Expression> components = new ArrayList<>();
        Expression component;
        while ((component = children.removeFirstOccurrence(Expression.class)) != null) {
            components.add(component);
        }
        this.expressions = new ImmutableArray<>(components);
    }

    public ImmutableArray<Expression> getExpressions() {
        return expressions;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (0 <= n && n < expressions.size())
            return Objects.requireNonNull(expressions.get(n));
        throw outOfBounds(n);
    }

    @Override
    public int getChildCount() {
        return expressions.size();
    }

    public Expression getExpression(int n) {
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
