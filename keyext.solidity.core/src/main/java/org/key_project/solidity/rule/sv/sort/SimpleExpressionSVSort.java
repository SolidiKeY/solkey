/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.literals.Literal;

public class SimpleExpressionSVSort extends ProgramSVSort {

    public SimpleExpressionSVSort() {
        super(new Name("SimpleExpression"));
    }

    @Override
    public boolean canStandFor(Term t) {
        return t.op() instanceof ProgramVariable;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        // Literals (BoolLiteral, Uint256Literal, etc.)
        if (pe instanceof Literal)
            return true;

        // Program variables
        if (pe instanceof ProgramVariable)
            return true;

        return false;
    }
}
