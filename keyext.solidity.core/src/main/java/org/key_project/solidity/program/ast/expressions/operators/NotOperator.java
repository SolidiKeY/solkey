package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;

public class NotOperator extends UnaryOperator {
    public NotOperator(Expression exp, Type type) {
        super(exp, type);
    }

    @Override
    public String getOperator() {
        return "!";
    }

    @Override
    public boolean isPrefix() {
        return true;
    }
}
