package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;

public class MinusMinusOperator extends UnaryBothCases {
    public MinusMinusOperator(Expression exp, Type type, boolean prefix) {
        super(exp, type, prefix);
    }

    @Override
    public String getOperator() {
        return "--";
    }
}
