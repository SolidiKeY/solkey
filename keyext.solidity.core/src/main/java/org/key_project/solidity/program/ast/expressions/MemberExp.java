/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.HashMap;
import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class MemberExp extends SolidityExpression {
    final Expression leftExp;
    SyntaxElement rightExp;

    public MemberExp(Expression leftExp, SyntaxElement rightExp, Type type) {
        super(type);
        this.leftExp = leftExp;
        this.rightExp = rightExp;
    }

    public MemberExp(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.leftExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.rightExp = Objects.requireNonNull(children.removeFirstOccurrence(SyntaxElement.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        if(n == 0)
            return leftExp;
        throw new IndexOutOfBoundsException(n + " is out of bonds");
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        return leftExp + "." + rightExp + "()";
    }

    public void visit(Visitor v) {
        v.performActionOnMemberExp(this);
    }
}
