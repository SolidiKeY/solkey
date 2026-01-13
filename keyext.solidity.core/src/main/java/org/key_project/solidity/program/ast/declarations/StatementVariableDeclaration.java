/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.key_project.util.collection.ImmutableArray;

import static org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation.Default;

//
public class StatementVariableDeclaration extends Declaration {
    private final Type type;
    private final @NonNull Name name;
    private String struct;
    private final DataLocation dataLocation;

    public StatementVariableDeclaration(@NonNull Name name, Type type, String struct,
            DataLocation dataLocation) {
        super(new ImmutableArray<>());
        this.name = name;
        this.type = type;
        this.struct = struct;
        this.dataLocation = dataLocation;
    }

    public StatementVariableDeclaration(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(ImmutableArray.class)));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
        this.type = Objects.requireNonNull(children.removeFirstOccurrence(Type.class));
        this.struct = Objects.requireNonNull(children.removeFirstOccurrence(String.class));
        this.dataLocation =
            Objects.requireNonNull(children.removeFirstOccurrence(DataLocation.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public String getStruct() {
        return struct;
    }

    @Override
    public String toString() {
        if (struct != null)
            return struct + " " + dataLocation + " " + name;
        if (dataLocation == Default)
            return type + " " + name;
        return type + " " + dataLocation + " " + name;
    }

    public void visit(Visitor v) {
        v.performActionOnStatementVariableDeclaration(this);
    }
}
