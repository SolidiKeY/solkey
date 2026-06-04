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

public class TernaryExpression extends SolidityExpression {

    protected final Expression condition;
    protected final Expression falseExpression;
    protected final Expression trueExpression;

    public TernaryExpression(Type expType, Expression condition, Expression falseExpression,
            Expression trueExpression) {
        super(expType);
        this.condition = condition;
        this.falseExpression = falseExpression;
        this.trueExpression = trueExpression;
    }

    public int getPrecedence() {
        return Operator.COPY_ASSIGN.precedence(); // according to
                                                  // https://docs.soliditylang.org/en/latest/cheatsheet.html
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return condition;
        else if (n == 1)
            return falseExpression;
        else if (n == 2)
            return trueExpression;
        else
            throw new IndexOutOfBoundsException(
                "Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public String toString() {
        return condition + " ? " + trueExpression + " : " + falseExpression;
    }

    @Override
    public int getChildCount() {
        return 3;
    }

    public void visit(Visitor v) {
        v.performActionOnTernaryExpression(this);
    }

    public TernaryExpression(ExtList children, Type type) {
        super(type);
        this.condition = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.falseExpression =
            Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.trueExpression =
            Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    public Expression getCondition() {
        return condition;
    }

    public Expression getFalseExpression() {
        return falseExpression;
    }

    public Expression getTrueExpression() {
        return trueExpression;
    }
}
