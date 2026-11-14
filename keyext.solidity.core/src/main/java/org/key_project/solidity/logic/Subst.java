/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic;

import org.key_project.logic.Term;
import org.key_project.solidity.logic.op.LogicVariable;

public class Subst {
    private final Term s;
    private final TermBuilder tb;
    private final int target;

    public Subst(Term sub, TermBuilder tb) {
        this(sub, 1, tb);
    }

    public Subst(Term sub, int target, TermBuilder tb) {
        this.s = sub;
        this.target = target;
        this.tb = tb;
    }

    /// substitute <code>s</code> for the variable with de Bruijn index <code>target</code> in
    /// <code>t</code> and remove that variable from the context: the indices of the variables
    /// bound further out are lowered by one. <code>s</code> lives outside that variable's binder
    /// and is shifted below the binders it is moved under.
    public Term apply(Term t) {
        return apply(t, 0);
    }

    private Term apply(Term t, int depth) {
        if (t.op() instanceof LogicVariable lv) {
            final int idx = lv.getIndex();
            if (idx == target + depth) {
                return new Shift(depth + target - 1, tb).apply(s);
            } else if (idx > target + depth) {
                return tb.var(LogicVariable.create(idx - 1, lv.sort()));
            }
            return t;
        }
        if (t.freeVars().isEmpty()) {
            return t;
        }
        final Term[] newSubterms = new Term[t.arity()];
        for (int i = 0; i < newSubterms.length; i++) {
            final int subDepth =
                t.op().bindVarsAt(i) ? depth + t.varsBoundHere(i).size() : depth;
            newSubterms[i] = apply(t.sub(i), subDepth);
        }
        return tb.tf().createTerm(t.op(), newSubterms, t.boundVars());
    }
}
