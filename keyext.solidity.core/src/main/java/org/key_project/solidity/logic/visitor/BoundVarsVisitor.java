/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.visitor;

import java.util.LinkedHashSet;
import java.util.Set;

import org.key_project.logic.Term;
import org.key_project.logic.Visitor;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

public class BoundVarsVisitor implements Visitor<Term> {
    private Set<QuantifiableVariable> bdVars = new LinkedHashSet<>();

    @Override
    public void visit(Term visited) {
        for (int i = 0, ar = visited.arity(); i < ar; i++) {
            for (int j = 0,
                    boundVarsSize = visited.varsBoundHere(i).size(); j < boundVarsSize; j++) {
                bdVars.add(visited.varsBoundHere(i).get(j));
            }
        }
    }

    /// visits a sequent
    public void visit(Sequent visited) {
        for (final SequentFormula sf : visited) {
            visit(sf.formula());
        }
    }

    /// returns all the bound variables that have been stored
    public ImmutableSet<QuantifiableVariable> getBoundVariables() {
        return DefaultImmutableSet.fromSet(bdVars);
    }

}
