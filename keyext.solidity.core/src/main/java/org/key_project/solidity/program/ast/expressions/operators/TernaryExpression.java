/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import static org.key_project.solidity.program.ast.SolidityProgramElement.takeChild;


public class TernaryExpression extends SolidityExpression {

    protected final Expression condition;
    protected final Expression falseExpression;
    protected final Expression trueExpression;

    public TernaryExpression(Type expType, Expression condition, Expression falseExpression,
            Expression trueExpression) {
        super(expType);
        this.condition = condition;
        this.falseExpression = falseExpression;
        this.trueExpression = trueExpression;
    }

    public int getPrecedence() {
        return Operator.COPY_ASSIGN.precedence(); // according to
                                                  // https://docs.soliditylang.org/en/latest/cheatsheet.html
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
            throw outOfBounds(n);
    }

    @Override
    public String toString() {
        return condition + " ? " + trueExpression + " : " + falseExpression;
    }

    @Override
    public int getChildCount() {
        return 3;
    }

    public void visit(Visitor v) {
        v.performActionOnTernaryExpression(this);
    }

    public TernaryExpression(ExtList children, Type type) {
        super(type);
        this.condition = takeChild(children, Expression.class);
        this.falseExpression =
            takeChild(children, Expression.class);
        this.trueExpression =
            takeChild(children, Expression.class);
    }

    public Expression getCondition() {
        return condition;
    }

    public Expression getFalseExpression() {
        return falseExpression;
    }

    public Expression getTrueExpression() {
        return trueExpression;
    }
}
