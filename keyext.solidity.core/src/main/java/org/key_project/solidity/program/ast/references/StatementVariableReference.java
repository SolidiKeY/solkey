/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.visitor.Visitor;

public class StatementVariableReference extends VariableReference {
    private final int id;
    private final Name name;
    private final StatementVariableDeclaration stmVarDecl;

    public StatementVariableReference(int id, Name name,
            StatementVariableDeclaration stmVarDeclaration, Type type) {
        super(type);
        this.id = id;
        this.name = name;
        this.stmVarDecl = stmVarDeclaration;
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public Declaration getDeclaration() {
        return stmVarDecl;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    public void visit(Visitor v){
        v.performActionOnStatementVariableReference(this);
    }
}
