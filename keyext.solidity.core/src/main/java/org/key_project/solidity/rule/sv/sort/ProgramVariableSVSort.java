/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
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
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return pe instanceof ProgramVariable;
    }

    /// A program variable is itself a logic term, so it may match in term position (the update
    /// calculus relies on this, e.g. `{pv := t}pv`).
    @Override
    public boolean canStandFor(Term t) {
        return t.op() instanceof ProgramVariable;
    }

    @Override
    public boolean mayOccurInTermPosition() {
        return true;
    }
}
