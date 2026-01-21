/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class IndexExpression extends SolidityExpression {

    String leftExp;
    Expression indexExp;

    public IndexExpression(String leftExp, Expression indexExp, Type expType) {
        super(expType);
        this.leftExp = leftExp;
        this.indexExp = indexExp;
    }

    public IndexExpression(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.leftExp = Objects.requireNonNull(children.removeFirstOccurrence(String.class));
        this.indexExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public String toString() {
        return leftExp + "[" + indexExp + "]";
    }

    public void visit(Visitor v) {
        v.performActionOnIndexExpression(this);
    }
}
