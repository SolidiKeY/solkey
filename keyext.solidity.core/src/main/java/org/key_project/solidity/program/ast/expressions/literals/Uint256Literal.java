/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.literals;

import java.math.BigInteger;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.solidity.theory.IntLDT;
import org.key_project.util.ExtList;

import org.jspecify.annotations.Nullable;

public class Uint256Literal extends Literal {

    @Override
    public Name getLDTName() { return IntLDT.NAME; }

    private final BigInteger value;

    public Uint256Literal(BigInteger value) {
        super(PrimitiveType.UINT256);
        this.value = value;
    }

    public Uint256Literal(ExtList children) {
        super(PrimitiveType.UINT256);
        this.value = Objects.requireNonNull(children.removeFirstOccurrence(BigInteger.class));
    }

    public BigInteger getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public void visit(Visitor v) {
        v.performActionOnUint256Literal(this);
    }

    @Override
    public int computeHashCode() {
        return 37 * super.computeHashCode() + value.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Uint256Literal that))
            return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
