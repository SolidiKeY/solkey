package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.expressions.Expression;

public class PlusPlusOperator extends UnaryBothCases {
    public PlusPlusOperator(Expression exp, boolean prefix) {
        super(exp, prefix);
    }

    @Override
    public String getOperator() {
        return "++";
    }
}
