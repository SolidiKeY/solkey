package org.key_project.solidity.parser;

import org.junit.jupiter.api.Test;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.statement.Block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.key_project.solidity.antlr.Parser.parseBlock;

public class SolidityToKeyTest {

    @Test
    void emptyBlock(){
        Block block = parseBlock("{ }");
        assertEquals(0, block.getStatements().size());
    }
}
