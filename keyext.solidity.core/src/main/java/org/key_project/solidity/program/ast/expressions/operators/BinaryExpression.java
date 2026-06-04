/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;

public final class BinaryExpression extends SolidityExpression implements OperatorExpression {

    protected final @NonNull Operator operator;
    protected final @NonNull Expression left;
    protected final @NonNull Expression right;

    public BinaryExpression(Operator operator, Expression left, Expression right, Type type) {
        super(type);
        assert type != null;
        this.operator = operator;
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
    }

    public BinaryExpression(ExtList children, Type type) {
        super(type);
        assert type != null;
        this.operator = Objects.requireNonNull(children.removeFirstOccurrence(Operator.class));
        this.left = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.right = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        switch (n) {
            case 0:
                return operator;
            case 1:
                return left;
            case 2:
                return right;
            default:
                throw new IndexOutOfBoundsException(
                    "Index should be 0 <= " + n + " < " + getChildCount());
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

    public @NonNull Expression getLeft() { return left; }

    public @NonNull Expression getRight() { return right; }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public Type getType() {
        return type;
    }

    public String getName() {
        return operator.symbol();
    }

    public String toString() {
        return left + " " + getName() + " " + right;
    }

}
