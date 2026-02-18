/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.Objects;

import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.checkerframework.checker.nullness.qual.Nullable;

public class CatchClause implements SolidityProgramElement {
    enum Kind {
        Error, Panic, LowLevel, ALL;
    }

    private final Kind kind;
    private final StatementVariableDeclaration declaration;
    private final Block body;

    // cache for hashcode
    private int hashCode = -1;

    public CatchClause(StatementVariableDeclaration declaration, Block body) {
        this.declaration = declaration;
        this.body = body;
        if (declaration == null) {
            this.kind = Kind.ALL;
        } else {
            Type type = declaration.getProgramVariable().getType();
            if (type == PrimitiveType.UINT) {
                this.kind = Kind.Panic;
            } else if (type == PrimitiveType.STRING) {
                this.kind = Kind.Error;
            } else if (type == PrimitiveType.BYTES) {
                this.kind = Kind.LowLevel;
            } else {
                throw new IllegalArgumentException(
                    "Unknown catch clause kind for declared catch variable " + declaration);
            }
        }
    }

    public CatchClause(Block body) {
        this(null, body);
    }

    public CatchClause(ExtList children) {
        this(children.get(StatementVariableDeclaration.class), children.get(Block.class));
    }

    public Kind getKind() {
        return kind;
    }

    public StatementVariableDeclaration getCatchDeclaration() {
        return declaration;
    }

    public Block getBody() {
        return body;
    }

    @Override
    public int getChildCount() {
        int count = declaration == null ? 0 : 1;
        count += body.getChildCount();
        return count;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnCatchClause(this);
    }

    @Override
    public SolidityProgramElement getChild(int index) {
        if (declaration != null && index == 0) {
            return declaration;
        }
        index -= 1;
        return body.getStatements().get(index);
    }

    @Override
    public int hashCode() {
        if (hashCode == -1) {
            int hash = computeHashCode();
            hashCode = hash == -1 ? 0 : hash;
        }
        return hashCode;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final CatchClause other = (CatchClause) obj;
        return Objects.equals(declaration, other.declaration) && Objects.equals(body, other.body);
    }

    @Override
    public String toString() {
        String catchString = "catch ";

        switch (kind) {
            case Error, Panic:
                catchString += kind.toString();
                break;
        }

        if (declaration != null) {
            catchString += "(" + declaration + ")";
        }

        catchString += " " + body.toString();

        return catchString;
    }
}
