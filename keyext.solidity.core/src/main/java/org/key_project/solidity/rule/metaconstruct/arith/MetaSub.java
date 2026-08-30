/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct.arith;

import java.math.BigInteger;

import org.key_project.logic.Name;

public final class MetaSub extends MetaBinaryArithOp {
    public MetaSub() {
        super(new Name("#sub"));
    }

    @Override
    protected BigInteger compute(BigInteger left, BigInteger right) {
        return left.subtract(right);
    }
}
