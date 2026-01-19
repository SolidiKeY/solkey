/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.INT;

public class StatementVariableReference extends Expression implements VariableReference {
    private final Name name;
    private final StatementVariableDeclaration stmVarDecl;

    // TODO: remove this class
    public StatementVariableReference(Name name,
            StatementVariableDeclaration stmVarDeclaration, Type type) {
        super(type);
        this.name = name;
        this.stmVarDecl = stmVarDeclaration;
    }

    public StatementVariableReference(ExtList children) {
        super(INT);
        this.name = new Name("");
        this.stmVarDecl = Objects .requireNonNull(children.removeFirstOccurrence(StatementVariableDeclaration.class));
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public SyntaxElement getChild(int n) {
        return stmVarDecl;
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    public void visit(Visitor v) {
        v.performActionOnStatementVariableReference(this);
    }
}
