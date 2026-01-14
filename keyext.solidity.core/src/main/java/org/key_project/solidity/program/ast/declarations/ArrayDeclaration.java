/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

public class ArrayDeclaration extends Declaration {
    public final ProgramVariable programVariable;
    int length;

    public ArrayDeclaration(ProgramVariable programVariable, int length) {
        super(new ImmutableArray<>());
        this.programVariable = programVariable;
        this.length = length;
    }

    public ArrayDeclaration(ExtList children) {
        super(new ImmutableArray<>());
        this.programVariable = Objects.requireNonNull(children.removeFirstOccurrence(ProgramVariable.class));;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return switch (n){
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
        return programVariable.sort() + "[" + length + "]" + " memory " + programVariable.name();
    }

    public void visit(Visitor v) {
        v.performActionOnArrayDeclaration(this);
    }
}
