/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import java.math.BigInteger;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.SolidityInfo;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MemoryReferenceTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.operators.Operator;
import org.key_project.solidity.program.ast.expressions.operators.TernaryExpression;
import org.key_project.solidity.program.ast.expressions.operators.UnaryExpression;
import org.key_project.solidity.program.ast.ghost.*;
import org.key_project.solidity.program.ast.references.FieldReference;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.references.TypeReference;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.program.ext.ContextStatementBlock;
import org.key_project.solidity.program.parser.ParserUtils;
import org.key_project.solidity.rule.metaconstruct.ExpandFunctionBody;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;

import org.antlr.v4.runtime.Token;
import org.jspecify.annotations.Nullable;

import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.VOID;
import static org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation.Default;

public class SolidityToKeyConverter extends SolidityBaseVisitor<SyntaxElement> {
    private Namespace<ProgramVariable> localVars;
    final private Namespace<? extends SchemaVariable> schemaVariables;
    final private Services services;

    public SolidityToKeyConverter() {
        this(new Services());
    }

    public SolidityToKeyConverter(Services services, Namespace<ProgramVariable> localVars,
            Namespace<? extends SchemaVariable> schemaVariables) {
        this.services = services;
        this.localVars = localVars;
        this.schemaVariables = schemaVariables;
    }

    public SolidityToKeyConverter(Services services) {
        this(services, new Namespace<>(), new Namespace<>());
    }

    public Namespace<ProgramVariable> localVars() {
        return localVars;
    }

    public Namespace<? extends SchemaVariable> schemaVariables() {
        return schemaVariables;
    }

    public Services services() {
        return services;
    }

    @Override
    public SyntaxElement visitBlock(BlockContext ctx) {
        localVars = new Namespace<>(localVars);
        boolean isContextBlock = ctx.contextBlock() != null;
        var statements =
            isContextBlock ? ctx.contextBlock().statement() : ctx.normalBlock().statement();
        List<Statement> stms = statements.stream()
                .map(this::visitStatement).map(Statement.class::cast).toList();
        localVars = localVars.parent();
        return isContextBlock ? new ContextStatementBlock(ImmutableList.fromList(stms))
                : new Block(stms);
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

    @Override
    public SyntaxElement visitExpandFunctionBodyTransformer(
            ExpandFunctionBodyTransformerContext ctx) {
        // strip the "s#" prefix from the schema variable name
        String variableName = ctx.schemaVariable().getText().substring(2);
        SchemaVariable sv = schemaVariables.lookup(variableName);
        if (sv == null) {
            reportError("Schema Variable " + variableName + " not declared.", ctx.start);
        }
        if (!(sv instanceof ProgramSV)) {
            reportError("Schema variable '" + variableName
                + "' used in s#expand_function_body must be a program schema variable, e.g. "
                + "'\\program FunctionBody " + variableName + ";', but is "
                + sv.getClass().getSimpleName() + ". Check its \\schemaVariables declaration and "
                + "its ProgramSVSort.", ctx.start);
        }
        return new ExpandFunctionBody((ProgramSV) sv);
    }

    private void reportError(String errorMsg, Token tokenWithPos)
            throws PositionedConverterException {
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
        reportError("Tuple expressions are not implemented yet.", ctx.start);
        return null; // unreachable: reportError always throws
        // TODO: implement (see disabled body below)
        // List<Expression> exps = parseExps(ctx.expression());
        // TupleType tupleType = services.getSolidityInfo().getTupleTypeMap(
        // exps.stream().map(Expression::getType).toList());
        // return new TupleExpression(tupleType, exps);
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
        return ParserUtils.parseAllBinary(left, right, operator);
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
    public SyntaxElement visitDelete(DeleteContext ctx) {
        Expression target = visitExpression(ctx.expression());
        return new UnaryExpression(Operator.DELETE, target);
    }

    @Override
    public SyntaxElement visitPostfix(PostfixContext ctx) {
        String operator = ctx.children.get(1).toString();
        Expression uExp = visitExpression(ctx.expression());
        return ParserUtils.parseUnaryOperation(uExp, operator, false);
    }

    @Override
    public SyntaxElement visitFunctionBodyStatement(FunctionBodyStatementContext ctx) {
        Name functionName = new Name(ctx.fn.getText());
        Name contractName = new Name(ctx.contract.getText());
        FunctionCallArguments args =
            (FunctionCallArguments) visitFunctionCallArguments(ctx.functionCallArguments());

        FunctionDeclaration function = null;
        for (FunctionDeclaration fd : services.getSolidityInfo().getFunctions(contractName)) {
            if (fd.name().equals(functionName)) {
                function = fd;
                break;
            }
        }
        if (function == null) {
            reportError("Unknown function " + functionName + " in contract " + contractName,
                ctx.start);
        }
        // optional left-hand side binds the function's return value
        ProgramVariable resultVar = null;
        if (ctx.lhs != null) {
            String resultName = ctx.lhs.getText();
            resultVar = localVars.lookup(resultName);
            if (resultVar == null) {
                reportError("Result variable " + resultName + " out of the scope", ctx.start);
            }
        }
        return new FunctionBodyStatement(resultVar, function, args.getArgs(), contractName);
    }

    @Override
    public SyntaxElement visitFunctionCallExp(FunctionCallExpContext ctx) {
        FunctionCallArguments args =
            (FunctionCallArguments) visitFunctionCallArguments(ctx.functionCallArguments());
        FunctionDeclaration functionDeclaration = simpleFunctionCallTarget(ctx.expression());
        if (functionDeclaration != null) {
            FunctionReference functionRef =
                new FunctionReference(functionDeclaration, functionDeclaration.getType());
            return new FunctionCallExpression(functionRef.getType(), functionRef, args.getArgs());
        }
        Expression functionExp = visitExpression(ctx.expression());
        if (functionExp instanceof NewExpression newExp) {
            return new FunctionCallExpression(newExp.getType(), newExp, args.getArgs());
        }
        if (functionExp instanceof MemberExp memberExp
                && memberExp.getRightExp() instanceof FunctionDeclaration memberFunction) {
            return new FunctionCallExpression(
                inferBuiltinMemberCallType(memberExp, memberFunction, args),
                memberExp, args.getArgs());
        }
        Name name = new Name(functionExp.toString());
        functionDeclaration = services.getSolidityInfo().getFunctionDeclaration(name);
        if (functionDeclaration == null) {
            reportError("Unknown function " + name, ctx.start);
        }
        FunctionReference functionRef =
            new FunctionReference(functionDeclaration, functionDeclaration.getType());
        return new FunctionCallExpression(functionRef.getType(), functionRef, args.getArgs());
    }

    private Type inferBuiltinMemberCallType(MemberExp memberExp,
            FunctionDeclaration functionDeclaration, FunctionCallArguments args) {
        String functionName = functionDeclaration.name().toString();
        if ("pop".equals(functionName)
                || ("push".equals(functionName) && !args.getArgs().isEmpty())) {
            return VOID;
        }
        if ("push".equals(functionName)) {
            Type receiverType = memberExp.getLeftExp().getType();
            Type arrayType = receiverType instanceof KeYSolidityType kst ? kst.getSolidityType()
                    : receiverType;
            if (arrayType instanceof DynamicArrayType dynamicArrayType) {
                return dynamicArrayType.getElementType();
            }
        }
        return functionDeclaration.getType();
    }

    private @Nullable FunctionDeclaration simpleFunctionCallTarget(ExpressionContext ctx) {
        if (ctx instanceof PrimaryContext primary
                && primary.primaryExpression().identifier() != null) {
            Name name = new Name(primary.primaryExpression().identifier().getText());
            return services.getSolidityInfo().getFunctionDeclaration(name);
        }
        return null;
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
    public SyntaxElement visitMemberAccess(MemberAccessContext ctx) {
        Expression leftExp = visitExpression(ctx.expression());
        Type leftType = leftExp.getType();
        if (ctx.schemaVariable() != null) {
            String svName = ctx.schemaVariable().getText().substring(2);
            SchemaVariable sv = schemaVariables.lookup(svName);
            if (sv == null) {
                reportError("Schema Variable " + svName + " not declared.", ctx.start);
            }
            return new MemberExp(leftExp, sv, leftType);
        }
        String fieldName = ctx.identifier().getText();
        FunctionDeclaration builtinFunction =
            SolidityInfo.getBuiltinFunctionDeclaration(new Name(fieldName));
        if (builtinFunction != null && ("push".equals(fieldName) || "pop".equals(fieldName))) {
            return new MemberExp(leftExp, builtinFunction, builtinFunction.getType());
        }
        FieldDeclaration resolved = resolveStructField(leftType, fieldName);
        FieldDeclaration field = resolved != null ? resolved
                : new FieldDeclaration(
                    new Name(fieldName), new TypeReference(new Name(fieldName)));
        Type memberType = resolved != null && resolved.getTypeReference().referencedType != null
                ? resolved.getTypeReference().referencedType
                : leftType;
        return new MemberExp(leftExp, field, memberType);
    }

    private @org.jspecify.annotations.Nullable FieldDeclaration resolveStructField(Type leftType,
            String fieldName) {
        Type unwrapped = leftType instanceof KeYSolidityType kst ? kst.getSolidityType() : leftType;
        if (!(unwrapped instanceof StructDeclaration struct)) {
            return null;
        }
        for (FieldDeclaration f : struct.getFields()) {
            if (f.name().toString().equals(fieldName)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public SyntaxElement visitTernary(TernaryContext ctx) {
        Expression condition = visitExpression(ctx.condition);
        Expression falseExp = visitExpression(ctx.false_);
        Expression trueExp = visitExpression(ctx.true_);

        return new TernaryExpression(falseExp.getType(), condition, falseExp, trueExp);
    }

    @Override
    public SyntaxElement visitNewInstance(NewInstanceContext ctx) {
        SyntaxElement type = visitTypeName(ctx.typeName());
        // a Type schema variable stands directly for the type; otherwise unwrap the KeYSolidityType
        Type newType = type instanceof KeYSolidityType kst ? kst.getSolidityType() : (Type) type;
        return new NewExpression(newType);
    }

    @Override
    public SyntaxElement visitExpressionStatement(ExpressionStatementContext ctx) {
        return new ExpressionStatement(visitExpression(ctx.expression()));
    }

    @Override
    public SyntaxElement visitIdentifier(IdentifierContext ctx) {
        String variableName = ctx.Identifier().getText();
        ProgramVariable res = localVars.lookup(variableName);
        if (res != null) {
            return res;
        }
        // not a local variable: it may be a contract state variable, which denotes a field access
        StateVariableDeclaration field =
            services.getSolidityInfo().getStateVariableDeclaration(new Name(variableName));
        if (field != null) {
            return new FieldReference(field, field.getType());
        }
        reportError("Identifier '" + variableName + "' is out of scope: it is neither a local "
            + "variable/parameter nor a contract state variable. Check that it is declared "
            + "(e.g. in a \\programVariables block) or that the contract field exists in the "
            + "loaded \\programSource.", ctx.start);
        return null; // unreachable: reportError always throws
    }

    public SyntaxElement visitTypeName(TypeNameContext ctx) {
        return ctx.accept(this);
    }

    @Override
    public SyntaxElement visitSchemaType(SchemaTypeContext ctx) {
        // strip the "s#" prefix and look up the (Type-sorted) schema variable
        String variableName = ctx.schemaVariable().getText().substring(2);
        SchemaVariable sv = schemaVariables.lookup(variableName);
        if (sv == null) {
            reportError("Schema Variable " + variableName + " not declared.", ctx.start);
        }
        return sv;
    }

    public SyntaxElement visitTypeDefined(TypeNameContext ctx) {
        KeYSolidityType kst = services.getSolidityInfo().getKeYSolidityType(ctx.getText());
        if (kst == null) {
            reportError("Unknown type " + ctx.getText(), ctx.start);
        }
        return kst;
    }

    @Override
    public SyntaxElement visitElementaryType(ElementaryTypeContext ctx) {
        return primitiveKST(ctx.getText(), ctx.start);
    }

    /// Resolves a primitive type name to its registered KeYSolidityType.
    private KeYSolidityType primitiveKST(String name, Token pos) {
        Type type = SolidityInfo.getPrimitiveType(name);
        KeYSolidityType kst =
            type == null ? null : services.getSolidityInfo().getKeYSolidityType(type);
        if (kst == null) {
            reportError("Unknown primitive type " + name, pos);
        }
        return kst;
    }

    @Override
    public SyntaxElement visitUserDefinedType(UserDefinedTypeContext ctx) {
        return visitTypeDefined(ctx);
    }

    @Override
    public SyntaxElement visitArrayType(ArrayTypeContext ctx) {
        KeYSolidityType elementKST = (KeYSolidityType) visitTypeName(ctx.typeName());
        String suffix = ctx.expression() == null
                ? "[]"
                : "[" + visitExpression(ctx.expression()) + "]";
        String name = elementKST.name() + suffix;
        KeYSolidityType kst = services.getSolidityInfo().getKeYSolidityType(name);
        if (kst == null) {
            reportError("Array type " + name + " is not used in the contract "
                + "and therefore unknown", ctx.start);
        }
        return kst;
    }

    @Override
    public SyntaxElement visitVariableDeclaration(VariableDeclarationContext ctx) {
        return visitVariableDeclarationWithInitialValue(ctx, null);
    }

    public SyntaxElement visitVariableDeclarationWithInitialValue(VariableDeclarationContext ctx,
            Expression initial) {
        SyntaxElement type = visitTypeName(ctx.typeName());

        // Taclet pattern: the variable position is a program schema variable (e.g. `T s#v = e;`).
        // Matching binds the schema variable to the concrete declared variable.
        if (ctx.schemaVariable() != null) {
            String svName = ctx.schemaVariable().getText().substring(2);
            SchemaVariable sv = schemaVariables.lookup(svName);
            if (sv == null) {
                reportError("Schema Variable " + svName + " not declared.", ctx.start);
            }
            StatementVariableDeclaration schematic =
                new StatementVariableDeclaration(type, (ProgramSV) sv);
            return new DeclarationStatement(List.of(schematic), initial);
        }

        DataLocation dataLocation = (DataLocation) visitStorageLocation(ctx.storageLocation());
        KeYSolidityType kst = asLocalVariableType((KeYSolidityType) type, dataLocation);
        ProgramVariable programVariable =
            new ProgramVariable(new Name(ctx.identifier().Identifier().getText()),
                kst, dataLocation);
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
            new ProgramVariable(new Name(variableName),
                asLocalVariableType(type, dataLocation), dataLocation);
        localVars.add(programVariable);
        return programVariable;
    }

    @Override
    public SyntaxElement visitParameterList(ParameterListContext ctx) {
        return new SyntaxElementList(ctx.parameter().stream().map(this::visitParameter).toList());
    }

    @Override
    public SyntaxElement visitElementaryTypeName(ElementaryTypeNameContext ctx) {
        return primitiveKST(ctx.getText(), ctx.start);
    }

    @Override
    public SyntaxElement visitStorageLocation(StorageLocationContext ctx) {
        if (ctx == null)
            return Default;
        String location = ctx.getText();
        return DataLocation.fromString(location);
    }

    /// A `storage`-qualified local (`T storage lp = …`) is a path alias, not a value: it points
    /// at a storage location and is updated by `{lp := <path>}`. Re-sort it to `List` so the
    /// path-shaped update type-checks and downstream lowering through
    /// [Services#convertToLogicElement] uses `consr(lp, fld)` instead of wrapping in a fresh
    /// single-element list.
    private KeYSolidityType asStorageAliasType(KeYSolidityType original,
            DataLocation dataLocation) {
        if (dataLocation != DataLocation.Storage || original == null) {
            return original;
        }
        var sort = original.getSort();
        if (sort == null || !"Struct".equals(sort.name().toString())) {
            return original;
        }
        var listSort = services.getNamespaces().sorts().lookup(new Name("List"));
        if (listSort == null) {
            return original;
        }
        return new KeYSolidityType(original.getSolidityType(), listSort);
    }

    private KeYSolidityType asLocalVariableType(KeYSolidityType original,
            DataLocation dataLocation) {
        return MemoryReferenceTypes.asMemoryReferenceType(
            asStorageAliasType(original, dataLocation), dataLocation, services);
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
