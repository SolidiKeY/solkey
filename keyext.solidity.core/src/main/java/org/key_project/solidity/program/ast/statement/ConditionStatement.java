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
import org.jspecify.annotations.Nullable;

public class ConditionStatement implements Statement {
    Expression condition;
    Statement thenBody;
    @Nullable
    Statement elseBody;

    public ConditionStatement(Expression condition, Statement thenBody) {
        this.condition = condition;
        this.thenBody = thenBody;
    }

    public ConditionStatement(Expression condition, Statement thenBody, Statement elseBody) {
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    public ConditionStatement(ExtList children) {
        this.condition = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.thenBody = Objects.requireNonNull(children.removeFirstOccurrence(Statement.class));
        this.elseBody = children.removeFirstOccurrence(Statement.class);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> condition;
            case 1 -> thenBody;
            default -> {
                if (getChildCount() == 3 && n == 2)
                    yield Objects.requireNonNull(elseBody);
                throw new IndexOutOfBoundsException(
                    "Index should be 0 <= " + n + " < " + getChildCount());
            }
        };
    }

    @Override
    public int getChildCount() {
        if (elseBody == null)
            return 2;
        return 3;
    }

    @Override
    public String toString() {
        String s = "if(" + condition + ") " + thenBody;
        if (elseBody != null)
            s += " else " + elseBody;
        return s;
    }

    public void visit(Visitor v) {
        v.performActionOnConditionStatement(this);
    }

    public Expression getCondition() {
        return condition;
    }

    public Statement getThenBody() {
        return thenBody;
    }

    public @Nullable Statement getElseBody() {
        return elseBody;
    }
}
