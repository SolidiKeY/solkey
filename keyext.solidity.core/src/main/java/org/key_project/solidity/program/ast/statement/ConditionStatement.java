/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class ConditionStatement implements Statement {
    Expression condition;
    Statement trueBody;
    Statement falseBody;

    public ConditionStatement(Expression condition, Statement trueBody) {
        this.condition = condition;
        this.trueBody = trueBody;
    }

    public ConditionStatement(Expression condition, Statement trueBody, Statement falseBody) {
        this.condition = condition;
        this.trueBody = trueBody;
        this.falseBody = falseBody;
    }

    public ConditionStatement(ExtList children) {
        this.condition = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.trueBody = Objects.requireNonNull(children.removeFirstOccurrence(Statement.class));
        this.falseBody = Objects.requireNonNull(children.removeFirstOccurrence(Statement.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> condition;
            case 1 -> trueBody;
            default -> {
                if (getChildCount() == 3 && n == 2) {
                    yield falseBody;
                }
                throw new IndexOutOfBoundsException();
            }
        };
    }

    @Override
    public int getChildCount() {
        if (falseBody == null)
            return 2;
        return 3;
    }

    @Override
    public String toString() {
        String s = "if(" + condition + ") " + trueBody;
        if (falseBody != null)
            s += " else " + falseBody;
        return s;
    }

    public void visit(Visitor v) {
        v.performActionOnConditionStatement(this);
    }
}
