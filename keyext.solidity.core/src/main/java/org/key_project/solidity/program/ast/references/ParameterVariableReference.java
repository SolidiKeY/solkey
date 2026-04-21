/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;

public class ParameterVariableReference extends SolidityExpression implements VariableReference {
    public final Name name;
    private final ProgramVariable referencedDeclaration;

    public ParameterVariableReference(Name name, ProgramVariable referencedDeclaration,
            Type type) {
        super(type);
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public ParameterVariableReference(ExtList children, Type type, Name name) {
        super(type);
        this.name = name;
        this.referencedDeclaration =
            Objects.requireNonNull(children.removeFirstOccurrence(ProgramVariable.class));
    }

    public ProgramVariable getDeclaration() {
        return referencedDeclaration;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public void visit(Visitor v) {
        v.performActionOnParameterVariableReference(this);
    }
}
