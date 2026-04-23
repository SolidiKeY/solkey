/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class FunctionCallExpression extends SolidityExpression {

    private final @NonNull ImmutableArray<Expression> arguments;

    public @NonNull Expression getFunctionExp() {
        return functionExp;
    }

    public final @NonNull Expression functionExp;

    public FunctionCallExpression(Type type, Expression functionExp, List<Expression> arguments) {
        super(type);
        this.functionExp = functionExp;
        this.arguments = new ImmutableArray<>(arguments);
    }

    public FunctionCallExpression(Type type, Expression functionExp,
            ImmutableArray<Expression> arguments) {
        super(type);
        this.functionExp = functionExp;
        this.arguments = arguments;
    }

    public FunctionCallExpression(ExtList children, Type type) {
        super(type);
        List<Expression> exprs = new java.util.ArrayList<>();
        Expression expr;
        while ((expr = children.removeFirstOccurrence(Expression.class)) != null) {
            exprs.add(expr);
        }
        this.functionExp = exprs.remove(exprs.size() - 1);
        this.arguments = new ImmutableArray<>(exprs);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (0 <= n && n < arguments.size()) {
            return arguments.get(n);
        }
        if (n == arguments.size())
            return functionExp;
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return arguments.size() + 1;
    }

    @Override
    public String toString() {
        return functionExp + "("
            + arguments.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
    }

    public void visit(Visitor v) {
        v.performActionOnFunctionCallExpression(this);
    }

    public @NonNull ImmutableArray<Expression> getArguments() {
        return arguments;
    }

    public Expression getArgument(int i) {
        return arguments.get(i);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FunctionCallExpression that))
            return false;
        return Objects.equals(arguments, that.arguments)
                && Objects.equals(functionExp, that.functionExp)
                && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arguments, functionExp, type);
    }
}
