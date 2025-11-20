/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.literals;

import java.math.BigInteger;

import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.visitor.Visitor;

public class Uint256Literal extends Literal {

    private final BigInteger value;

    public Uint256Literal(BigInteger value) {
        super(PrimitiveType.UINT256);
        this.value = value;
    }

    public BigInteger getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public void visit(Visitor v){
        v.performActionOnUint256Literal(this);
    }
}
