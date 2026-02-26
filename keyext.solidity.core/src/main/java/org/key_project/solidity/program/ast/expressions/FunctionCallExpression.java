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

public class FunctionCallExpression extends SolidityExpression {

    private final List<Expression> arguments;

    public Expression getFunctionExp() {
        return functionExp;
    }

    public final Expression functionExp;

    public FunctionCallExpression(Type type, Expression functionExp, List<Expression> arguments) {
        super(type);
        this.functionExp = functionExp;
        this.arguments = arguments;
    }

    public FunctionCallExpression(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.functionExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.arguments = Objects.requireNonNull(children.removeFirstOccurrence(List.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (0 <= n && n < arguments.size()) {
            return arguments.get(n);
        }
        if (n == arguments.size())
            return functionExp;
        throw new IndexOutOfBoundsException("Not 0 <= " + n + " < " + getChildCount());
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

    public List<Expression> getArguments() {
        return arguments;
    }

    public Expression getArgument(int i) {
        return arguments.get(i);
    }
}
