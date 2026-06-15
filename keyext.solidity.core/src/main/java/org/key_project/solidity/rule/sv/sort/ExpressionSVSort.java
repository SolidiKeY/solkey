/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.Expression;

public class ExpressionSVSort extends ProgramSVSort {

    public ExpressionSVSort() {
        super(new Name("Expression"));
    }

    @Override
    public boolean canStandFor(Term t) {
        return t.op() instanceof Expression;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return pe instanceof Expression;
    }
}
