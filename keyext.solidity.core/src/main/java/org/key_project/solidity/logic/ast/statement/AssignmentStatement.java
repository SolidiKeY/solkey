/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.statement;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.expressions.Expression;

public class AssignmentStatement implements Statement {

    private Expression leftHandSide;
    private Expression rightHandSide;


    public Expression getLeftHandSide() {
        return leftHandSide;
    }

    public Expression getRightHandSide() {
        return rightHandSide;
    }

    @Override
    public SyntaxElement getChild(int n) {
        switch (n) {
            case 0:
                return leftHandSide;
            case 1:
                return rightHandSide;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public String toString() {
        return leftHandSide + " = " + rightHandSide + ";";
    }

}
