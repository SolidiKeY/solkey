/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class IndexExpression extends SolidityExpression {

    @NonNull Expression leftExp;
    @NonNull Expression indexExp;

    public IndexExpression(Expression leftExp, Expression indexExp) {
        super(leftExp.getType());
        this.leftExp = leftExp;
        this.indexExp = indexExp;
    }

    public IndexExpression(ExtList children, Type type) {
        super(type);
        this.leftExp =
            Objects.requireNonNull(children.removeFirstOccurrence(ProgramVariable.class));
        this.indexExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> leftExp;
            case 1 -> indexExp;
            default -> throw new IndexOutOfBoundsException();
        };
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    public @NonNull Expression getLeftExp()  { return leftExp; }
    public @NonNull Expression getIndexExp() { return indexExp; }

    public String toString() {
        return leftExp + "[" + indexExp + "]";
    }

    public void visit(Visitor v) {
        v.performActionOnIndexExpression(this);
    }
}
