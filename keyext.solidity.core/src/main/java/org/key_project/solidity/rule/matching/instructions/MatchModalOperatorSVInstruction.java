/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.matching.instructions;

import java.util.Set;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.ModalOperatorSV;

public class MatchModalOperatorSVInstruction implements MatchInstruction {
    private final Set<SModality.SolidityModalityKind> modalityKinds;
    private final ModalOperatorSV modalitySV;

    public MatchModalOperatorSVInstruction(ModalOperatorSV op) {
        this.modalitySV = op;
        this.modalityKinds = modalitySV.getModalities().toSet();
    }

    @Override
    public MatchResultInfo match(SyntaxElement actualElement,
            MatchResultInfo mc, LogicServices services) {
        if (actualElement instanceof SModality.SolidityModalityKind kind
                && modalityKinds.contains(kind)) {
            final SVInstantiations instantiations = (SVInstantiations) mc.getInstantiations();
            return mc.setInstantiations(instantiations.add(modalitySV, kind, services));
        } else {
            return null;
        }
    }
}
