/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AntlrParserForTestingTest {

    ParserForTesting parser = new ParserForTesting();

    @Test
    void testParseBool() {
        SolidityParser parser = this.parser.parse("true");

        SolidityParser.PrimaryExpressionContext exp = parser.primaryExpression();
        assertEquals("([] true)", exp.toStringTree());
    }

    @Test
    void simpleBlock() {
        SolidityParser.BlockContext block = parser.parseBlockContext("{}");
        assertEquals("([] { })", block.toStringTree());
    }

    @Test
    void schema() {
        SolidityParser parser = this.parser.parse("s#abc");
        SolidityParser.SchemaVariableContext scm = parser.schemaVariable();
        String s = scm.toStringTree();
        assertEquals(0, parser.getNumberOfSyntaxErrors());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{ }",
        "{ int a; }",
        "{ bool a = b; }",
        "{ return true; }",
        "{ a = 5; b = 10; }",
        "{ bool a = true; return a; }",
        "{ int x; { int x; } }",
        "{ uint256 x; { uint256 y; } }",
        "{ if (true) { x = 1; } }",
        "{ if (a > b) x = 1; else x = 2; }",
        "{ for (uint i = 0; i < 10; i++) { } }",
        "{ while (x < 5) { x++; } }",
        "{ do { x--; } while (x > 0); }",
        "{ emit Transfer(msg.sender, to, val); }",
        "{ try externalContract.f() { } catch { } }",
        "{ unchecked { x = x - 1; } }",
        "{ revert(\"error\"); }",
        "{ (a, b) = (1, 2); }",
        "{ address payable x = payable(0x123); }",
        "{ s#schemaStm; }",
        "{ int a = s#schema; }"
    })
    void correctParsing(String input) {
        SolidityParser parser = this.parser.parse(input);
        SolidityParser.BlockContext block = parser.block();
        String s = block.toStringTree();
        assertEquals(0, parser.getNumberOfSyntaxErrors());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{",
        "{ int a }",
        "{ assembly { let x := 0 } }",
    })
    void wrongParsing(String input) {
        SolidityParser parser = this.parser.parse(input);
        SolidityParser.BlockContext block = parser.block();
        assertTrue(parser.getNumberOfSyntaxErrors() > 0);
    }

}
