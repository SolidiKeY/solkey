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

public class ReturnStatement implements Statement {
    private @Nullable Expression returnExp;

    public ReturnStatement(@Nullable Expression returnExp) {
        this.returnExp = returnExp;
    }

    public ReturnStatement(ExtList children) {
        this.returnExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0 && returnExp != null)
            return returnExp;
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return returnExp != null ? 1 : 0;
    }

    public @Nullable Expression getReturnExp() {
        return returnExp;
    }

    @Override
    public String toString() {
        return getReturnExp() == null ? "return;" : "return " + getReturnExp() + ";";
    }

    public void visit(Visitor v) {
        v.performActionOnReturnStatment(this);
    }
}
