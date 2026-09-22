/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.HashMap;
import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.Nullable;

public class MemberExp extends SolidityExpression implements Resolver {
    private final Expression leftExp;
    private @Nullable SyntaxElement rightExp;
    private final int id;

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
        super(typeFrom(children));
        this.leftExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.rightExp = Objects.requireNonNull(children.removeFirstOccurrence(SyntaxElement.class));
        this.id = -1;
    }

    private static Type typeFrom(ExtList children) {
        Type t = children.removeFirstOccurrence(Type.class);
        if (t != null) {
            return t;
        }
        for (Object child : children) {
            if (child instanceof FieldDeclaration fd) {
                Type referencedType = fd.getTypeReference().getReferencedType();
                if (referencedType != null) {
                    return referencedType;
                }
            }
        }
        // Final fallback: pull the type off the receiver expression.
        for (Object child : children) {
            if (child instanceof Expression expr && expr.getType() != null) {
                return expr.getType();
            }
        }
        throw new NullPointerException(
            "MemberExp(ExtList) requires a non-null Type in the change list");
    }

    /// A member access denotes the *member*, so its type is the member's declared type. That
    /// type is often still unresolved while the enclosing function body is parsed, so it is read
    /// through here rather than captured at construction; the constructor argument remains the
    /// fallback for builtins and unresolved members.
    @Override
    public Type getType() {
        if (rightExp instanceof FieldDeclaration fd) {
            Type referencedType = fd.getTypeReference().getReferencedType();
            if (referencedType != null) {
                return referencedType;
            }
        }
        return type;
    }

    public Expression getLeftExp() { return leftExp; }

    public @Nullable SyntaxElement getRightExp() {
        return rightExp;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0)
            return leftExp;
        if (n == 1 && rightExp != null)
            return rightExp;
        throw outOfBounds(n);
    }

    @Override
    public int getChildCount() {
        return 1 + (rightExp == null ? 0 : 1);
    }

    @Override
    public String toString() {
        if (rightExp instanceof FunctionDeclaration)
            return leftExp + "." + ((FunctionDeclaration) rightExp).name();
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
