/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.matching.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.prover.rules.instantiation.IllegalInstantiationException;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;

import org.jspecify.annotations.Nullable;

import static org.key_project.solidity.common.Services.convertToLogicElement;

public class MatchProgramSVInstruction extends MatchSchemaVariableInstruction {

    public MatchProgramSVInstruction(ProgramSV sv) {
        super(sv);
    }


    /// {@inheritDoc}
    @Override
    public MatchResultInfo match(
            SolidityProgramElement instantiationCandidate,
            MatchResultInfo matchCond,
            LogicServices services) {
        final ProgramSVSort svSort = (ProgramSVSort) op.sort();

        // TODO: will need execution context when we add functions (in programs)
        if (svSort.canStandFor(instantiationCandidate, (Services) services)) {
            return addInstantiation(instantiationCandidate, matchCond, (Services) services);
        }

        return null;
    }

    /// tries to add the pair <tt>(this,pe)</tt> to the match conditions. If possible the resulting
    /// match conditions are returned, otherwise <tt>null</tt>. Such an addition can fail, e.g. if
    /// already a pair <tt>(this,x)</tt> exists where <tt>x!=pe</tt>
    private MatchResultInfo addInstantiation(SolidityProgramElement pe, MatchResultInfo matchCond,
            Services services) {

        final SVInstantiations instantiations =
            (SVInstantiations) matchCond.getInstantiations();
        final Object inMap = instantiations.getInstantiation(op);

        if (inMap == null) {
            try {
                return matchCond.setInstantiations(instantiations.add(op, pe, services));
            } catch (IllegalInstantiationException e) {

            }
        } else {
            Object peForCompare = pe;
            if (inMap instanceof Term) {
                try {
                    peForCompare = convertToLogicElement(pe, services);
                } catch (RuntimeException re) {
                    return null;
                }
            }
            if (inMap.equals(peForCompare)) {
                return matchCond;
            }
        }
        return null;
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo mc,
            LogicServices services) {
        if (actualElement instanceof SolidityProgramElement programElement) {
            return match(programElement, mc, services);
        }
        // A program schema variable may match a term only when its sort permits it, i.e. only
        // a program-*variable* sort (a program variable is itself a logic term, needed by the
        // update calculus, e.g. {pv := t}pv). All other program SV sorts return false here, so
        // they cannot occur in a term position of \find/\assumes; use a \term schema variable
        // with the \sameAsTerm variable condition instead.
        if (actualElement instanceof Term term) {
            final ProgramSVSort svSort = (ProgramSVSort) op.sort();
            if (svSort.canStandFor(term)) {
                return addInstantiation(term, mc, services);
            }
        }
        return null;
    }
}
