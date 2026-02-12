package org.key_project.solidity.antlr;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.parser.SolidityBaseVisitor;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.TupleExpression;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.Statement;

import java.math.BigInteger;
import java.util.List;

import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.BOOL;

public class SolidityToKey extends SolidityBaseVisitor<SyntaxElement> {

    @Override
    public SyntaxElement visitBlock(BlockContext ctx) {
        List<Statement> stms = ctx.statement().stream()
                .map(stm -> (Statement) visitStatement(stm)).toList();
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
        if(ctx.BooleanLiteral() != null){
            boolean b = Boolean.parseBoolean(ctx.BooleanLiteral().getText());
            return new BoolLiteral(b);
        } else if (ctx.tupleExpression() != null) {
            List<Expression> exps = ctx.tupleExpression().expression().stream()
                    .map(exp -> (Expression) visitExpression(exp))
                    .toList();
            return new TupleExpression(BOOL, exps);
        }
        return visitChildren(ctx);
    }


}
