/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import java.math.BigInteger;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.SolidityInfo;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.operators.TernaryOperator;
import org.key_project.solidity.program.ast.ghost.*;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.program.parser.ParserUtils;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.util.collection.ImmutableArray;

import static org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation.Default;

public class SolidityToKeyConverter extends SolidityBaseVisitor<SyntaxElement> {
    private Namespace<FunctionDeclaration> localFunctions;
    private Namespace<ProgramVariable> localVars;
    final private Namespace<? extends SchemaVariable> schemaVariables;
    final private Services services;

    public SolidityToKeyConverter() {
        this.services = new Services();
        this.localFunctions = new Namespace<>();
        this.localVars = new Namespace<>();
        this.schemaVariables = new Namespace<>();
    }

    public SolidityToKeyConverter(Services services, Namespace<FunctionDeclaration> localFunctions,
            Namespace<ProgramVariable> localVars,
            Namespace<? extends SchemaVariable> schemaVariables) {
        this.services = services;
        this.localFunctions = localFunctions;
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
        String variableName = ctx.getText().substring(2);
        // remove s# prefix from name
        SchemaVariable sv = schemaVariables.lookup(variableName);
        if (sv == null) {
            reportError("Schema Variable " + variableName + " not declared.", ctx.start);
        }
        return sv;
    }

    private void reportError(String errorMsg, Token tokenWithPos) throws PositionedConverterException {
        int line = tokenWithPos != null ? tokenWithPos.getLine() : -1;
        int column = tokenWithPos != null ? tokenWithPos.getCharPositionInLine() : -1;
        throw new PositionedConverterException(errorMsg, line, column);
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
        throw new RuntimeException("Not implemented yet");
//        List<Expression> exps = parseExps(ctx.expression());
//        TupleType tupleType = services.getSolidityInfo().getTupleTypeMap(
//            exps.stream().map(Expression::getType).toList());
//        return new TupleExpression(tupleType, exps);
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
        return ParserUtils.parseAllBinary(left, right, operator, left.getType());
    }

    public Expression visitExpression(ExpressionContext ctx) {
        return ctx == null ? null : (Expression) ctx.accept(this);
    }

    @Override
    public SyntaxElement visitUnaryPrefix(UnaryPrefixContext ctx) {
        String operator = ctx.children.getFirst().toString();
        Expression uExp = visitExpression(ctx.expression());
        return ParserUtils.parseUnaryOperation(uExp, operator, true);
    }

    @Override
    public SyntaxElement visitPostfix(PostfixContext ctx) {
        String operator = ctx.children.get(1).toString();
        Expression uExp = visitExpression(ctx.expression());
        return ParserUtils.parseUnaryOperation(uExp, operator, false);
    }

    @Override
    public SyntaxElement visitFunctionCallExp(FunctionCallExpContext ctx) {
        Expression functionExp = visitExpression(ctx.expression());
        FunctionCallArguments args =
            (FunctionCallArguments) visitFunctionCallArguments(ctx.functionCallArguments());
        if (functionExp instanceof NewExpression newExp) {
            return new FunctionCallExpression(newExp.getType(), newExp, args.getArgs());
        }
        String nameS = functionExp.toString();
        FunctionDeclaration functionDeclaration = localFunctions.lookup(new Name(nameS));
        FunctionReference functionRef =
            new FunctionReference(functionDeclaration, functionDeclaration.getType());
        return new FunctionCallExpression(functionRef.getType(), functionRef, args.getArgs());
    }

    @Override
    public SyntaxElement visitIndexAccess(IndexAccessContext ctx) {
        Expression left = visitExpression(ctx.left);
        Expression index = visitExpression(ctx.index);
        return new IndexExpression(left, index);
    }

    @Override
    public SyntaxElement visitSliceAccess(SliceAccessContext ctx) {
        Expression base = visitExpression(ctx.base);
        Expression start = visitExpression(ctx.start);
        Expression end = visitExpression(ctx.end);

        return new IndexRangeExpression(base, start, end, base.getType());
    }

    @Override
    public SyntaxElement visitTernary(TernaryContext ctx) {
        Expression condition = visitExpression(ctx.condition);
        Expression falseExp = visitExpression(ctx.false_);
        Expression trueExp = visitExpression(ctx.true_);

        return new TernaryOperator(falseExp.getType(), condition, falseExp, trueExp);
    }

    @Override
    public SyntaxElement visitNewInstance(NewInstanceContext ctx) {
        KeYSolidityType keyType = (KeYSolidityType) visitTypeName(ctx.typeName());
        Type type = keyType.getSolidityType();
        return new NewExpression(type.name().toString(), type);
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

    public SyntaxElement visitTypeName(TypeNameContext ctx) {
        return ctx.accept(this);
    }

    public SyntaxElement visitTypeDefined(TypeNameContext ctx) {
        throw new RuntimeException("Not implemented yet");
//        Type type = services.getSolidityInfo().getType(new Name(ctx.getText()));
//        final Sort sort = type.getSort(services);
//        return new KeYSolidityType(type, sort);
    }

    @Override
    public SyntaxElement visitElementaryType(ElementaryTypeContext ctx) {
        Type type = SolidityInfo.getPrimitiveType(ctx.getText());
        return services.getSolidityInfo().getKeYSolidityType(type);
    }

    @Override
    public SyntaxElement visitUserDefinedType(UserDefinedTypeContext ctx) {
        return visitTypeDefined(ctx);
    }

    @Override
    public SyntaxElement visitArrayType(ArrayTypeContext ctx) {
        throw new RuntimeException("Not implemented yet");
//        Type primaryType = (Type) visitTypeName(ctx.typeName());
//        if (ctx.expression() == null) {
//            Type type = services.getSolidityInfo().getDynamicTypeMap(primaryType.name());
//            final Sort sort = type.getSort(services);
//            return new KeYSolidityType(type, sort);
//        }
//        Expression sizeExp = visitExpression(ctx.expression());
//        Type type = services.getSolidityInfo().getStaticTypeMap(primaryType.name(), sizeExp);
//        final Sort sort = type.getSort(services);
//        return new KeYSolidityType(type, sort);
    }

    @Override
    public SyntaxElement visitVariableDeclaration(VariableDeclarationContext ctx) {
        return visitVariableDeclarationWithInitialValue(ctx, null);
    }

    public SyntaxElement visitVariableDeclarationWithInitialValue(VariableDeclarationContext ctx,
            Expression initial) {
        KeYSolidityType ksType = (KeYSolidityType) visitTypeName(ctx.typeName());
        ProgramVariable programVariable =
            new ProgramVariable(new Name(ctx.identifier().Identifier().getText()),
                ksType, (DataLocation) visitStorageLocation(ctx.storageLocation()));
        localVars.add(programVariable);
        StatementVariableDeclaration stmDecl =
            new StatementVariableDeclaration(programVariable);
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
        List<ProgramVariable> parameters = ctx.returnParameters() == null ? List.of()
                : ((SyntaxElementList) visitReturnParameters(ctx.returnParameters()))
                        .getElements().stream().map(ProgramVariable.class::cast).toList();
        Block body = (Block) visitBlock(ctx.block());
        List<CatchClause> clauses = ctx.catchClause().stream().map(this::visitCatchClause)
                .map(CatchClause.class::cast).toList();
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
        return ctx == null ? null : visitChildren(ctx);
    }

    @Override
    public SyntaxElement visitParameter(ParameterContext ctx) {
        KeYSolidityType type = (KeYSolidityType) visitTypeName(ctx.typeName());
        DataLocation dataLocation = (DataLocation) visitStorageLocation(ctx.storageLocation());
        String variableName = ctx.identifier().getText();

        ProgramVariable programVariable =
            new ProgramVariable(new Name(variableName), type, dataLocation);
        localVars.add(programVariable);
        return programVariable;
    }

    @Override
    public SyntaxElement visitParameterList(ParameterListContext ctx) {
        return new SyntaxElementList(ctx.parameter().stream().map(this::visitParameter).toList());
    }

    @Override
    public SyntaxElement visitElementaryTypeName(ElementaryTypeNameContext ctx) {
        return services.getSolidityInfo().getKeYSolidityType(SolidityInfo.getPrimitiveType(ctx.getText()));
    }

    @Override
    public SyntaxElement visitStorageLocation(StorageLocationContext ctx) {
        if (ctx == null)
            return Default;
        String location = ctx.getText();
        return DataLocation.fromString(location);
    }

    private static class PositionedConverterException extends RuntimeException {
        private final int line;
        private final int column;

        public PositionedConverterException(String errorMessage, int line, int column) {
            super(errorMessage + " at line " + line + ", column " + column);
            this.line = line;
            this.column = column;
        }

        public int line() {
            return line;
        }

        public int column() {
            return column;
        }

    }
}
