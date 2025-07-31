/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.references;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.TypeResolver;
import org.key_project.solidity.logic.ast.abstractions.Type;
import org.key_project.solidity.logic.ast.declarations.Declaration;
import org.key_project.solidity.logic.ast.expressions.Expression;

public abstract class VariableReference implements Expression {
    protected Type type;
    protected final Name typeName;

    protected VariableReference(Type type) {
        this.type = type;
        this.typeName = type.getName();
    }

    protected VariableReference(Name typeName) {
        this.typeName = typeName;
    }

    public boolean isResolved() {
        return type != null;
    }

    public void resolve(TypeResolver resolver) {
        if (!isResolved()) {
            type = resolver.resolveTypeByName(typeName);
        }
    }

    public abstract Name getName();

    public abstract Declaration getDeclaration();

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("A variable reference has no children");
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
