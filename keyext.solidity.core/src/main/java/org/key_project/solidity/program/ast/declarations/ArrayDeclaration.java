/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

public class ArrayDeclaration extends DeclarationClass implements SolidityProgramElement {
    private final ProgramVariable programVariable;

    public ArrayDeclaration(ProgramVariable programVariable) {
        super(new ImmutableArray<>());
        this.programVariable = programVariable;
    }

    public ArrayDeclaration(ExtList children) {
        super(new ImmutableArray<>());
        this.programVariable =
            Objects.requireNonNull(children.removeFirstOccurrence(ProgramVariable.class));;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> programVariable;
            default -> throw new IndexOutOfBoundsException(n + " should be 0");
        };
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        Type type = programVariable.getType();
        String size = "";
        if (type instanceof ArrayType)
            size = String.valueOf(((ArrayType) type).getLength());
        return programVariable.sort() + "[" + size + "]" + " memory " + programVariable.name();
    }

    public void visit(Visitor v) {
        v.performActionOnArrayDeclaration(this);
    }

    public ProgramVariable getProgramVariable() {
        return programVariable;
    }

}
