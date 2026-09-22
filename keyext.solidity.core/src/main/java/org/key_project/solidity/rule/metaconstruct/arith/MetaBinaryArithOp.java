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

/// Term transformer evaluating a binary integer operation on two literal arguments,
/// producing an integer literal.
public abstract class MetaBinaryArithOp extends AbstractTermTransformer {

    protected MetaBinaryArithOp(Name name) {
        super(name, 2);
    }

    protected abstract BigInteger compute(BigInteger left, BigInteger right);

    @Override
    public Term transform(Term term, SVInstantiations svInst, Services services) {
        BigInteger left = new BigInteger(convertToDecimalString(term.sub(0), services));
        BigInteger right = new BigInteger(convertToDecimalString(term.sub(1), services));
        return services.getTermBuilder().zTerm(compute(left, right).toString());
    }
}
