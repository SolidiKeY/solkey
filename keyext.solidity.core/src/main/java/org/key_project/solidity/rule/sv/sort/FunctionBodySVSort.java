/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.statement.FunctionBodyStatement;

/// Program schema variable sort matching a [FunctionBodyStatement], i.e. the placeholder
/// for a not-yet-inlined Solidity function body. Used by the `functionBodyExpand` taclet.
public class FunctionBodySVSort extends ProgramSVSort {

    public FunctionBodySVSort() {
        super(new Name("FunctionBody"));
    }

    @Override
    public boolean canStandFor(Term t) {
        return t.op() instanceof FunctionBodyStatement;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return pe instanceof FunctionBodyStatement;
    }
}
