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

public final class AssignExpression
        implements SolidityProgramElement, Expression, OperatorExpression {

    private final @NonNull Operator operator;
    private final @NonNull Expression lhs;
    private final @NonNull Expression rhs;
    private int hashcode = -1;

    public AssignExpression(Operator operator, Expression lhs, Expression rhs) {
        this.operator = Objects.requireNonNull(operator);
        this.lhs = Objects.requireNonNull(lhs);
        this.rhs = Objects.requireNonNull(rhs);
    }

    public AssignExpression(ExtList children) {
        this.operator = Objects.requireNonNull(children.removeFirstOccurrence(Operator.class));
        this.lhs = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.rhs = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public Type getType() {
        return lhs.getType();
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> operator;
            case 1 -> lhs;
            case 2 -> rhs;
            default -> throw new IndexOutOfBoundsException(
                "Index should be 0 <= " + n + " < " + getChildCount());
        };
    }

    @Override
    public int getChildCount() {
        return 3;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnAssignExpression(this);
    }

    public @NonNull Expression getLeft() { return lhs; }

    public @NonNull Expression getRight() { return rhs; }

    @Override
    public Operator getOperator() {
        return operator;
    }

    @Override
    public String toString() {
        return lhs + " " + operator.symbol() + " " + rhs;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AssignExpression that))
            return false;
        return operator.equals(that.operator) && lhs.equals(that.lhs) && rhs.equals(that.rhs);
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
        if (!(src instanceof AssignExpression that)) {
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
