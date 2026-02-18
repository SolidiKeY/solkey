/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.PosInProgram;
import org.key_project.solidity.program.ProgramPrefix;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

public class Block implements Statement, ProgramPrefix {

    private final ImmutableArray<Statement> statements;

    public Block(ImmutableArray<Statement> statements) {
        this.statements = statements;
    }

    public Block(List<Statement> statements) {
        this.statements = new ImmutableArray<>(statements);
    }

    public Block(ExtList children) {
        this.statements = new ImmutableArray<>(children.collect(Statement.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        return statements.get(n);
    }

    @Override
    public int getChildCount() {
        return statements.size();
    }

    public boolean isEmpty() {
        return statements.isEmpty();
    }

    @Override
    public String toString() {
        String body = "{\n";
        for (Statement statement : statements) {
            body += statement.toString() + "\n";
        }
        return body + "}\n";
    }

    public ImmutableArray<Statement> getStatements() { return statements; }

    @Override
    public boolean isPrefix() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean hasNextPrefixElement() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public ProgramPrefix getNextPrefixElement() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public ProgramPrefix getLastPrefixElement() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public ImmutableArray<ProgramPrefix> getPrefixElements() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public PosInProgram getFirstActiveChildPos() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public int getPrefixLength() {
        throw new RuntimeException("Not implemented yet");
    }

    public void visit(Visitor v) {
        v.performActionOnBlock(this);
    }
}
