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
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.program.ast.statement.LoopStatement;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.speclang.LoopSpecification;

/// Extracts the loop invariants for a loop term (for all applicable heap contexts).
///
/// @author Dominic Steinhoefel
public class LoopInvariantCondition implements VariableCondition {
    private final ProgramSV loopStatementSV;
    private final SchemaVariable modalitySV;
    private final SchemaVariable invSV;

    public LoopInvariantCondition(ProgramSV loopExprSV, SchemaVariable modalitySV,
            SchemaVariable invSV) {
        this.loopStatementSV = loopExprSV;
        this.modalitySV = modalitySV;
        this.invSV = invSV;
    }

    @Override
    public MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo matchCond, LogicServices lServices) {
        final var services = (Services) lServices;
        final var svInst = (SVInstantiations) matchCond.getInstantiations();
        final var tb = services.getTermBuilder();

        final var loop = (LoopStatement) svInst.getInstantiation(loopStatementSV);
        LoopSpecification loopSpec = services.getSpecificationRepository().getLoopSpec(loop);

        if (loopSpec == null) {
            return null;
        }

        if (services.getProof().getInitConfig().getActivatedChoices().stream()
                .anyMatch(c -> c.name().toString().equals("intRules:soliditySemantics"))) {
            loopSpec = loopSpec.withInRangePredicates(services);
        }

        final var solidityBlock =
            new SolidityBlock(svInst.getContextInstantiation().contextProgram());

        var modKind = (SModality.SolidityModalityKind) svInst.getInstantiation(modalitySV);

        Term invInst = tb.tt();

        final var inst = loopSpec.getInvariant(services);
        if (inst != null) {
            invInst = tb.and(invInst, inst);
        }

        return matchCond.setInstantiations(svInst.add(invSV, invInst, services));
    }

    @Override
    public String toString() {
        return "\\getInvariant(" + loopStatementSV + ", " + modalitySV + ", " + invSV + ")";
    }
}
