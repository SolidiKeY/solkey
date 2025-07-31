package org.key_project.solidity.logic.ast.statement;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.expressions.Expression;

public class ReturnStatment implements Statement {
    private Expression returnExp;

    public ReturnStatment(Expression returnExp) {
        this.returnExp = returnExp;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public Expression getReturnExp() {
        return returnExp;
    }
}
