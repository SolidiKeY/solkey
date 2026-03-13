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
import org.key_project.util.collection.ImmutableArray;

import static org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation.Default;

//
public class StatementVariableDeclaration extends DeclarationClass implements SolidityProgramElement {
    private final ProgramVariable programVariable;

    public StatementVariableDeclaration(ProgramVariable programVariable) {
        super(new ImmutableArray<>());
        this.programVariable = programVariable;
    }

    public StatementVariableDeclaration(ProgramVariable programVariable,
            DataLocation dataLocation) {
        super(new ImmutableArray<>(dataLocation));
        this.programVariable = programVariable;
    }

    public StatementVariableDeclaration(ExtList children) {
        super(children);
        this.programVariable =
            Objects.requireNonNull(children.removeFirstOccurrence(ProgramVariable.class));
        // this.struct = Objects.requireNonNull(children.removeFirstOccurrence(String.class));
        // this.dataLocation =
        // Objects.requireNonNull(children.removeFirstOccurrence(DataLocation.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n < 0 || n >= getChildCount())
            throw new IndexOutOfBoundsException(n + " out of bonds");
        if (n < modifiers.size())
            return modifiers.get(n);
        return programVariable;
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public String toString() {
        DataLocation dataLocation = programVariable.getLocation();
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
