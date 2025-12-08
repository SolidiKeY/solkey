/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class TryStatement implements Statement {

    private final Expression expression;
    private final List<Block> blocks;

    public TryStatement(Expression expression, List<Block> blocks) {
        this.expression = expression;
        this.blocks = blocks;
    }

    public TryStatement(ExtList children) {
        this.expression = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.blocks = Objects.requireNonNull(children.removeFirstOccurrence(List.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0)
            return expression;
        n -= 1;
        if (n >= 0 && n < getChildCount()) {
            return blocks.get(n);
        }
        return null;
    }

    @Override
    public int getChildCount() {
        return blocks.size() + 1;
    }

    @Override
    public String toString() {
        Block tryBlock = blocks.getFirst();
        List<Block> catchBlocks = blocks.subList(1, blocks.size());
        return "try " + expression + " " + tryBlock + " " +
            catchBlocks.stream().map(Block::toStringCatch).collect(Collectors.joining());
    }

    public void visit(Visitor v) {
        v.performActionOnTryStatement(this);
    }
}
