/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.SourceData;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.solidity.rule.matching.inst.MatchConditions;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class UnaryExpression implements SolidityProgramElement, Expression, OperatorExpression {

    private final Operator operator;
    private final Expression exp;

    public UnaryExpression(Operator operator, Expression exp) {
        this.operator = operator;
        this.exp = exp;
    }

    public UnaryExpression(ExtList children) {
        this.operator = Objects.requireNonNull(children.removeFirstOccurrence(Operator.class));
        this.exp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public Type getType() {
        return exp.getType();
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        switch (n) {
            case 0:
                return operator;
            case 1:
                return exp;
            default:
                throw new IndexOutOfBoundsException(
                    "Index should be 0 <= " + n + " < " + getChildCount());
        }
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnUnaryExpression(this);
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression getExp() { return exp; }

    public String toString() {
        switch (operator) {
            case POST_INC, POST_DEC:
                return exp + operator.symbol();
            case DELETE:
                return operator.symbol() + " " + exp;
            case PRE_INC, PRE_DEC, LOGICAL_NOT, BITWISE_NOT, UNARY_MINUS:
                return operator.symbol() + exp;
            default:
                throw new IllegalStateException("Unknown unary operator: " + operator);
        }
    }

    @Override
    public @Nullable MatchConditions match(SourceData sourceData, @Nullable MatchConditions mc) {
        final var src = sourceData.getSource();

        if (src == null)
            return null;

        // Check class type
        if (!(src instanceof UnaryExpression that)) {
            return null;
        }

        // CRITICAL FIX: Check operator matches
        if (!this.operator.equals(that.operator)) {
            return null;
        }

        // Match children
        final SourceData newSource = new SourceData(src, 0, sourceData.getServices());
        mc = matchChildren(newSource, mc, 0);

        if (mc == null) {
            return null;
        }

        sourceData.next();
        return mc;
    }
}
