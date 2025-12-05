/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import java.util.Objects;

public class MemberExp extends Expression {
    final Expression leftExp;
    final String rightName;

    public MemberExp(Expression leftExp, String name, Type type) {
        super(type);
        this.leftExp = leftExp;
        this.rightName = name;
    }

    public MemberExp(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.leftExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.rightName = Objects.requireNonNull(children.removeFirstOccurrence(String.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0)
            return leftExp;
        return null;
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        return leftExp.toString() + "." + rightName + "()";
    }

    public void visit(Visitor v){
        v.performActionOnMemberExp(this);
    }
}
