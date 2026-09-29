/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.HashCachingElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.Nullable;

import static org.key_project.solidity.program.ast.SolidityProgramElement.takeChild;

public final class AssignExpression extends HashCachingElement implements OperatorExpression {

    private final Operator operator;
    private final Expression lhs;
    private final Expression rhs;

    public AssignExpression(Operator operator, Expression lhs, Expression rhs) {
        this.operator = Objects.requireNonNull(operator);
        this.lhs = Objects.requireNonNull(lhs);
        this.rhs = Objects.requireNonNull(rhs);
    }

    public AssignExpression(ExtList children) {
        this.operator = takeChild(children, Operator.class);
        this.lhs = takeChild(children, Expression.class);
        this.rhs = takeChild(children, Expression.class);
    }

    @Override
    public Type getType() {
        return lhs.getType();
    }

    @Override
    public SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> operator;
            case 1 -> lhs;
            case 2 -> rhs;
            default -> throw outOfBounds(n);
        };
    }

    @Override
    public int getChildCount() {
        return 3;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnAssignExpression(this);
    }

    public Expression getLeft() { return lhs; }

    public Expression getRight() { return rhs; }

    @Override
    public Operator getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        return lhs + " " + operator.symbol() + " " + rhs;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AssignExpression that))
            return false;
        return operator.equals(that.operator) && lhs.equals(that.lhs) && rhs.equals(that.rhs);
    }

}
