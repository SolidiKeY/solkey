/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;


import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.util.collection.ImmutableArray;

public class ParameterDeclaration extends Declaration {
    private final ProgramVariable programVariable;

    public ParameterDeclaration(ProgramVariable programVariable) {
        super(new ImmutableArray<>());
        this.programVariable = programVariable;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return programVariable.getChild(n);
    }

    @Override
    public int getChildCount() {
        return programVariable.getChildCount();
    }

    @Override
    public String toString() {
        return programVariable.toString();
    }
}
