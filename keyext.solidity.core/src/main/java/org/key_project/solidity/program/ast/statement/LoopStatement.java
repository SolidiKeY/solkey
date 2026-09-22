/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.util.ExtList;

import org.jspecify.annotations.Nullable;

public abstract class LoopStatement implements Statement {
    protected final @Nullable Expression condition;
    protected final Statement body;

    protected LoopStatement(@Nullable Expression condition, Statement body) {
        this.condition = condition;
        this.body = Objects.requireNonNull(body);
    }

    public LoopStatement(ExtList children) {
        this.condition = Objects.requireNonNull(children.get(Expression.class));
        this.body = Objects.requireNonNull(children.get(Statement.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> Objects.requireNonNull(condition);
            case 1 -> body;
            default -> throw outOfBounds(n);
        };
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    public @Nullable Expression getCondition() {
        return condition;
    }

    public Statement getBody() {
        return body;
    }
}
