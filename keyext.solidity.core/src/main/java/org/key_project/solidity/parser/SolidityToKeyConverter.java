/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import java.math.BigInteger;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.ParameterDeclaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.TernaryOperator;
import org.key_project.solidity.program.ast.ghost.ExpressionList;
import org.key_project.solidity.program.ast.ghost.FunctionCallArguments;
import org.key_project.solidity.program.ast.ghost.SyntaxElementList;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.program.parser.ParserUtils;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.util.collection.ImmutableArray;

import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.UINT256;

public class SolidityToKeyConverter extends SolidityBaseVisitor<SyntaxElement> {
    private Namespace<ProgramVariable> localVars;
    final private Namespace<ProgramSV> schemaVariables;
    final private Services services;

    public SolidityToKeyConverter() {
        this.services = new Services();
        this.localVars = new Namespace<>();
        this.schemaVariables = new Namespace<>();
    }

    public SolidityToKeyConverter(Services services, Namespace<ProgramVariable> localVars,
            Namespace<ProgramSV> schemaVariables) {
        this.services = services;
        this.localVars = localVars;
        this.schemaVariables = schemaVariables;
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
    public SyntaxElement visitSchemaVariable(SchemaVariableContext ctx) {
        String variableName = ctx.getText();
        return schemaVariables.lookup(variableName);
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
        Expression left = visitExpression(ctx.left);
        Expression index = visitExpression(ctx.index);
        return new IndexExpression(left, index, UINT256);
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
    public SyntaxElement visitNewInstance(NewInstanceContext ctx) {
        KeYSolidityType func = (KeYSolidityType) visitTypeName(ctx.typeName());
        return new NewExpression(func.getSort().toString(), func);
    }

    @Override
    public SyntaxElement visitExpressionStatement(ExpressionStatementContext ctx) {
        return new ExpressionStatement(visitExpression(ctx.expression()));
    }

    @Override
    public SyntaxElement visitIdentifier(IdentifierContext ctx) {
        String variableName = ctx.Identifier().getText();
        ProgramVariable res = localVars.lookup(variableName);
        if (res == null)
            throw new RuntimeException("Variable " + variableName + " out of the scope");
        return res;
    }

    @Override
    public SyntaxElement visitTypeName(TypeNameContext ctx) {
        Type type = services.getSolidityInfo().getType(new Name(ctx.getText()));
        final Sort sort = type.getSort(services);
        return new KeYSolidityType(type, sort);
    }

    @Override
    public SyntaxElement visitVariableDeclaration(VariableDeclarationContext ctx) {
        return visitVariableDeclarationWithInitialValue(ctx, null);
    }

    public SyntaxElement visitVariableDeclarationWithInitialValue(VariableDeclarationContext ctx,
            Expression initial) {
        KeYSolidityType ksType = (KeYSolidityType) visitTypeName(ctx.typeName());
        ProgramVariable programVariable =
            new ProgramVariable(new Name(ctx.identifier().Identifier().getText()), ksType, null);
        localVars.add(programVariable);
        StatementVariableDeclaration stmDecl =
            new StatementVariableDeclaration(programVariable, DataLocation.Storage);
        return new DeclarationStatement(List.of(stmDecl), initial);
    }

    @Override
    public SyntaxElement visitSingleVarDeclStatement(SingleVarDeclStatementContext ctx) {
        return visitVariableDeclarationWithInitialValue(ctx.variableDeclaration(),
            visitExpression(ctx.expression()));
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
        ForUpdate loopExp =
            ctx.expression() == null ? null : new ForUpdate(visitExpression(ctx.expression()));
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
        List<CatchClause> clauses = ctx.catchClause().stream().map(this::visitCatchClause)
                .map(CatchClause.class::cast).toList();
        List<ParameterDeclaration> parameters = ctx.returnParameters() == null ? List.of()
                : ((SyntaxElementList) visitReturnParameters(ctx.returnParameters()))
                        .getElements().stream().map(ParameterDeclaration.class::cast).toList();
        return new TryStatement(exp, new ImmutableArray<>(parameters), body,
            new ImmutableArray<>(clauses));
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

    @Override
    public SyntaxElement visitParameter(ParameterContext ctx) {
        return new ParameterDeclaration(null, null, null);
    }

    @Override
    public SyntaxElement visitParameterList(ParameterListContext ctx) {
        return new SyntaxElementList(ctx.parameter().stream().map(this::visitParameter).toList());
    }

    @Override
    public SyntaxElement visitElementaryTypeName(ElementaryTypeNameContext ctx) {
        return services.getSolidityInfo().getKeYSolidityType(ctx.getText());
    }
}
