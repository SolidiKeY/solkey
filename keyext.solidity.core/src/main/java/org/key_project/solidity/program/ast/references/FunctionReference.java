/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.HashMap;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class FunctionReference extends Expression implements Resolver, VariableReference {

    public final int id;
    public final Name name;
    public FunctionDeclaration referencedDeclaration;

    public FunctionReference(int id, Name name, FunctionDeclaration referencedDeclaration,
            Type type) {
        super(type);
        this.id = id;
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public FunctionReference(int id, Name name, Type type) {
        super(type);
        this.id = id;
        this.name = name;
        this.referencedDeclaration = null;
    }

    public FunctionReference(ExtList children, Type type, int id, Name name) {
        super(type);
        this.id = id;
        this.name = name;
        this.referencedDeclaration =
            Objects.requireNonNull(children.removeFirstOccurrence(FunctionDeclaration.class));
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public void resolve(HashMap<Integer, Declaration> id2Name) {
        this.referencedDeclaration = (FunctionDeclaration) id2Name.get(id);
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0 && getChildCount() == 1)
            return referencedDeclaration;
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return referencedDeclaration == null ? 0 : 1;
    }

    public void visit(Visitor v) {
        v.performActionOnFunctionReference(this);
    }
}
