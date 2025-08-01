/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.util.collection.ImmutableArray;

public class Block implements Statement {

    private final ImmutableArray<Statement> statements;

    public Block(ImmutableArray<Statement> statements) {
        this.statements = statements;
    }

    public Block(List<Statement> statements) {
        this.statements = new ImmutableArray<>(statements);
    }

    @Override
    public SyntaxElement getChild(int n) {
        return statements.get(n);
    }

    @Override
    public int getChildCount() {
        return statements.size();
    }

    @Override
    public String toString() {
        String body = "{";
        for (Statement statement : statements) {
            body += statement.toString();
        }
        return body + "}";
    }

    public ImmutableArray<Statement> getStatements() { return statements; }
}
