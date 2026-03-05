package org.key_project.solidity.program.ast;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jspecify.annotations.NonNull;
import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.parser.SolidityLexer;
import org.key_project.solidity.parser.SolidityParser;
import org.key_project.solidity.parser.SolidityToKeyConverter;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.parser.SolJSONParser;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.key_project.solidity.program.parser.SolcWrapper.getDeclStrJsonParser;

public class HirSolidityReader {

    private final Services services;
    private static final Logger LOGGER = LoggerFactory.getLogger(HirSolidityReader.class);

    public HirSolidityReader(Services services) {
        this.services = services;
    }

    public Services getServices() {
        return services;
    }

    public Block readBlock(String block, Context context) throws IOException {
        SolJSONParser jsonParser = new SolJSONParser(services);
        getDeclStrJsonParser(jsonParser, context.getSolidityPath());
        Namespace<ProgramSV> schemaVariables = new Namespace<>();
        SolidityToKeyConverter stk = new SolidityToKeyConverter(services, context.getVarNS(), schemaVariables);

        CodePointCharStream input = CharStreams.fromString(block);
        SolidityLexer lexer = new SolidityLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SolidityParser parser = new SolidityParser(tokens);
        return (Block) stk.visitBlock(parser.block());
    }


    public Block readBlockWithEmptyContext(String s) throws IOException {
        return readBlock(s, createEmptyContext());
    }

    public Block readBlockWithProgramVariables(Namespace<@NonNull ProgramVariable> varNS,
                                                    String s) throws IOException {
        return readBlock(s, new Context(varNS));
    }

    public Context createEmptyContext() {
        return new Context(new Namespace<>());
    }
}
