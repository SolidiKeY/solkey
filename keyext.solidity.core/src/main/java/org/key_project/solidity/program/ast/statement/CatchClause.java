/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.key_project.solidity.program.ast.HashCachingElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.Nullable;

public class CatchClause extends HashCachingElement {
    public enum Kind {
        Error, Panic, Other;

        public static Kind fromName(@Nullable String name) {
            if (name == null) {
                return Other;
            }
            return switch (name) {
                case "Error" -> Error;
                case "Panic" -> Panic;
                default -> throw new IllegalArgumentException("Unknown catch clause " + name);
            };
        }
    }

    private final Kind kind;
    private final Statement body;

    public CatchClause(Kind kind, Statement body) {
        this.kind = kind;
        this.body = body;
    }

    public CatchClause(Kind kind, ExtList children) {
        this(kind, Objects.requireNonNull(children.get(Statement.class)));
    }

    public static CatchClause of(Kind kind, @Nullable StatementVariableDeclaration parameter,
            Statement body) {
        return new CatchClause(kind, declaring(parameter == null ? List.of() : List.of(parameter),
            body));
    }

    static Statement declaring(List<StatementVariableDeclaration> variables, Statement body) {
        if (variables.isEmpty()) {
            return body;
        }
        List<Statement> statements = new ArrayList<>();
        for (StatementVariableDeclaration variable : variables) {
            statements.add(new DeclarationStatement(List.<Declaration>of(variable), null));
        }
        if (body instanceof Block block) {
            block.getStatements().forEach(statements::add);
        } else {
            statements.add(body);
        }
        return new Block(statements);
    }

    public Kind getKind() {
        return kind;
    }

    public Statement getBody() {
        return body;
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public SolidityProgramElement getChild(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException(index);
        }
        return body;
    }

    @Override
    public boolean matchesHead(SolidityProgramElement src) {
        return ((CatchClause) src).kind == kind;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnCatchClause(this);
    }

    @Override
    public int computeHashCode() {
        return Objects.hash(kind, body);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final CatchClause other = (CatchClause) obj;
        return kind == other.kind && Objects.equals(body, other.body);
    }

    @Override
    public String toString() {
        String head = kind == Kind.Other ? "catch " : "catch " + kind + " ";
        return head + body;
    }
}
