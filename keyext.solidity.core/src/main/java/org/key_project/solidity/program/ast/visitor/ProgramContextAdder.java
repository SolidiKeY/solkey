/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;


import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ext.ContextStatementBlock;
import org.key_project.solidity.rule.matching.inst.ContextBlockExpressionInstantiation;

public class ProgramContextAdder {
    /// singleton instance of the program context adder
    public final static ProgramContextAdder INSTANCE = new ProgramContextAdder();

    /// an empty private constructor to ensure the singleton property
    private ProgramContextAdder() {
    }

    public ContextStatementBlock start(SolidityProgramElement solidityProgramElement,
            ContextStatementBlock pe,
            ContextBlockExpressionInstantiation instantiation) {
        throw new RuntimeException("Not implemented yet");
    }
}
