/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class TupleExpression extends SolidityExpression {
    List<Expression> expressions;

    public TupleExpression(Type type, List<Expression> expressions) {
        super(type);
        this.expressions = expressions;
    }

    public TupleExpression(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.expressions = Objects.requireNonNull(children.removeFirstOccurrence(List.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (0 <= n && n < expressions.size())
            return expressions.get(n);
        throw new IndexOutOfBoundsException("Not 0 <= " + n + " < " + expressions.size());
    }

    @Override
    public int getChildCount() {
        return expressions.size();
    }

    public Expression getExpression(int n) { return expressions.get(n); }

    @Override
    public String toString() {
        return "[" + expressions.stream().map(Objects::toString).collect(Collectors.joining(", "))
            + "]";
    }

    public void visit(Visitor v) {
        v.performActionOnTupleExpression(this);
    }
}
