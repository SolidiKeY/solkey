/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.jspecify.annotations.Nullable;

/// Variable condition `\sameAsTerm(v, s)` tying a program schema variable `v` to a term schema
/// variable `s`: it holds iff the program element `v` is instantiated with converts to a logic
/// term equal to `s`.
///
/// This is the bridge for the rule that program schema variables may only occur *inside a
/// modality* in `\find` / `\assumes`. To also refer to such a variable's value as a term outside
/// the modality, declare a `\term` schema variable `s`, use it in the term position, and add
/// `\varcond(\sameAsTerm(v, s))`. The condition then
///
/// * instantiates `s` with the conversion of `v` if `s` is not yet bound, or
/// * checks that the existing instantiation of `s` equals the conversion of `v`.
///
/// The conversion is the same lazy, by-name resolution used everywhere else
/// ([Services#convertToLogicElement]).
public final class SameAsTermCondition implements VariableCondition {
    private final ProgramSV programSV;
    private final SchemaVariable termSV;

    public SameAsTermCondition(ProgramSV programSV, SchemaVariable termSV) {
        this.programSV = programSV;
        this.termSV = termSV;
    }

    @Override
    @Nullable
    public MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != programSV && var != termSV) {
            return matchCond;
        }
        final Services services = (Services) lServices;
        final SVInstantiations inst = (SVInstantiations) matchCond.getInstantiations();

        // resolve the program element the program schema variable stands for
        final Object progInst = var == programSV ? svSubst : inst.getInstantiation(programSV);
        if (progInst == null) {
            // the program variable is not matched yet; defer until its own instantiation arrives
            return matchCond;
        }
        if (!(progInst instanceof SolidityProgramElement pe)) {
            return null;
        }
        final Term value;
        try {
            value = Services.convertToLogicElement(pe, services);
        } catch (RuntimeException e) {
            // the program element has no logic conversion -> the condition cannot hold
            return null;
        }

        final Object termInst = var == termSV ? svSubst : inst.getInstantiation(termSV);
        if (termInst == null) {
            // s is still free: bind it to the converted value
            return matchCond.setInstantiations(inst.add(termSV, value, lServices));
        }
        if (!(termInst instanceof Term t)) {
            return null;
        }
        return value.equals(t) ? matchCond : null;
    }

    @Override
    public String toString() {
        return "\\sameAsTerm(" + programSV.name() + ", " + termSV.name() + ")";
    }
}
