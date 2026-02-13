package org.key_project.solidity.parser;

import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.TupleExpression;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.AddOperator;
import org.key_project.solidity.program.ast.expressions.operators.AssignmentExpression;
import org.key_project.solidity.program.ast.expressions.operators.PlusPlusOperator;
import org.key_project.solidity.program.ast.statement.*;

import java.math.BigInteger;
import java.util.List;

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
        switch (ctx.children.size()){
            case 2:
                if(ctx.children.get(0) instanceof TerminalNodeImpl){
                    String opStr = ctx.children.get(0).toString();
                    Expression exp = exps.get(0);
                    return switch (opStr){
                        case "++" -> new PlusPlusOperator(exp, UINT256, true);
                        default -> throw new IllegalStateException("Unexpected value: " + opStr);
                    };

                }
            case 3:
                if(ctx.children.get(1) instanceof TerminalNodeImpl){
                    String opStr = ctx.children.get(1).toString();
                    Expression expL = exps.get(0);
                    Expression expR = exps.get(1);
                    return switch (opStr){
                        case "+" -> new AddOperator(expL, expR, UINT256);
                        case "=" -> new AssignmentExpression(expL, expR, UINT256);
                        default -> throw new IllegalStateException("Unexpected value: " + opStr);
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
}
