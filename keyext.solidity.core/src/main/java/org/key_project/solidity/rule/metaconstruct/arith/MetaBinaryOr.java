/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct.arith;

import org.key_project.logic.Name;

import java.math.BigInteger;

public final class MetaBinaryOr extends MetaArithBitMaskOp {
    public MetaBinaryOr() {
        super(new Name("#BinaryOr"));
    }


    @Override
    protected BigInteger bitmaskOp(BigInteger left, BigInteger right) {
        return left.or(right);
    }
}
