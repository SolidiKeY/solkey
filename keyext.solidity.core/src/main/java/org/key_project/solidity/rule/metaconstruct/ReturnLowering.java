/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.TupleExpression;
import org.key_project.solidity.program.ast.expressions.operators.AssignExpression;
import org.key_project.solidity.program.ast.expressions.operators.Operator;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.ExpressionStatement;
import org.key_project.solidity.program.ast.statement.ReturnStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.ast.visitor.CreatingASTVisitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.Nullable;

final class ReturnLowering extends CreatingASTVisitor {

    private final List<ProgramVariable> returns;
    private final Set<ProgramVariable> returnSet =
        Collections.newSetFromMap(new IdentityHashMap<>());
    private @Nullable SolidityProgramElement result;

    private ReturnLowering(Block body, List<ProgramVariable> returns, Services services) {
        super(body, services);
        this.returns = returns;
        returnSet.addAll(returns);
    }

    static Block lower(Block body, List<ProgramVariable> returns, Services services) {
        ReturnLowering lowering = new ReturnLowering(body, returns, services);
        lowering.start();
        return (Block) Objects.requireNonNull(lowering.result);
    }

    @Override
    public void start() {
        stack.push(new ExtList());
        walk(root());
        for (Object element : getTop()) {
            if (element instanceof SolidityProgramElement pe) {
                result = pe;
                return;
            }
        }
    }

    @Override
    protected void walk(SolidityProgramElement node) {
        if (node instanceof Expression) {
            stack.push(new ExtList());
            doDefaultAction(node);
            return;
        }
        super.walk(node);
    }

    @Override
    public void performActionOnReturnStatement(ReturnStatement x) {
        if (x.getReturnExp() == null) {
            doDefaultAction(x);
            return;
        }
        List<Statement> statements = new ArrayList<>(assignments(x));
        statements.add(new ReturnStatement((Expression) null));
        addChild(new Block(statements));
        changed();
    }

    private List<Statement> assignments(ReturnStatement ret) {
        Expression value = Objects.requireNonNull(ret.getReturnExp());
        List<Expression> values = value instanceof TupleExpression tuple
                ? tuple.getExpressions().toList()
                : List.of(value);
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

    private static Statement assign(Expression target, Expression value) {
        return new ExpressionStatement(new AssignExpression(Operator.COPY_ASSIGN, target, value));
    }
}
