/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.references.TypeReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import java.util.Objects;

public class StateVariableDeclaration extends Declaration {

    private final @NonNull TypeReference typeReference;
    private final @Nullable Expression initializer;
    private final Visibility visibility;

    public StateVariableDeclaration(@NonNull Name name, @NonNull TypeReference type,
            Visibility visibility) {
        super(name);
        this.typeReference = type;
        this.initializer = null;
        this.visibility = visibility;
    }

    public StateVariableDeclaration(@NonNull Name name, @NonNull TypeReference typeReference,
            @Nullable Expression initializer, Visibility visibility) {
        super(name);
        this.typeReference = typeReference;
        this.initializer = initializer;
        this.visibility = visibility;
    }

    public StateVariableDeclaration(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Name.class)));
        this.typeReference = Objects.requireNonNull(children.removeFirstOccurrence(TypeReference.class));
        this.initializer = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.visibility = Objects.requireNonNull(children.removeFirstOccurrence(Visibility.class));
    }

    public @NonNull TypeReference getTypeReference() {
        return typeReference;
    }

    public @Nullable Expression getInitializer() {
        return initializer;
    }

    // Syntax Element interface
    @Override
    public int getChildCount() {
        return initializer == null ? 1 : 2;
    }

    @Override
    public SolidityProgramElement getChild(int i) {
        if (i < 0 || i >= getChildCount()) {
            throw new IndexOutOfBoundsException("No child at index " + i + " in " + name());
        }
        if (i == 0) {
            return typeReference;
        }
        return initializer;
    }


    // common interface
    public String toString() {
        return typeReference + " " + visibility + " " + name()
            + (initializer != null ? " = " + initializer : "") + ";";
    }

    public void visit(Visitor v){
        v.performActionOnStateVariableDeclaration(this);
    }
}
