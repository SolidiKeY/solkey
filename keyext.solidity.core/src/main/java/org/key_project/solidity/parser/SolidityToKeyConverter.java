package org.key_project.solidity.parser;

import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.TupleExpression;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.statement.*;

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
                return switch (operator){
                    case "++" -> new PlusPlusOperator(uExp, expType, prefix);
                    case "--" -> new MinusMinusOperator(uExp, expType, prefix);
                    case "~" -> new BitwiseNotOperator(uExp, expType);
                    case "!" -> new NotOperator(uExp, expType);
                    case "-" -> new NegateOperator(uExp, expType);
                    case "delete" -> new DeleteOperator(uExp, expType);
                    default ->
                            throw new RuntimeException("Not yet supported binary operation: " + operator);
                };
            case 3:
                if(ctx.children.get(1) instanceof TerminalNodeImpl){
                    operator = ctx.children.get(1).toString();
                    Expression left = exps.get(0);
                    Expression right = exps.get(1);
                    return switch (operator){
                        case "+" -> new AddOperator(left, right, expType);
                        case "-" -> new SubtractionOperator(left, right, expType);
                        case "*" -> new MultiplicationOperator(left, right, expType);
                        case "/" -> new DivOperator(left, right, expType);
                        case "%" -> new ModOperator(left, right, expType);
                        case "^" -> new ExponentialOperator(left, right, expType);
                        case "&&" -> new AndOperator(left, right, expType);
                        case "&" -> new BitwiseAndOperator(left, right, expType);
                        case "||" -> new OrOperator(left, right, expType);
                        case "|" -> new BitwiseOrOperator(left, right, expType);
                        case "!=" -> new UnequalOperator(left, right, expType);
                        case "==" -> new EqualOperator(left, right, expType);
                        case ">=" -> new GreaterEqualOperator(left, right, expType);
                        case ">" -> new GreaterOperator(left, right, expType);
                        case "<=" -> new LessEqualOperator(left, right, expType);
                        case "<" -> new LessOperator(left, right, expType);
                        case "<<" -> new LeftShiftOperator(left, right, expType);
                        case ">>" -> new RightShiftOperator(left, right, expType);
                        case ">>>" -> new LogicalRightShiftOperator(left, right, expType);
                        // Assign expressions
                        case "=" -> new AssignmentExpression(left, right, expType);
                        case "|=" -> new OrEqualOperator(left, right, expType);
                        case "^=" -> new XorEqualOperator(left, right, expType);
                        case "&=" -> new AndEqualOperator(left, right, expType);
                        case "<<=" -> new LeftShiftEqualOperator(left, right, expType);
                        case ">>=" -> new RightShiftEqualOperator(left, right, expType);
                        case ">>>=" -> new LogicalRightShiftEqualOperator(left, right, expType);
                        case "+=" -> new PlusEqualOperator(left, right, expType);
                        case "-=" -> new MinusEqualOperator(left, right, expType);
                        case "*=" -> new MultiplicationEqualOperator(left, right, expType);
                        case "/=" -> new DivisionEqualOperator(left, right, expType);
                        case "%=" -> new ModEqualOperator(left, right, expType);
                        default ->
                                throw new RuntimeException("Not yet supported binary operation: " + operator);
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


}
