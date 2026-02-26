/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.op.ProgramVariable;

import org.jspecify.annotations.NonNull;
import org.key_project.solidity.parser.SolidityLexer;
import org.key_project.solidity.parser.SolidityParser;
import org.key_project.solidity.parser.SolidityToKeyConverter;
import org.key_project.solidity.program.ast.statement.Block;

public class SolidityReader {
    protected final Services services;
    protected final NamespaceSet nss;

    public SolidityReader(Services services, NamespaceSet nss) {
        this.services = services;
        this.nss = nss;
    }

    static public SolidityParser parse(String s) {
        CodePointCharStream input = CharStreams.fromString(s);

        SolidityLexer lexer = new SolidityLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        return new SolidityParser(tokens);
    }

    static public SolidityParser.BlockContext parseBlockContext(String s) {
        SolidityParser parser = parse(s);
        return parser.block();
    }

    static public Block parseBlock(SolidityToKeyConverter stk, String s) {
        SolidityParser.BlockContext bc = parseBlockContext(s);
        return (Block) stk.visitBlock(bc);
    }

    public SolidityBlock readBlockWithProgramVariables(
            Namespace<@NonNull ProgramVariable> programVariableNamespace, String solidity) {
        SolidityToKeyConverter stk = new SolidityToKeyConverter(services, programVariableNamespace, new Namespace<>());
        return new SolidityBlock(parseBlock(stk, solidity));
    }

    public SolidityBlock readBlockWithEmptyContext(String solidity) {
        SolidityToKeyConverter stk = new SolidityToKeyConverter(services, nss.programVariables(), new Namespace<>());
        return new SolidityBlock(parseBlock(stk, solidity));
    }
}
