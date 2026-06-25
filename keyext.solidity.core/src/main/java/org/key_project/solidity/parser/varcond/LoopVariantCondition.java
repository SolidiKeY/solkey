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
import org.key_project.solidity.program.ast.statement.LoopStatement;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.ProgramSV;

/// Extracts the variant for a loop term.
///
/// @author Dominic Steinhoefel
public class LoopVariantCondition implements VariableCondition {
    private final SchemaVariable loopStatementSV;
    private final SchemaVariable variantSV;

    public LoopVariantCondition(ProgramSV loopStatementSV, SchemaVariable variantSV) {
        this.loopStatementSV = loopStatementSV;
        this.variantSV = variantSV;
    }

    @Override
    public MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo matchCond, LogicServices lServices) {
        final var services = (Services) lServices;
        final var svInst = (SVInstantiations) matchCond.getInstantiations();

        if (svInst.getInstantiation(variantSV) != null) {
            return matchCond;
        }

        final var loop = (LoopStatement) svInst.getInstantiation(loopStatementSV);
        final var loopSpec = services.getSpecificationRepository().getLoopSpec(loop);

        if (loopSpec == null) {
            return null;
        }
        final Term variant = loopSpec.getVariant(services);

        if (variant == null) {
            return null;
        }

        return matchCond.setInstantiations(svInst.add(variantSV, variant, services));
    }

    @Override
    public String toString() {
        return "\\getVariant(" + loopStatementSV.name() + ", " + variantSV.name() + ")";
    }
}
