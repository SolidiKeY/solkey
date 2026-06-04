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

public class UnaryExpression extends SolidityExpression {

    private final Operator operator;
    private final Expression exp;

    public UnaryExpression(Operator operator, Expression exp, Type type) {
        super(type);
        assert type != null;
        this.operator = operator;
        this.exp = exp;
    }

    public UnaryExpression(ExtList children, Type type) {
        super(type);
        assert type != null;
        this.operator = Objects.requireNonNull(children.removeFirstOccurrence(Operator.class));
        this.exp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        switch (n) {
            case 0:
                return operator;
            case 1:
                return exp;
            default:
                throw new IndexOutOfBoundsException(
                    "Index should be 0 <= " + n + " < " + getChildCount());
        }
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnUnaryExpression(this);
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression getExp() { return exp; }

    public String toString() {
        switch (operator) {
            case POST_INC, POST_DEC:
                return exp + operator.symbol();
            case PRE_INC, PRE_DEC, LOGICAL_NOT, BITWISE_NOT, UNARY_MINUS:
                return operator.symbol() + exp;
            default:
                throw new IllegalStateException("Unknown unary operator: " + operator);
        }
    }
}
