/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.HashCachingElement;
import org.key_project.solidity.program.ast.SolidityInfo;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.statement.CatchClause.Kind;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// At the moment TryStatement is not a ProgramPrefix as it works different to
/// Java and we may not want it to disappear in the prefix, this has to be checked
public class TryStatement extends HashCachingElement implements Statement {

    private final Expression expression;
    private final Statement body;
    private final ImmutableArray<@NonNull CatchClause> catchClauses;

    public TryStatement(Expression expression, Statement body,
            ImmutableArray<@NonNull CatchClause> clauses) {
        this.expression = expression;
        this.body = body;
        this.catchClauses = clauses;
    }

    public TryStatement(ExtList children) {
        this.expression = Objects.requireNonNull(children.get(Expression.class));
        this.body = Objects.requireNonNull(children.get(Statement.class));
        this.catchClauses = new ImmutableArray<>(children.collect(CatchClause.class));
    }

    public static TryStatement of(Expression call, List<ProgramVariable> returns, Statement body,
            List<CatchClause> clauses) {
        Map<Kind, Statement> handlers = new EnumMap<>(Kind.class);
        for (CatchClause clause : clauses) {
            if (handlers.put(clause.getKind(), clause.getBody()) != null) {
                throw new IllegalArgumentException(
                    "Duplicate catch clause of kind " + clause.getKind());
            }
        }
        Statement other = handlers.getOrDefault(Kind.Other, revertBlock());
        List<CatchClause> normalized = List.of(
            new CatchClause(Kind.Error, handlers.getOrDefault(Kind.Error, other)),
            new CatchClause(Kind.Panic, handlers.getOrDefault(Kind.Panic, other)),
            new CatchClause(Kind.Other, other));
        Statement success = CatchClause.declaring(
            returns.stream().map(StatementVariableDeclaration::new).toList(), body);
        return new TryStatement(call, success, new ImmutableArray<>(normalized));
    }

    private static Block revertBlock() {
        FunctionDeclaration revert = Objects.requireNonNull(
            SolidityInfo.getBuiltinFunctionDeclaration(new Name("revert")));
        FunctionReference reference = new FunctionReference(revert, PrimitiveType.VOID);
        return new Block(List.of(new ExpressionStatement(
            new FunctionCallExpression(PrimitiveType.VOID, reference, List.of()))));
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0)
            return expression;
        if (n == 1)
            return body;
        return catchClauses.get(n - 2);
    }

    @Override
    public int getChildCount() {
        return 2 + catchClauses.size();
    }

    public void visit(Visitor v) {
        v.performActionOnTryStatement(this);
    }

    public Expression getExpression() {
        return expression;
    }

    public Statement getBody() {
        return body;
    }

    public ImmutableArray<CatchClause> getCatchClauses() {
        return catchClauses;
    }

    public int getCatchClauseCount() {
        return catchClauses.size();
    }

    public CatchClause getCatchClause(int i) {
        return catchClauses.get(i);
    }

    public Statement getCatchBody(Kind kind) {
        return catchClauses.stream().filter(clause -> clause.getKind() == kind).findFirst()
                .orElseThrow().getBody();
    }

    @Override
    public int computeHashCode() {
        return Objects.hash(expression, body, catchClauses);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final TryStatement other = (TryStatement) obj;
        return Objects.equals(this.expression, other.expression) &&
                Objects.equals(this.body, other.body) &&
                Objects.equals(this.catchClauses, other.catchClauses);
    }

    @Override
    public String toString() {
        return "try " + expression + " " + body + " " +
            catchClauses.stream().map(CatchClause::toString).collect(Collectors.joining(" "));
    }
}
