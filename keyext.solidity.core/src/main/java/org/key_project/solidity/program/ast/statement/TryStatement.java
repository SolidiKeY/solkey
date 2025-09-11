package org.key_project.solidity.program.ast.statement;

import org.key_project.logic.SyntaxElement;

import java.util.List;

public class TryStatement implements Statement {

    private final List<Block> blocks;

    public TryStatement(List<Block> blocks) {
        this.blocks = blocks;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
