/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;


import java.util.List;

import org.key_project.solidity.program.ast.LeafProgramElement;
import org.key_project.solidity.program.ast.declarations.ModifierDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;


public class ModifierReference implements LeafProgramElement {

    public final String name;
    private final @Nullable ModifierDeclaration declaration;
    private final ImmutableArray<Expression> arguments;

    public ModifierReference(String name) {
        this(name, null, List.of());
    }

    public ModifierReference(String name, @Nullable ModifierDeclaration declaration,
            List<Expression> arguments) {
        this.name = name;
        this.declaration = declaration;
        this.arguments = new ImmutableArray<>(arguments);
    }

    public @Nullable ModifierDeclaration getDeclaration() {
        return declaration;
    }

    public ImmutableArray<Expression> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return name;
    }

    public void visit(Visitor v) {
        v.performActionOnModifierReference(this);
    }
}
