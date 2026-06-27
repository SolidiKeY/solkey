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

import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.BOOL;

public final class BinaryExpression
        implements SolidityProgramElement, Expression, OperatorExpression {

    protected final @NonNull Operator operator;
    protected final @NonNull Expression left;
    protected final @NonNull Expression right;
    private int hashcode = -1;

    public BinaryExpression(Operator operator, Expression left, Expression right) {
        this.operator = operator;
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
    }

    public BinaryExpression(ExtList children) {
        this.operator = Objects.requireNonNull(children.removeFirstOccurrence(Operator.class));
        this.left = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.right = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        switch (n) {
            case 0:
                return operator;
            case 1:
                return left;
            case 2:
                return right;
            default:
                throw new IndexOutOfBoundsException(
                    "Index should be 0 <= " + n + " < " + getChildCount());
        }
    }

    @Override
    public int getChildCount() {
        return 3;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnBinaryExpression(this);
    }

    public @NonNull Expression getLeft() { return left; }

    public @NonNull Expression getRight() { return right; }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public Type getType() {
        return switch (operator) {
            case EQUAL, NOT_EQUAL, LESS_THAN, GREATER_THAN, LESS_EQUAL, GREATER_EQUAL,
                    LOGICAL_AND, LOGICAL_OR ->
                BOOL;
            default -> left.getType();
        };
    }

    public String toString() {
        return left + " " + operator.symbol() + " " + right;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BinaryExpression that))
            return false;
        return operator.equals(that.operator) && left.equals(that.left)
                && right.equals(that.right);
    }

    @Override
    public int hashCode() {
        if (hashcode == -1) {
            int hash = computeHashCode();
            hashcode = hash == -1 ? 0 : hash;
        }
        return hashcode;
    }

    @Override
    public @Nullable MatchConditions match(SourceData sourceData, @Nullable MatchConditions mc) {
        final var src = sourceData.getSource();

        if (src == null)
            return null;

        // Check class type
        if (!(src instanceof BinaryExpression that)) {
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
