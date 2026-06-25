/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.program.ast.statement.LoopStatement;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.speclang.LoopSpecification;

/// Checks whether a loop has an invariant.
///
/// @author Dominic Steinhoefel
public class HasLoopInvariantCondition implements VariableCondition {
    private final ProgramSV loopStatementSV;
    private final SchemaVariable modalitySV;

    public HasLoopInvariantCondition(ProgramSV loopExprSV, SchemaVariable modalitySV) {
        this.loopStatementSV = loopExprSV;
        this.modalitySV = modalitySV;
    }

    @Override
    public MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo matchCond, LogicServices lServices) {
        final var svInst = (SVInstantiations) matchCond.getInstantiations();
        final var services = (Services) lServices;

        final var loop = (LoopStatement) svInst.getInstantiation(loopStatementSV);
        final LoopSpecification loopSpec = services.getSpecificationRepository().getLoopSpec(loop);

        if (loopSpec == null) {
            return null;
        }

        final var sb = new SolidityBlock(svInst.getContextInstantiation().contextProgram());

        var modKind = (SModality.SolidityModalityKind) svInst.getInstantiation(modalitySV);

        return loopSpec.getInvariant(services) != null ? matchCond : null;
    }

    @Override
    public String toString() {
        return "\\hasInvariant(" + loopStatementSV.name() + ", " + modalitySV.name() + ")";
    }
}
