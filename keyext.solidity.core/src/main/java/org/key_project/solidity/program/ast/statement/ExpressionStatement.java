/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;

public class ExpressionStatement implements Statement {
    final @NonNull Expression expression;

    public ExpressionStatement(Expression expression) {
        this.expression = Objects.requireNonNull(expression);
    }

    public ExpressionStatement(ExtList children) {
        this.expression = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return expression;
        throw new IndexOutOfBoundsException("index " + n + " is out of bounds");
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        return expression.toString() + ";";
    }

    public void visit(Visitor v) {
        v.performActionOnExpressionStatement(this);
    }

    public @NonNull Expression getExpression() {
        return expression;
    }
}
