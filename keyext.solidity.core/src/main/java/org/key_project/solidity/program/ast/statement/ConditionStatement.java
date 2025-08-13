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
        if(falseBody != null)
            s += " else " + falseBody;
        return s;
    }
}
