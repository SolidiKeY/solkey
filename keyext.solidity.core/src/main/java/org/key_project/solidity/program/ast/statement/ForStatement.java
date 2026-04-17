/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import org.jspecify.annotations.Nullable;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class ForStatement extends LoopStatement {
    private final @Nullable ForInit init;
    private final @Nullable ForUpdate update;

    public ForStatement(ForInit init, Expression condition,
            ForUpdate updateExpression, Statement body) {
        super(condition, body);
        this.init = init;
        this.update = updateExpression;
    }

    public ForStatement(ExtList children) {
        super(children);
        this.init = children.get(ForInit.class);
        this.update = children.get(ForUpdate.class);
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (init != null) {
            if (n == 0) return init;
            n--;
        }
        if (condition != null) {
            if (n == 0) return condition;
            n--;
        }
        if (update != null) {
            if (n == 0) return update;
            n--;
        }
        if (n == 0) return body;
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        int n = 4;
        if (init == null)
            n -= 1;
        if (condition == null)
            n -= 1;
        if (update == null)
            n -= 1;
        return n;
    }

    public @Nullable ForInit getInit() {
        return init;
    }

    public @Nullable ForUpdate getUpdate() {
        return update;
    }

    public String nullOrEmpty(SolidityProgramElement e) {
        return e == null ? "" : e.toString();
    }

    @Override
    public String toString() {
        return "for(" + nullOrEmpty(init)
            + "; " + nullOrEmpty(condition) + "; " + nullOrEmpty(update) + ")\n" + body;
    }

    public void visit(Visitor v) {
        v.performActionOnForStatement(this);
    }
}
