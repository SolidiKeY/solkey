package org.key_project.solidity.antlr;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.parser.SolidityBaseVisitor;
import org.key_project.solidity.parser.SolidityParser.*;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.Statement;

import java.util.ArrayList;
import java.util.List;

public class SolidityToKey extends SolidityBaseVisitor<SyntaxElement> {

    @Override
    public SyntaxElement visitBlock(BlockContext ctx) {
        List<Statement> stms = new ArrayList<>();
        for(var stm: ctx.statement()){
            SyntaxElement stmEl = visitStatement(stm);
            stms.add((Statement) stmEl);
        }
        return new Block(stms);
    }
}
