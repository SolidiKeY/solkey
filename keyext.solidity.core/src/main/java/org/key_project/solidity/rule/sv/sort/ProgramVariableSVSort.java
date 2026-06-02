package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;

public class ProgramVariableSVSort extends ProgramSVSort {

    protected ProgramVariableSVSort(Name name) {
        super(name);
    }

    @Override
    public boolean canStandFor(Term t) {
        return t.op() instanceof ProgramVariable;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return pe instanceof ProgramVariable;
    }
}
