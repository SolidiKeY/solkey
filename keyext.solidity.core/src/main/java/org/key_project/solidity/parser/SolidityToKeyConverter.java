/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import java.math.BigInteger;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.TernaryOperator;
import org.key_project.solidity.program.ast.ghost.ExpressionList;
import org.key_project.solidity.program.ast.ghost.FunctionCallArguments;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.program.parser.ParserUtils;
import org.key_project.util.collection.ImmutableArray;

import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.UINT256;

public class SolidityToKeyConverter extends SolidityBaseVisitor<SyntaxElement> {
    // ProgramSV for schema variable and it is declared outside of the program
    private Namespace<ProgramVariable> localVars = new Namespace<>();
    // TODO: Add constructor to the service
    private Services services;

    public SolidityToKeyConverter(){
        this.services = new Services();
    }

    public SolidityToKeyConverter(Services services){
        this.services = services;
    }

    @Override
    public SyntaxElement visitBlock(BlockContext ctx) {
        localVars = new Namespace<>(localVars);
        List<Statement> stms = ctx.statement().stream()
                .map(this::visitStatement).map(Statement.class::cast).toList();
        localVars = localVars.parent();
        return new Block(stms);
    }

    @Override
    public SyntaxElement visitNumberLiteral(NumberLiteralContext ctx) {
        if (ctx.DecimalNumber() != null) {
            BigInteger number = new BigInteger(ctx.DecimalNumber().getText());
            return new Uint256Literal(number);
        }
        return visitChildren(ctx);
    }

    @Override
    public SyntaxElement visitPrimaryExpression(PrimaryExpressionContext ctx) {
        if (ctx.BooleanLiteral() != null) {
            boolean b = Boolean.parseBoolean(ctx.BooleanLiteral().getText());
            return new BoolLiteral(b);
        }
        return visitChildren(ctx);
    }

    @Override
    public SyntaxElement visitTupleExpression(TupleExpressionContext ctx) {
        return new TupleExpression(UINT256, parseExps(ctx.expression()));
    }

    List<Expression> parseExps(List<ExpressionContext> exps) {
        return exps.stream().map(this::visitExpression).toList();
    }

    @Override
    public SyntaxElement visitBinaryOp(BinaryOpContext ctx) {
        List<Expression> exps = parseExps(ctx.expression());
        String operator = ctx.children.get(1).toString();
        Expression left = exps.get(0);
        Expression right = exps.get(1);
        return ParserUtils.parseAllBinary(left, right, operator, UINT256);
    }

    public Expression visitExpression(ExpressionContext ctx) {
        return ctx == null ? null : (Expression) ctx.accept(this);
    }

    @Override
    public SyntaxElement visitUnaryPrefix(UnaryPrefixContext ctx) {
        String operator = ctx.children.getFirst().toString();
        Expression uExp = visitExpression(ctx.expression());
        return ParserUtils.parseUnaryOperation(uExp, operator, UINT256, true);
    }

    @Override
    public SyntaxElement visitPostfix(PostfixContext ctx) {
        String operator = ctx.children.get(1).toString();
        Expression uExp = visitExpression(ctx.expression());
        return ParserUtils.parseUnaryOperation(uExp, operator, UINT256, false);
    }

    @Override
    public SyntaxElement visitFunctionCallExp(FunctionCallExpContext ctx) {
        String nameS = visitExpression(ctx.expression()).toString();
        Name name = new Name(nameS);
        FunctionReference functionRef = new FunctionReference(0, name, UINT256);
        FunctionCallArguments args =
            (FunctionCallArguments) visitFunctionCallArguments(ctx.functionCallArguments());
        return new FunctionCallExpression(UINT256, functionRef, args.getArgs());
    }

    @Override
    public SyntaxElement visitIndexAccess(IndexAccessContext ctx) {
        List<Expression> exps = parseExps(ctx.expression());
        // TODO: this.a.b would not work
        String nameS = exps.getFirst().toString();
        Name name = new Name(nameS);
        ProgramVariable p = localVars.lookup(name);
        return new IndexExpression(p, exps.get(1), UINT256);
    }

    @Override
    public SyntaxElement visitSliceAccess(SliceAccessContext ctx) {
        Expression base = visitExpression(ctx.base);
        Expression start = visitExpression(ctx.start);
        Expression end = visitExpression(ctx.end);

        return new IndexRangeExpression(base, start, end, UINT256);
    }

    @Override
    public SyntaxElement visitTernary(TernaryContext ctx) {
        Expression condition = visitExpression(ctx.condition);
        Expression falseExp = visitExpression(ctx.false_);
        Expression trueExp = visitExpression(ctx.true_);

        return new TernaryOperator(UINT256, condition, falseExp, trueExp);
    }

    @Override
    public SyntaxElement visitExpressionStatement(ExpressionStatementContext ctx) {
        return new ExpressionStatement(visitExpression(ctx.expression()));
    }

    @Override
    public SyntaxElement visitIdentifier(IdentifierContext ctx) {
        String variableName = ctx.Identifier().getText();
        return localVars.lookup(variableName);
    }

    @Override
    public SyntaxElement visitVariableDeclarationStatement(
            VariableDeclarationStatementContext ctx) {
        // TODO: fix the type
        KeYSolidityType ksType = new KeYSolidityType(PrimitiveType.UINT, new SortImpl(new Name("UINT"))) ;//null; // services.getSolidityInfo().getKeYSolidityType("");
        ProgramVariable programVariable = new ProgramVariable(new Name(ctx.variableDeclaration().identifier().Identifier().getText()), ksType);
        localVars.add(programVariable);
        StatementVariableDeclaration stmDecl =
            new StatementVariableDeclaration(programVariable, "", null);
        return new DeclarationStatement(List.of(stmDecl), null);
    }

    @Override
    public SyntaxElement visitIfStatement(IfStatementContext ctx) {
        Expression condition = visitExpression(ctx.expression());
        Statement trueBody = (Statement) visitStatement(ctx.ifStm);
        Statement elseBody = (Statement) visitStatement(ctx.elseStm);
        return new ConditionStatement(condition, trueBody, elseBody);
    }

    @Override
    public SyntaxElement visitReturnStatement(ReturnStatementContext ctx) {
        Expression exp = visitExpression(ctx.expression());
        return new ReturnStatement(exp);
    }

    @Override
    public SyntaxElement visitContinueStatement(ContinueStatementContext ctx) {
        return new ContinueStatement();
    }

    @Override
    public SyntaxElement visitBreakStatement(BreakStatementContext ctx) {
        return new BreakStatement();
    }

    @Override
    public SyntaxElement visitWhileStatement(WhileStatementContext ctx) {
        Expression exp = visitExpression(ctx.expression());
        Statement stm = (Statement) visitStatement(ctx.statement());
        return new WhileStatement(exp, stm);
    }

    @Override
    public SyntaxElement visitDoWhileStatement(DoWhileStatementContext ctx) {
        Expression exp = visitExpression(ctx.expression());
        Statement stm = (Statement) visitStatement(ctx.statement());
        return new DoWhileStatement(exp, stm);
    }

    @Override
    public SyntaxElement visitForStatement(ForStatementContext ctx) {
        ForInit initial = ctx.simpleStatement() == null ? null
                : new ForInit(((ExpressionStatement) visitSimpleStatement(ctx.simpleStatement()))
                        .getExpression());
        Expression condition = ctx.expressionStatement() == null ? null
                : ((ExpressionStatement) visitExpressionStatement(ctx.expressionStatement()))
                        .getExpression();
        ForUpdate loopExp = new ForUpdate(visitExpression(ctx.expression()));
        Statement body = (Statement) visitStatement(ctx.statement());
        return new ForStatement(initial, condition, loopExp, body);

    }

    @Override
    public SyntaxElement visitCatchClause(CatchClauseContext ctx) {
        return new CatchClause(null, (Block) visitBlock(ctx.block()));
    }

    @Override
    public SyntaxElement visitTryStatement(TryStatementContext ctx) {
        Expression exp = visitExpression(ctx.expression());
        Block body = (Block) visitBlock(ctx.block());
        List<CatchClause> clauses = ctx.catchClause().stream().map(this::visitCatchClause).map(CatchClause.class::cast).toList();
        // TODO: add paramater declaration
        return new TryStatement(exp, new ImmutableArray<>(), body, new ImmutableArray<>(clauses));
    }

    @Override
    public SyntaxElement visitExpressionList(ExpressionListContext ctx) {
        List<Expression> expressions = ctx == null ? List.of() : parseExps(ctx.expression());
        return new ExpressionList(expressions);
    }

    @Override
    public SyntaxElement visitFunctionCallArguments(FunctionCallArgumentsContext ctx) {
        return new FunctionCallArguments(
            (ExpressionList) visitExpressionList(ctx.expressionList()));
    }

    @Override
    public SyntaxElement visitStatement(StatementContext ctx) {
        if (ctx == null)
            return null;
        return visitChildren(ctx);
    }
}
