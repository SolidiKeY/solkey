/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.matching.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.SourceData;
import org.key_project.solidity.rule.matching.inst.MatchConditions;

public class MatchProgramInstruction implements MatchInstruction {
    private final SolidityProgramElement pe;

    public MatchProgramInstruction(SolidityProgramElement pe) {
        this.pe = pe;
    }

    @Override
    public MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo matchConditions,
            LogicServices services) {
        final var sb = (SolidityBlock) actualElement;
        final MatchResultInfo result = pe.match(
            new SourceData(sb.program(), -1, (Services) services),
            (MatchConditions) matchConditions);
        return result;
    }
}
