/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.PosInProgram;
import org.key_project.solidity.program.ProgramPrefix;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class TryStatement implements Statement, ProgramPrefix {

    private final @NonNull Expression expression;
    private final @NonNull ImmutableArray<@NonNull ProgramVariable> returnDeclaration;
    private final @NonNull Block body;
    private final @NonNull ImmutableArray<@NonNull CatchClause> catchClauses;

    // cache hash
    private int hashcode = -1;

    public TryStatement(@NonNull Expression expression,
            @NonNull ImmutableArray<@NonNull ProgramVariable> returnDeclaration,
            @NonNull Block body,
            @NonNull ImmutableArray<@NonNull CatchClause> clauses) {
        this.expression = expression;
        this.returnDeclaration = returnDeclaration;
        this.body = body;
        this.catchClauses = clauses;
    }

    public TryStatement(ExtList children) {
        this.expression = Objects.requireNonNull(children.get(Expression.class));
        this.returnDeclaration = new ImmutableArray<>(children.collect(ProgramVariable.class));
        this.body = Objects.requireNonNull(children.get(Block.class));
        this.catchClauses = new ImmutableArray<>(children.collect(CatchClause.class));
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0)
            return expression;
        n -= 1;
        if (n >= 0 && n < returnDeclaration.size())
            return returnDeclaration.get(n);
        n -= returnDeclaration.size();
        if (n == 0)
            return body;
        n -= 1;
        return catchClauses.get(n);
    }

    @Override
    public int getChildCount() {
        int count = 1; // expression
        count += 1; // body
        count += returnDeclaration.size();
        count += catchClauses.size();
        return count;
    }

    public void visit(Visitor v) {
        v.performActionOnTryStatement(this);
    }

    public Expression getExpression() {
        return expression;
    }

    public Block getBody() {
        return body;
    }

    public ImmutableArray<ProgramVariable> getReturnDeclaration() {
        return returnDeclaration;
    }

    public int getReturnCount() {
        return returnDeclaration.size();
    }

    public ProgramVariable getReturnParameter(int i) {
        return returnDeclaration.get(i);
    }

    public @NonNull ImmutableArray<CatchClause> getCatchClauses() {
        return catchClauses;
    }

    public int getCatchClauseCount() {
        return catchClauses.size();
    }

    public @NonNull CatchClause getCatchClause(int i) {
        return catchClauses.get(i);
    }

    @Override
    public int hashCode() {
        if (hashcode == -1) {
            int hash = Objects.hash(expression, returnDeclaration, body, catchClauses);
            hashcode = hash == -1 ? 0 : hash;
        }
        return hashcode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final TryStatement other = (TryStatement) obj;
        return Objects.equals(this.expression, other.expression) &&
                Objects.equals(this.returnDeclaration, other.returnDeclaration) &&
                Objects.equals(this.body, other.body) &&
                Objects.equals(this.catchClauses, other.catchClauses);
    }

    @Override
    public String toString() {
        String params = returnDeclaration.stream().map(ProgramVariable::typeAndName)
                .collect(Collectors.joining(", "));
        String returnsString = returnDeclaration.size() == 0 ? "" : "returns (" + params + ") ";
        return "try " + expression + " " + returnsString + body + " " +
            catchClauses.stream().map(CatchClause::toString).collect(Collectors.joining());
    }

    @Override
    public boolean isPrefix() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean hasNextPrefixElement() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public ProgramPrefix getNextPrefixElement() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public ProgramPrefix getLastPrefixElement() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public ImmutableArray<ProgramPrefix> getPrefixElements() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public PosInProgram getFirstActiveChildPos() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public int getPrefixLength() {
        throw new RuntimeException("Not implemented yet");
    }
}
