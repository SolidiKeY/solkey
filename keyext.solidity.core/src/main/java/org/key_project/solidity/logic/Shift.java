/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic;

import org.key_project.logic.Term;
import org.key_project.solidity.logic.op.LogicVariable;

public class Shift {
    private final TermBuilder tb;
    private final int shiftDistance;

    public Shift(int shiftDistance, TermBuilder tb) {
        this.shiftDistance = shiftDistance;
        this.tb = tb;
    }

    public Term apply(Term t) {
        if (shiftDistance == 0) {
            return t;
        }
        return apply(t, 1);
    }

    private Term apply(Term t, int cutoff) {
        if (t.op() instanceof LogicVariable lv) {
            if (lv.getIndex() < cutoff) {
                return t;
            }
            return tb.var(LogicVariable.create(lv.getIndex() + shiftDistance, lv.sort()));
        }
        if (t.freeVars().isEmpty()) {
            return t;
        }
        final Term[] newSubterms = new Term[t.arity()];
        for (int i = 0; i < newSubterms.length; i++) {
            final int subCutoff =
                t.op().bindVarsAt(i) ? cutoff + t.varsBoundHere(i).size() : cutoff;
            newSubterms[i] = apply(t.sub(i), subCutoff);
        }
        return tb.tf().createTerm(t.op(), newSubterms, t.boundVars());
    }
}
