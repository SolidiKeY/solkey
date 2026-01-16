/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.IProgramVariable;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.references.VariableReference;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.key_project.util.collection.ImmutableArray;

import static org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation.Default;

//
public class StatementVariableDeclaration extends Declaration {
    public final ProgramVariable programVariable;
    private String struct;

    public StatementVariableDeclaration(ProgramVariable programVariable, String struct, DataLocation dataLocation) {
        super(new ImmutableArray<>(dataLocation));
        this.programVariable = programVariable;
        this.struct = struct;
    }

    public StatementVariableDeclaration(ExtList children) {
        super(children);
        this.programVariable = Objects.requireNonNull(children.removeFirstOccurrence(ProgramVariable.class));
        this.struct = null;
//        this.struct = Objects.requireNonNull(children.removeFirstOccurrence(String.class));
//        this.dataLocation =
//            Objects.requireNonNull(children.removeFirstOccurrence(DataLocation.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        if(n < 0 || n >= getChildCount())
            throw new IndexOutOfBoundsException(n + " out of bonds");
        if(n < modifiers.size())
            return modifiers.get(n);
        return programVariable;
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public String toString() {
        DataLocation dataLocation = (DataLocation) modifiers.get(0);
        String name = programVariable.name().toString();
        String type = programVariable.getType().toString();
        if (struct != null)
            return struct + " " + dataLocation + " " + name;
        if (dataLocation == Default)
            return type + " " + name;
        return type + " " + dataLocation + " " + name;
    }

    public void visit(Visitor v) {
        v.performActionOnStatementVariableDeclaration(this);
    }

    public IProgramVariable getProgramVariable() {
        return programVariable;
    }
}
