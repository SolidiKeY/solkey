package org.key_project.solidity.parser;

import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.TupleExpression;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.ghost.ExpressionList;
import org.key_project.solidity.program.ast.ghost.FunctionCallArguments;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.program.parser.ParserUtils;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;

import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.BOOL;
import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.UINT256;

public class SolidityToKeyConverter extends SolidityBaseVisitor<SyntaxElement> {

    @Override
    public SyntaxElement visitBlock(BlockContext ctx) {
        List<Statement> stms = ctx.statement().stream()
                .map(this::visitStatement).map(Statement.class::cast).toList();
        return new Block(stms);
    }

    @Override public SyntaxElement visitNumberLiteral(NumberLiteralContext ctx) {
        if(ctx.DecimalNumber() != null){
            BigInteger number = new BigInteger(ctx.DecimalNumber().getText());
            return new Uint256Literal(number);
        }
        return visitChildren(ctx);
    }

    @Override
    public SyntaxElement visitPrimaryExpression(PrimaryExpressionContext ctx) {
        if(ctx.identifier() != null){
            return visitIdentifier(ctx.identifier());
        }
        else if(ctx.BooleanLiteral() != null){
            boolean b = Boolean.parseBoolean(ctx.BooleanLiteral().getText());
            return new BoolLiteral(b);
        } else if (ctx.tupleExpression() != null) {
            List<Expression> exps = ctx.tupleExpression().expression().stream()
                    .map(this::visitExpression).map(Expression.class::cast).toList();
            return new TupleExpression(BOOL, exps);
        }
        return visitChildren(ctx);
    }

    @Override
    public SyntaxElement visitExpression(ExpressionContext ctx) {
        List<Expression> exps = ctx.expression().stream()
                    .map(this::visitExpression).map(Expression.class::cast).toList();
        Type expType = UINT256;
        switch (ctx.children.size()){
            case 2:
                boolean prefix = ctx.children.get(0) instanceof TerminalNodeImpl;
                String operator = ctx.children.get(prefix ? 0 : 1).toString();
                Expression uExp = exps.getFirst();
                return ParserUtils.parseUnaryOperation(uExp, operator, expType, prefix);
            case 3:
                if(ctx.children.get(1) instanceof TerminalNodeImpl){
                    operator = ctx.children.get(1).toString();
                    Expression left = exps.get(0);
                    Expression right = exps.get(1);
                    return ParserUtils.parseAllBinary(left, right, operator, expType);
                }
            case 4:
                if(ctx.children.get(1) instanceof TerminalNodeImpl){
                    String nameS = exps.getFirst().toString();
                    Name name = new Name(nameS);
                    return switch (ctx.children.get(1).toString()) {
                        case "(" -> {
                            FunctionReference functionRef = new FunctionReference(0, name, UINT256);
                            FunctionCallArguments args = (FunctionCallArguments) visitFunctionCallArguments(ctx.functionCallArguments());
                            yield new FunctionCallExpression(UINT256, functionRef, args.getArgs());
                        }
                        case "[" -> {
                            ProgramVariable p = new ProgramVariable(name, null, null);
                            yield new IndexExpression(p, exps.get(1), UINT256);
                        }
                        default ->
                                throw new IllegalStateException("Unexpected value: " + ctx.children.get(1).toString());
                    };
                }
        }
        return visitChildren(ctx);
    }

    @Override
    public SyntaxElement visitExpressionStatement(ExpressionStatementContext ctx) {
        return new ExpressionStatement((Expression) visitExpression(ctx.expression()));
    }

    @Override public SyntaxElement visitIdentifier(IdentifierContext ctx) {
        String variableName = ctx.Identifier().getText();
        return new ProgramVariable(new Name(variableName), null, null);
    }

    @Override
    public SyntaxElement visitVariableDeclarationStatement(VariableDeclarationStatementContext ctx) {
        ProgramVariable programVariable = (ProgramVariable) visitIdentifier(ctx.variableDeclaration().identifier());
        StatementVariableDeclaration stmDecl = new StatementVariableDeclaration(programVariable,"", null);
        return new DeclarationStatement(List.of(stmDecl), null);
    }

    @Override
    public SyntaxElement visitIfStatement(IfStatementContext ctx) {
        Expression condition = (Expression) visitExpression(ctx.expression());
        var stms = ctx.statement();
        Statement trueBody = (Statement) visitStatement(stms.getFirst());
        Statement elseBody = stms.size() <= 1 ? null : (Statement) visitStatement(stms.get(1));
        return new ConditionStatement(condition, trueBody, elseBody);
    }

    @Override
    public SyntaxElement visitReturnStatement(ReturnStatementContext ctx) {
        if(ctx.expression() == null)
            return new ReturnStatment();
        Expression exp = (Expression) visitExpression(ctx.expression());
        return new ReturnStatment(exp);
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
        Expression exp = (Expression) visitExpression(ctx.expression());
        Statement stm = (Statement) visitStatement(ctx.statement());
        return new WhileStatement(exp, stm);
    }

    @Override
    public SyntaxElement visitDoWhileStatement(DoWhileStatementContext ctx) {
        Expression exp = (Expression) visitExpression(ctx.expression());
        Statement stm = (Statement) visitStatement(ctx.statement());
        return new DoWhileStatement(exp, stm);
    }

    @Override
    public SyntaxElement visitForStatement(ForStatementContext ctx) {
        Expression initial = ctx.simpleStatement() == null ? null :
                ((ExpressionStatement) visitSimpleStatement(ctx.simpleStatement())).getExpression();
        Expression condition = ctx.expressionStatement() == null ? null :
                ((ExpressionStatement) visitExpressionStatement(ctx.expressionStatement())).getExpression();
        Expression loopExp = ctx.expression() == null ? null : (Expression) visitExpression(ctx.expression());
        Statement body = (Statement) visitStatement(ctx.statement());
        return new ForStatement(initial, condition, loopExp, body);

    }

    @Override
    public SyntaxElement visitCatchClause(CatchClauseContext ctx) {
        return visitBlock(ctx.block());
    }

    @Override
    public SyntaxElement visitTryStatement(TryStatementContext ctx) {
        Expression exp = (Expression) visitExpression(ctx.expression());
        List<Block> blocks = Stream.concat(
                Stream.of((Block) visitBlock(ctx.block())),
                ctx.catchClause().stream().map(this::visitCatchClause).map(Block.class::cast)
        ).toList();
        return new TryStatement(exp, blocks);
    }

    @Override
    public SyntaxElement visitExpressionList(ExpressionListContext ctx) {
        List<Expression> expressions = ctx.expression().stream().map(this::visitExpression).map(Expression.class::cast).toList();
        return new ExpressionList(expressions);
    }

    @Override
    public SyntaxElement visitFunctionCallArguments(FunctionCallArgumentsContext ctx) {
        return new FunctionCallArguments((ExpressionList) visitExpressionList(ctx.expressionList()));
    }

}
