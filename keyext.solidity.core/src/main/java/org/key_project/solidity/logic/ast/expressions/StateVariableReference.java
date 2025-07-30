/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.expressions;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.TypeResolver;
import org.key_project.solidity.logic.ast.abstractions.Type;
import org.key_project.solidity.logic.ast.declarations.StateVariableDeclaration;

public class StateVariableReference implements Expression {

    private final Name name;
    private final StateVariableDeclaration referedDeclaration;
    private Type type;
    private Name typeName;

    public StateVariableReference(Name name, StateVariableDeclaration referedDeclaration,
            Type type) {
        this.name = name;
        this.referedDeclaration = referedDeclaration;
        this.type = type;
        this.typeName = type.getName();
    }

    public StateVariableReference(Name name, StateVariableDeclaration referedDeclaration,
            Name typeName) {
        this.name = name;
        this.referedDeclaration = referedDeclaration;
        this.typeName = type.getName();
    }

    public boolean isResolved() {
        return type != null;
    }

    public void resolve(TypeResolver resolver) {
        if (!isResolved()) {
            type = resolver.resolveTypeByName(typeName);
        }
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("State variable reference has no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }


    @Override
    public Type getType() {
        return type;
    }
}
