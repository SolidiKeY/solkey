/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.Nullable;

import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.BOOL;

public final class BinaryExpression
        implements SolidityProgramElement, Expression, OperatorExpression {

    protected final Operator operator;
    protected final Expression left;
    protected final Expression right;
    private int hashcode = -1;

    public BinaryExpression(Operator operator, Expression left, Expression right) {
        this.operator = operator;
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
    }

    public BinaryExpression(ExtList children) {
        this.operator = Objects.requireNonNull(children.removeFirstOccurrence(Operator.class));
        this.left = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.right = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        switch (n) {
            case 0:
                return operator;
            case 1:
                return left;
            case 2:
                return right;
            default:
                throw outOfBounds(n);
        }
    }

    @Override
    public int getChildCount() {
        return 3;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnBinaryExpression(this);
    }

    public Expression getLeft() { return left; }

    public Expression getRight() { return right; }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public Type getType() {
        return switch (operator) {
            case EQUAL, NOT_EQUAL, LESS_THAN, GREATER_THAN, LESS_EQUAL, GREATER_EQUAL,
                    LOGICAL_AND, LOGICAL_OR ->
                BOOL;
            default -> left.getType();
        };
    }

    public String toString() {
        return left + " " + operator.symbol() + " " + right;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BinaryExpression that))
            return false;
        return operator.equals(that.operator) && left.equals(that.left)
                && right.equals(that.right);
    }

    @Override
    public int hashCode() {
        if (hashcode == -1) {
            int hash = computeHashCode();
            hashcode = hash == -1 ? 0 : hash;
        }
        return hashcode;
    }

}
