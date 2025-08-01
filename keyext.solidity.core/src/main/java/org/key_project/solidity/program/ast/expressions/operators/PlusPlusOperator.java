package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;

public class PlusPlusOperator extends UnaryBothCases {

    public PlusPlusOperator(Expression exp, Type type, boolean prefix) {
        super(exp, type, prefix);
    }

    @Override
    public String getOperator() {
        return "++";
    }
}
