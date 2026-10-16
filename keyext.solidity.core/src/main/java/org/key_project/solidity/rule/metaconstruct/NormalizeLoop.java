/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.ProgramSV;

public final class NormalizeLoop extends ProgramTransformer {
    public NormalizeLoop(ProgramSV body) {
        super(new Name("normalize_loop"), body);
    }

    @Override
    public SolidityProgramElement[] transform(SolidityProgramElement pe, Services services,
            SVInstantiations svInst) {
        return new SolidityProgramElement[] {
            LoopLowering.lower(new Block(List.of((Statement) pe)), services)
        };
    }
}
