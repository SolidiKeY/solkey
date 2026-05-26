/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;

import static org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation.Default;

//
public class StatementVariableDeclaration implements Declaration, SolidityProgramElement {
    private final ProgramVariable programVariable;

    public StatementVariableDeclaration(ProgramVariable programVariable) {
        this.programVariable = programVariable;
    }

    public StatementVariableDeclaration(ExtList extList) {
        this.programVariable =
            Objects.requireNonNull(extList.removeFirstOccurrence(ProgramVariable.class));
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return programVariable;
        throw new IndexOutOfBoundsException(
            "Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        DataLocation dataLocation = programVariable.getDataLocation();
        String name = programVariable.name().toString();
        String type = programVariable.getType().toString();
        if (dataLocation == Default)
            return type + " " + name;
        return type + " " + dataLocation + " " + name;
    }

    public void visit(Visitor v) {
        v.performActionOnStatementVariableDeclaration(this);
    }

    public ProgramVariable getProgramVariable() {
        return programVariable;
    }
}
