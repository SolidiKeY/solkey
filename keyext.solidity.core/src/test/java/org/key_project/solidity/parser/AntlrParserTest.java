/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.key_project.solidity.antlr.Parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AntlrParserTest {

    @Test
    void testParseBool() {
        SolidityParser parser = Parser.parse("true");

        SolidityParser.PrimaryExpressionContext exp = parser.primaryExpression();
        assertEquals("([] true)", exp.toStringTree());
    }

    @Test
    void simpleBlock() {
        SolidityParser.BlockContext block = Parser.parseBlock("{}");
        assertEquals("([] { })", block.toStringTree());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{ }",
            "{ int a; }",
            "{ bool a = b; }",
            "{ return true; }",
    })
    void correctParsing(String input) {
        SolidityParser parser = Parser.parse(input);
        SolidityParser.BlockContext block = parser.block();
        String s = block.toStringTree();
        assertEquals(0, parser.getNumberOfSyntaxErrors());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{",
            "{ int a }",
    })
    void wrongParsing(String input) {
        SolidityParser parser = Parser.parse(input);
        SolidityParser.BlockContext block = parser.block();
        assertTrue(parser.getNumberOfSyntaxErrors() > 0);
    }

}
