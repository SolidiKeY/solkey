package org.key_project.solidity.parser;

import org.junit.jupiter.api.Test;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.expressions.TupleExpression;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.AddOperator;
import org.key_project.solidity.program.ast.expressions.operators.PlusPlusOperator;
import org.key_project.solidity.program.ast.statement.Block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.key_project.solidity.antlr.Parser.*;

public class SolidityToKeyTest {

    @Test
    void emptyBlock(){
        Block block = parseBlock("{ }");
        assertEquals(0, block.getStatements().size());
    }

    @Test
    void literalInt(){
        Uint256Literal exp = (Uint256Literal) parseExpression("1");
        assertEquals(1, exp.getValue().intValue());
    }

    @Test
    void literalBool(){
        BoolLiteral exp = (BoolLiteral) parseExpression("false");
        assertEquals(false, exp.getValue());
    }

    @Test
    void tupleExpression(){
        TupleExpression exp = (TupleExpression) parseExpression("(false, true)");
        assertEquals(false, ((BoolLiteral) exp.getChild(0)).getValue());
    }

    @Test
    void addiction() {
        AddOperator exp = (AddOperator) parseExpression("1 + 2");
        assertEquals(1, ((Uint256Literal) exp.getChild(0)).getValue().intValue());
    }

    @Test
    void plusPlusLeft() {
        PlusPlusOperator exp = (PlusPlusOperator) parseExpression("++1");
        assertEquals(1, ((Uint256Literal) exp.getChild(0)).getValue().intValue());
        assertTrue(exp.isPrefix());
    }
}
