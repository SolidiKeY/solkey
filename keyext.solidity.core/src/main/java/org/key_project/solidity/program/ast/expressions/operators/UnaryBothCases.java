package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.expressions.Expression;

abstract class UnaryBothCases extends UnaryOperator {
    protected boolean prefix;

    protected UnaryBothCases(Expression exp, boolean prefix) {
        super(exp);
        this.prefix = prefix;
    }

    @Override
    public boolean isPrefix() {
        return prefix;
    }
}
