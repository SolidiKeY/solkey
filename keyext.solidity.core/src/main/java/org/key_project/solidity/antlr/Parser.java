/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.antlr;

import org.key_project.solidity.parser.SolidityLexer;
import org.key_project.solidity.parser.SolidityParser;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.parser.SolidityToKeyConverter;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.Statement;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class Parser {

    static SolidityToKeyConverter stk = new SolidityToKeyConverter();

    static public SolidityParser parse(String s) {
        CodePointCharStream input = CharStreams.fromString(s);

        SolidityLexer lexer = new SolidityLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        return new SolidityParser(tokens);
    }

    static public BlockContext parseBlockContext(String s) {
        SolidityParser parser = parse(s);
        return parser.block();
    }

    static public Block parseBlock(String s) {
        BlockContext bc = Parser.parseBlockContext(s);
        return (Block) stk.visitBlock(bc);
    }

    static public Expression parseExpression(String s) {
        SolidityParser parser = parse(s);
        ExpressionContext expCtx = parser.expression();
        return stk.visitExpression(expCtx);
    }

    static public Statement parseStatement(String s) {
        SolidityParser parser = parse(s);
        StatementContext stmCtx = parser.statement();
        return (Statement) stk.visitStatement(stmCtx);
    }
}
