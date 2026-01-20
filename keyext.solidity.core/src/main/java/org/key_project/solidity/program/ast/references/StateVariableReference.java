/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class StateVariableReference extends Expression implements VariableReference {

    // private ReferencePrefix prefix; // a reference prefix for account.person.age here
    // account.person is the prefix
    public final Name name;
    private final StateVariableDeclaration referencedDeclaration;

    public StateVariableReference(Name name, StateVariableDeclaration referencedDeclaration,
            Type type) {
        super(type);
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public StateVariableReference(ExtList children, Type type, Name name) {
        super(type);
        this.name = name;
        this.referencedDeclaration =
            Objects.requireNonNull(children.removeFirstOccurrence(StateVariableDeclaration.class));
    }

    public StateVariableDeclaration getDeclaration() {
        return referencedDeclaration;
    }

    public Name name() {
        return name;
    }

    @Override
    public String toString() {
        return name.toString();
    }


    @Override
    public SyntaxElement getChild(int n) {
        if(n == 0)
            return referencedDeclaration;
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    public void visit(Visitor v) {
        v.performActionOnStateVariableReference(this);
    }
}
