package org.key_project.solidity.logic.ast.expressions;

import org.key_project.solidity.logic.ast.abstractions.PrimitiveType;

import java.math.BigInteger;

public class Uint256Literal extends Literal {

    private final BigInteger value;

    public Uint256Literal(BigInteger value) {
        super(PrimitiveType.getPrimitiveType("uint256"));
        this.value = value;
    }

    public BigInteger getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
