package org.key_project.solidity.program.ast.statement;

import org.jetbrains.annotations.NotNull;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.expressions.Expression;

import java.util.ArrayList;
import java.util.List;

public class ForStatement implements Statement {
    private final Expression initializationExpression;
    private final Expression condition;
    private final Expression loopExpression;
    private final Statement body;

    public ForStatement(Expression initializationExpression, Expression condition, Expression loopExpression, Statement body) {
        this.initializationExpression = initializationExpression;
        this.condition = condition;
        this.loopExpression = loopExpression;
        this.body = body;
    }

    @Override
    public @NotNull SyntaxElement getChild(int n) {
        List<Expression> exps = new ArrayList<>();
        exps.add(initializationExpression);
        exps.add(condition);
        exps.add(loopExpression);
        for(Expression exp: exps){
            if(exp == null)
                continue;
            if(n == 0)
                return exp;
            n -= 1;
        }
        if(n == 0)
            return body;
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        int n = 4;
        if(initializationExpression == null) n-= 1;
        if(condition == null) n-= 1;
        if(loopExpression == null) n-= 1;
        return n;
    }

    public String nullOrEmpty(Expression e){
        return e == null ? "" : e.toString();
    }

    @Override
    public String toString() {
        return "for(" + nullOrEmpty(initializationExpression)
                + "; " + nullOrEmpty(condition ) + "; " + nullOrEmpty(loopExpression) + ")\n" + body;
    }
}
