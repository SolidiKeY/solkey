/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast;

import java.io.IOException;

import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.parser.SolidityLexer;
import org.key_project.solidity.parser.SolidityParser;
import org.key_project.solidity.parser.SolidityToKeyConverter;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.parser.SolcParser;
import org.key_project.solidity.rule.sv.ProgramSV;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HirSolidityReader {

    private final Services services;
    private static final Logger LOGGER = LoggerFactory.getLogger(HirSolidityReader.class);

    public HirSolidityReader(Services services, NamespaceSet nss) {
        this(services);
    }

    public HirSolidityReader(Services services) {
        this.services = services;
    }

    public Services getServices() {
        return services;
    }

    public SolidityBlock readBlock(String block, Context context) throws IOException {
        return readBlock(block, context, new Namespace<>());
    }

    public SolidityBlock readBlock(String block, Context context,
            Namespace<ProgramSV> schemaVariables) throws IOException {
        SolcParser solcParser = new SolcParser(services);
        if (context.getSolidityPath() != null)
            solcParser.getDeclStrJsonParser(context.getSolidityPath());
        SolidityToKeyConverter stk =
            new SolidityToKeyConverter(services, context.getVarNS(), schemaVariables);

        CodePointCharStream input = CharStreams.fromString(block);
        SolidityLexer lexer = new SolidityLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SolidityParser parser = new SolidityParser(tokens);
        Block bc = (Block) stk.visitBlock(parser.block());
        return new SolidityBlock(bc);
    }


    public SolidityBlock readBlockWithEmptyContext(String s) throws IOException {
        return readBlock(s, createEmptyContext());
    }

    public SolidityBlock readBlockWithProgramVariables(Namespace<@NonNull ProgramVariable> varNS,
            String s) throws IOException {
        return readBlock(s, new Context(varNS));
    }

    public Context createEmptyContext() {
        return new Context(new Namespace<>());
    }
}
