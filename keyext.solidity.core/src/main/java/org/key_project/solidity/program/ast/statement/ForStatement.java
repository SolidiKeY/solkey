/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.ArrayList;
import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class ForStatement extends LoopStatement {
    private final ForInit init;
    private final ForUpdate update;

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
        // TODO: Don't use array List
        List<SolidityProgramElement> exps = new ArrayList<>();
        exps.add(init);
        exps.add(condition);
        exps.add(update);
        for (var exp : exps) {
            if (exp == null)
                continue;
            if (n == 0)
                return exp;
            n -= 1;
        }
        if (n == 0)
            return body;
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

    public ForInit getInit() {
        return init;
    }

    public ForUpdate getUpdate() {
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
