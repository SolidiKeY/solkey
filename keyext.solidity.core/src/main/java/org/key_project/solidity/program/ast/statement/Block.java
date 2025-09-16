/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.ParameterDeclaration;
import org.key_project.util.collection.ImmutableArray;

public class Block implements Statement {

    private final ImmutableArray<Statement> statements;
    private String errorName;
    private List<ParameterDeclaration> inputParamenters;

    public Block(ImmutableArray<Statement> statements) {
        this.statements = statements;
    }

    public Block(List<Statement> statements, String errorName, List<ParameterDeclaration> inputParamenters) {
        this.statements = new ImmutableArray<>(statements);
        this.errorName = errorName;
        this.inputParamenters = inputParamenters;
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
        String body = "{\n";
        for (Statement statement : statements) {
            body += statement.toString() + "\n";
        }
        return body + "}\n";
    }

    public ImmutableArray<Statement> getStatements() { return statements; }
}
