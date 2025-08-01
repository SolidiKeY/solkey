package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.expressions.Expression;

public class MinusMinusOperator extends UnaryBothCases {
    public MinusMinusOperator(Expression exp, boolean prefix) {
        super(exp, prefix);
    }

    @Override
    public String getOperator() {
        return "--";
    }
}
