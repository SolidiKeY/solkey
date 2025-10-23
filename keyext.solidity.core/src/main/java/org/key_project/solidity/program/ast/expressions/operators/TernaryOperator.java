/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;

public class TernaryOperator extends Expression {

    protected final Expression condition;
    protected final Expression falseExpression;
    protected final Expression trueExpression;

    public TernaryOperator(Type expType, Expression condition, Expression falseExpression,
            Expression trueExpression) {
        super(expType);
        this.condition = condition;
        this.falseExpression = falseExpression;
        this.trueExpression = trueExpression;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0)
            return condition;
        else if (n == 1)
            return falseExpression;
        else if (n == 2)
            return trueExpression;
        else
            throw new IndexOutOfBoundsException();
    }

    @Override
    public String toString() {
        return condition + " ? " + trueExpression + " : " + falseExpression;
    }

    @Override
    public int getChildCount() {
        return 3;
    }
}
