/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.TupleExpression;
import org.key_project.solidity.program.ast.expressions.operators.AssignExpression;
import org.key_project.solidity.program.ast.expressions.operators.Operator;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.ConditionStatement;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.ReturnStatement;
import org.key_project.solidity.program.ast.statement.Statement;

final class ReturnLowering {

    private final List<ProgramVariable> returns;
    private final Set<ProgramVariable> returnSet =
        Collections.newSetFromMap(new IdentityHashMap<>());

    private ReturnLowering(List<ProgramVariable> returns) {
        this.returns = returns;
        returnSet.addAll(returns);
    }

    static Block lower(Block body, List<ProgramVariable> returns) {
        return new Block(new ReturnLowering(returns).lower(body.getStatements().toList()));
    }

    private List<Statement> lower(List<Statement> statements) {
        List<Statement> lowered = new ArrayList<>(statements.size());
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            List<Statement> rest = statements.subList(i + 1, statements.size());
            if (!containsReturn(statement)) {
                lowered.add(statement);
                continue;
            }
            switch (statement) {
                case ReturnStatement ret -> lowered.addAll(assignments(ret));
                case Block block -> lowered
                        .add(new Block(lower(followedBy(block.getStatements().toList(), rest))));
                case ConditionStatement cond -> lowered.add(branches(cond, rest));
                default -> {
                    lowered.add(statement);
                    continue;
                }
            }
            return lowered;
        }
        return lowered;
    }

    private Statement branches(ConditionStatement cond, List<Statement> rest) {
        Block thenBlock = new Block(lower(followedBy(List.of(cond.getThenBody()), rest)));
        Statement elseBody = cond.getElseBody();
        List<Statement> elseStatements =
            lower(followedBy(elseBody == null ? List.of() : List.of(elseBody), rest));
        return elseStatements.isEmpty() ? new ConditionStatement(cond.getCondition(), thenBlock)
                : new ConditionStatement(cond.getCondition(), thenBlock, new Block(elseStatements));
    }

    private static List<Statement> followedBy(List<Statement> first, List<Statement> rest) {
        List<Statement> all = new ArrayList<>(first);
        all.addAll(rest);
        return all;
    }

    private List<Statement> assignments(ReturnStatement ret) {
        Expression value = ret.getReturnExp();
        List<Expression> values = value == null ? List.of()
                : value instanceof TupleExpression tuple ? tuple.getExpressions().toList()
                        : List.of(value);
        if (values.isEmpty()) {
            return List.of();
        }
        if (values.size() != returns.size()) {
            throw new IllegalStateException("return of " + values.size()
                + " values from a function with " + returns.size() + " return values");
        }
        List<Statement> statements = new ArrayList<>(2 * values.size());
        if (values.stream().noneMatch(this::readsReturn)) {
            for (int i = 0; i < values.size(); i++) {
                statements.add(assign(returns.get(i), values.get(i)));
            }
            return statements;
        }
        List<ProgramVariable> temps = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            ProgramVariable target = returns.get(i);
            ProgramVariable temp = new ProgramVariable(new Name(target.name() + "_ret"),
                target.getKeYSolidityType(), target.getDataLocation());
            temps.add(temp);
            statements.add(new DeclarationStatement(
                List.of(new StatementVariableDeclaration(temp)), values.get(i)));
        }
        for (int i = 0; i < values.size(); i++) {
            statements.add(assign(returns.get(i), temps.get(i)));
        }
        return statements;
    }

    private boolean readsReturn(SyntaxElement element) {
        if (element instanceof ProgramVariable pv && returnSet.contains(pv)) {
            return true;
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            if (readsReturn(element.getChild(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsReturn(SyntaxElement element) {
        if (element instanceof ReturnStatement) {
            return true;
        }
        if (!(element instanceof Statement)) {
            return false;
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            if (containsReturn(element.getChild(i))) {
                return true;
            }
        }
        return false;
    }

    private static Statement assign(Expression target, Expression value) {
        return new ExpressionStatement(new AssignExpression(Operator.COPY_ASSIGN, target, value));
    }
}
