/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class DoWhileStatement implements Statement {

    private final Expression condition;
    private final Statement body;

    public DoWhileStatement(Expression condition, Statement body) {
        this.condition = condition;
        this.body = body;
    }

    public DoWhileStatement(ExtList children) {
        this.condition = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.body = Objects.requireNonNull(children.removeFirstOccurrence(Statement.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> condition;
            case 1 -> body;
            default -> throw new IndexOutOfBoundsException(
                "Index should be 0 <= " + n + " < " + getChildCount());
        };
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public String toString() {
        return "do " + body + " while (" + condition + ")";
    }

    public void visit(Visitor v) {
        v.performActionOnDoWhileStatement(this);
    }
}
