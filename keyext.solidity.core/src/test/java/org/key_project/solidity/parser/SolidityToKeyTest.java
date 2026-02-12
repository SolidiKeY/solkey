package org.key_project.solidity.parser;

import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.key_project.solidity.antlr.Parser;
import org.key_project.solidity.antlr.SolidityToKey;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.statement.Block;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SolidityToKeyTest {

    @Test
    void emptyBlock(){
        SolidityToKey stk = new SolidityToKey();
        BlockContext bc = Parser.parseBlock("{ }");
        Block block = (Block) stk.visitBlock(bc);
        assertEquals(0, block.getStatements().size());
    }
}
