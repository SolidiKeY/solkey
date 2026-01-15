/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class StatementVariableReference extends VariableReference {
    private final int id;
    private final Name name;
    private final StatementVariableDeclaration stmVarDecl;

    // TODO: remove this class
    public StatementVariableReference(int id, Name name,
            StatementVariableDeclaration stmVarDeclaration, Type type) {
        super(type);
        this.id = id;
        this.name = name;
        this.stmVarDecl = stmVarDeclaration;
    }

    public StatementVariableReference(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.id = Objects.requireNonNull(children.removeFirstOccurrence(int.class));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
        this.stmVarDecl = Objects
                .requireNonNull(children.removeFirstOccurrence(StatementVariableDeclaration.class));
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

    public void visit(Visitor v) {
        v.performActionOnStatementVariableReference(this);
    }
}
