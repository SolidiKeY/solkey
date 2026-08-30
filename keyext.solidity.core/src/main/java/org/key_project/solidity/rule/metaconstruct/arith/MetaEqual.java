/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct.arith;

import java.math.BigInteger;

import org.key_project.logic.Name;

public final class MetaEqual extends MetaComparisonOp {
    public MetaEqual() {
        super(new Name("#eq"));
    }

    @Override
    protected boolean compare(BigInteger left, BigInteger right) {
        return left.compareTo(right) == 0;
    }
}
