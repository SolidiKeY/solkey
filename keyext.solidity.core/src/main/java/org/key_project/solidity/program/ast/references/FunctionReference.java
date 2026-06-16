/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.HashMap;
import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class FunctionReference extends SolidityExpression implements Resolver, VariableReference {

    public final int id;
    public @Nullable FunctionDeclaration referencedDeclaration;

    public FunctionReference(int id, Type type) {
        super(type);
        this.id = id;
        this.referencedDeclaration = null;
    }

    public FunctionReference(FunctionDeclaration referencedDeclaration, Type type) {
        super(type);
        this.referencedDeclaration = referencedDeclaration;
        this.id = -1;
    }

    @Override
    public String toString() {
        return referencedDeclaration == null ? "<unresolved function>"
                : referencedDeclaration.name().toString();
    }

    @Override
    public void resolve(HashMap<Integer, SyntaxElement> id2Name) {
        if (this.referencedDeclaration == null)
            this.referencedDeclaration =
                Objects.requireNonNull((FunctionDeclaration) id2Name.get(id));
        else
            throw new IllegalStateException(
                "function " + referencedDeclaration.name() + " has already been resolved");
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public FunctionDeclaration mainProgramElement() {
        return Objects.requireNonNull(referencedDeclaration, "function reference is not resolved");
    }

    public void visit(Visitor v) {
        v.performActionOnFunctionReference(this);
    }
}
