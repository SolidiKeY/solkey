/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.expressions;

import java.math.BigInteger;

import org.key_project.solidity.logic.ast.abstractions.PrimitiveType;

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
