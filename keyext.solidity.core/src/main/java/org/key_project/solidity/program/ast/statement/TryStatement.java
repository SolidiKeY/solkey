package org.key_project.solidity.program.ast.statement;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.expressions.Expression;

import java.util.List;
import java.util.stream.Collectors;

public class TryStatement implements Statement {

    private final Expression expression;
    private final List<Block> blocks;

    public TryStatement(Expression expression, List<Block> blocks) {
        this.expression = expression;
        this.blocks = blocks;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if(n >= 0 && n < getChildCount()){
            return blocks.get(n);
        }
        return null;
    }

    @Override
    public int getChildCount() {
        return blocks.size();
    }

    @Override
    public String toString() {
        return "try " + expression + " " + blocks.stream().map(Block::toString).collect(Collectors.joining());
    }
}
