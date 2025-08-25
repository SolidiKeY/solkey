package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;

public class NegateOperator extends UnaryOperator {
    public NegateOperator(Expression exp, Type type) {
        super(exp, type);
    }

    @Override
    public String getOperator() {
        return "-";
    }

    @Override
    public boolean isPrefix() {
        return true;
    }
}
