/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct.arith;

import java.math.BigInteger;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.metaconstruct.AbstractTermTransformer;

public abstract class MetaArithBitMaskOp extends AbstractTermTransformer {
    public MetaArithBitMaskOp(Name name) {
        super(name, 2);
    }

    protected abstract BigInteger bitmaskOp(BigInteger left, BigInteger right);

    public Term transform(Term term, SVInstantiations svInst, Services services) {
        Term arg1 = term.sub(0);
        Term arg2 = term.sub(1);
        BigInteger left;
        BigInteger right;

        left = new BigInteger(convertToDecimalString(arg1, services));
        right = new BigInteger(convertToDecimalString(arg2, services));

        BigInteger result = bitmaskOp(left, right);

        return services.getTermBuilder().zTerm(result.toString());
    }
}
