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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class AssignExpression implements SolidityProgramElement, Expression, OperatorExpression {

    private final @NonNull Operator operator;
    private final @NonNull Expression lhs;
    private final @NonNull Expression rhs;

    public AssignExpression(Operator operator, Expression lhs, Expression rhs) {
        this.operator = Objects.requireNonNull(operator);
        this.lhs = Objects.requireNonNull(lhs);
        this.rhs = Objects.requireNonNull(rhs);
    }

    public AssignExpression(ExtList children) {
        this.operator = Objects.requireNonNull(children.removeFirstOccurrence(Operator.class));
        this.lhs = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.rhs = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public @Nullable Type getType() {
        return lhs.getType();
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> operator;
            case 1 -> lhs;
            case 2 -> rhs;
            default -> throw new IndexOutOfBoundsException(
                "Index should be 0 <= " + n + " < " + getChildCount());
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

    public @NonNull Expression getLeft() { return lhs; }

    public @NonNull Expression getRight() { return rhs; }

    @Override
    public Operator getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        return lhs + " " + operator.symbol() + " " + rhs;
    }
}
