/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;

public class StateVariableReference extends VariableReference {

    private int id;
    // private ReferencePrefix prefix; // a reference prefix for account.person.age here
    // account.person is the prefix
    private final Name name;
    private final StateVariableDeclaration referencedDeclaration;

    public StateVariableReference(int id, Name name, StateVariableDeclaration referencedDeclaration,
            Type type) {
        super(type);
        this.id = id;
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public StateVariableReference(Name name, StateVariableDeclaration referencedDeclaration,
            Name typeName, Type type) {
        super(type);
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public StateVariableDeclaration getDeclaration() {
        return referencedDeclaration;
    }

    public Name name() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return name.toString();
    }

}
