/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class ReturnStatment implements Statement {
    private Expression returnExp;

    public ReturnStatment() { }

    public ReturnStatment(Expression returnExp) {
        this.returnExp = returnExp;
    }

    public ReturnStatment(ExtList children) {
        this.returnExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public Expression getReturnExp() {
        return returnExp;
    }

    @Override
    public String toString() {
        return "return " + getReturnExp().toString() + ";";
    }

    public void visit(Visitor v) {
        v.performActionOnReturnStatment(this);
    }
}
