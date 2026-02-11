/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct.arith;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.rule.metaconstruct.AbstractTermTransformer;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

import java.math.BigInteger;

public class MetaLeq extends AbstractTermTransformer {
    public MetaLeq() {
        super(new Name("#leq"), 2);
    }

    public Term transform(Term term, SVInstantiations svInst, Services services) {
        Term arg1 = term.sub(0);
        Term arg2 = term.sub(1);
        BigInteger bigIntArg1;
        BigInteger bigIntArg2;

        bigIntArg1 = new BigInteger(convertToDecimalString(arg1, services));
        bigIntArg2 = new BigInteger(convertToDecimalString(arg2, services));
        boolean result = bigIntArg1.compareTo(bigIntArg2) <= 0;

        if (result) {
            return services.getTermBuilder().tt();
        } else {
            return services.getTermBuilder().ff();
        }
    }
}
