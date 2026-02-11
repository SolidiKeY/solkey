/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class WhileStatement extends LoopStatement {

    public WhileStatement(Expression condition, Statement body) {
        super(condition, body);
    }

    public WhileStatement(ExtList children) {
        super(children);
    }

    @Override
    public String toString() {
        return "while(" + condition + ")\n" + body;
    }

    public void visit(Visitor v) {
        v.performActionOnWhileStatement(this);
    }
}
