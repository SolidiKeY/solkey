/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.TypeResolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.util.ExtList;

import java.util.Objects;

public abstract class VariableReference extends Expression {
    protected Type type;
    protected final Name typeName;

    protected VariableReference(Type type) {
        super(type);
        this.type = type;
        this.typeName = type.name();
    }

    protected VariableReference(Name typeName, Type type) {
        super(type);
        this.typeName = typeName;
    }

    protected VariableReference(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.type = Objects.requireNonNull(children.removeFirstOccurrence(Type.class));
        this.typeName = type.name();
    }

    public boolean isResolved() {
        return type != null;
    }

    public void resolve(TypeResolver resolver) {
        if (!isResolved()) {
            type = resolver.resolveTypeByName(typeName);
        }
    }

    public abstract Name name();

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
