/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.HashMap;
import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MemberExp extends SolidityExpression implements Resolver {
    final Expression leftExp;
    @Nullable
    SyntaxElement rightExp;
    final int id;

    public MemberExp(Expression leftExp, SyntaxElement rightExp, Type type) {
        super(type);
        this.leftExp = leftExp;
        this.rightExp = rightExp;
        this.id = -1;
    }

    public MemberExp(Expression leftExp, int id, Type type) {
        super(type);
        this.leftExp = leftExp;
        this.id = id;
    }

    public MemberExp(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.leftExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.rightExp = Objects.requireNonNull(children.removeFirstOccurrence(SyntaxElement.class));
        this.id = -1;
    }

    public Expression getLeftExp() { return leftExp; }

    public @Nullable SyntaxElement getRightExp() {
        return rightExp;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return leftExp;
        if (n == 1)
            return Objects.requireNonNull(rightExp);
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 1 + (rightExp == null ? 0 : 1);
    }

    @Override
    public String toString() {
        if (rightExp instanceof FunctionDeclaration)
            return leftExp + "." + ((FunctionDeclaration) rightExp).name() + "()";
        return leftExp + "." + rightExp;
    }

    public void visit(Visitor v) {
        v.performActionOnMemberExp(this);
    }

    @Override
    public void resolve(HashMap<Integer, SyntaxElement> id2Name) {
        if (id != -1)
            rightExp = id2Name.get(id);
    }
}
