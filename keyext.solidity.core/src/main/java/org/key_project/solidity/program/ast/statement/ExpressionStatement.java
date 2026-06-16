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
    private int hashcode = -1;

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
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ExpressionStatement that))
            return false;
        return expression.equals(that.expression);
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
