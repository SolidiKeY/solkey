/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.expressions.Expression;

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

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        String s = "if(" + condition + ") " + trueBody;
        if (falseBody != null)
            s += " else " + falseBody;
        return s;
    }
}
