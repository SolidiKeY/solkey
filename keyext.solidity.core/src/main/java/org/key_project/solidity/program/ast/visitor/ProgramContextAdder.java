package org.key_project.solidity.program.ast.visitor;

import org.key_project.solidity.program.ast.SolidityProgramElement;

public class ProgramContextAdder {
    /// singleton instance of the program context adder
    public final static ProgramContextAdder INSTANCE = new ProgramContextAdder();

    /// an empty private constructor to ensure the singleton property
    private ProgramContextAdder() {
    }
}
