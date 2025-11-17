/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.List;
import java.util.stream.Collectors;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.util.collection.ImmutableArray;

public class Block implements Statement {

    private int id;
    private final ImmutableArray<Statement> statements;
    private String errorName;
    private List<Declaration> arguments;

    public Block(ImmutableArray<Statement> statements) {
        this.statements = statements;
    }

    public Block(int id, List<Statement> statements, String errorName,
            List<Declaration> arguments) {
        this.id = id;
        this.statements = new ImmutableArray<>(statements);
        this.errorName = errorName;
        this.arguments = arguments;
    }

    public Block(int id, List<Statement> statements) {
        this.id = id;
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

    public String toStringCatch() {
        String body = "catch ";
        if (errorName != null && !errorName.isEmpty()) {
            body += errorName + " ("
                + arguments.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
        }
        body += " " + this;
        return body;
    }

    public ImmutableArray<Statement> getStatements() { return statements; }
}
