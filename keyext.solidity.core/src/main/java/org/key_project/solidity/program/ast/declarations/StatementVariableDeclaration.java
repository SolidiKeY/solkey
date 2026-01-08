/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;

import static org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation.Default;

//
public class StatementVariableDeclaration extends Declaration {
    private final int id;
    private final Type type;
    private final ProgramVariable programVariable;
    private String struct;
    private final DataLocation dataLocation;

    public StatementVariableDeclaration(int id, @NonNull Name name, Type type, String struct,
            DataLocation dataLocation) {
        super(name);
        this.id = id;
        this.type = type;
        this.struct = struct;
        this.dataLocation = dataLocation;

        Services services = new Services();
        final Sort uint = new SortImpl(new Name("uint"), false);
        KeYSolidityType uintKST = new KeYSolidityType(PrimitiveType.UINT, uint);
        services.getNamespaces().sorts().add(uint);

        this.programVariable = new ProgramVariable(name, uintKST);
    }

    public StatementVariableDeclaration(ExtList children) {
        super(new Name(""));
        this.id = 0;
        this.type = null;
        this.struct = null;
        this.dataLocation = null;

//        super(Objects.requireNonNull(children.removeFirstOccurrence(Name.class)));
//        this.id = Objects.requireNonNull(children.removeFirstOccurrence(int.class));
//        this.type = Objects.requireNonNull(children.removeFirstOccurrence(Type.class));
//        this.struct = Objects.requireNonNull(children.removeFirstOccurrence(String.class));
//        this.dataLocation =
//            Objects.requireNonNull(children.removeFirstOccurrence(DataLocation.class));

//        Services services = new Services();
//        final Sort uint = new SortImpl(new Name("uint"), false);
//        KeYSolidityType uintKST = new KeYSolidityType(PrimitiveType.UINT, uint);
//        services.getNamespaces().sorts().add(uint);

        this.programVariable = Objects.requireNonNull(children.removeFirstOccurrence(ProgramVariable.class));
    }

    @Override
    public SyntaxElement getChild(int n) {

        return switch (n) {
//            case 0 -> name;
//            case 1 -> id;
//            case 2 -> type;
//            case 3 -> struct;
//            case 4 -> dataLocation;
            case 0 -> programVariable;
            default -> throw new RuntimeException("Children does not exist");
        };
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    public String getStruct() {
        return struct;
    }

    @Override
    public String toString() {
        if (struct != null)
            return struct + " " + dataLocation + " " + name();
        if (dataLocation == Default)
            return type + " " + name();
        return type + " " + dataLocation + " " + name();
    }

    public void visit(Visitor v) {
        v.performActionOnStatementVariableDeclaration(this);
    }
}
