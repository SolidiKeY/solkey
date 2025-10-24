/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic;

import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.Junctor;

public class TermBuilder {
    private final TermFactory tf;
    private final Term trueT;
    private final Term falseT;

    public TermBuilder(Services services) {
        this.tf = services.getTermFactory();
        this.trueT = services.getTermFactory().createTerm(Junctor.TRUE);
        this.falseT = services.getTermFactory().createTerm(Junctor.FALSE);
    }

    public Term and(Term... conjuncts) {
        if (conjuncts == null || conjuncts.length == 0) {
            return trueT;
        } else if (conjuncts.length == 1) {
            return conjuncts[0];
        } else {
            Term result = conjuncts[0];
            for (int i = 1; i < conjuncts.length; i++) {
                result = tf.createTerm(Junctor.AND, result, conjuncts[i]);
            }
            return result;
        }

    }

    public Term or(Term... disjuncts) {
        if (disjuncts == null || disjuncts.length == 0) {
            return falseT;
        } else if (disjuncts.length == 1) {
            return disjuncts[0];
        } else {
            Term result = disjuncts[0];
            for (int i = 1; i < disjuncts.length; i++) {
                result = tf.createTerm(Junctor.OR, result, disjuncts[i]);
            }
            return result;
        }

    }

    public Term func(Function op) {
        return tf.createTerm(op);
    }
}
