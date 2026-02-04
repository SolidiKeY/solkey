/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import org.key_project.solidity.antlr.Parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void blockOneStm() {
        SolidityParser.BlockContext block = Parser.parseBlock("{ int a; }");
        String s = block.toStringTree();
    }

}
