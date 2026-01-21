/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.List;
import java.util.stream.Collectors;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

public class DeclarationStatement implements Statement {
    private final ImmutableArray<Declaration> declarations;
    private final Expression initialValue;

    public DeclarationStatement(List<Declaration> declarations, Expression initialValue) {
        this.declarations = new ImmutableArray<>(declarations);
        this.initialValue = initialValue;
    }

    public DeclarationStatement(ExtList children) {
        this.declarations = new ImmutableArray<>(children.collect(Declaration.class));
        this.initialValue = children.removeFirstOccurrence(Expression.class);
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n < 0 || n >= getChildCount()) {
            throw new IndexOutOfBoundsException(n + " out of bonds");
        }

        if (n < declarations.size()) {
            return declarations.get(n);
        }
        return initialValue;
    }

    @Override
    public int getChildCount() {
        return declarations.size() + (initialValue == null ? 0 : 1);
    }

    public ImmutableArray<Declaration> getDeclarations() {
        return declarations;
    }

    @Override
    public String toString() {
        String s = declarations.stream().map(Declaration::toString).collect(Collectors.joining(""));
        if (initialValue != null)
            s += " = " + initialValue.toString();
        return s + ";";
    }

    public void visit(Visitor v) {
        v.performActionOnDeclarationStatement(this);
    }
}
