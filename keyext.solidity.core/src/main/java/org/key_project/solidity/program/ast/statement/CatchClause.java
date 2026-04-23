/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.checkerframework.checker.nullness.qual.Nullable;

public class CatchClause implements SolidityProgramElement {
    enum Kind {
        Error, Panic, LowLevel, ALL;
    }

    private final Kind kind;
    private final ImmutableArray<StatementVariableDeclaration> declarations;
    private final Block body;

    // TODO: Make this field protected and in SolidityProgramElement
    private int hashCode = -1;

    public CatchClause(ImmutableArray<StatementVariableDeclaration> declarations, Block body) {
        this.declarations = declarations;
        this.body = body;
        if (declarations == null)
            this.kind = Kind.ALL;
        else {
            Type type = declarations.get(0).getProgramVariable().getType();
            if (type == PrimitiveType.UINT)
                this.kind = Kind.Panic;
            else if (type == PrimitiveType.STRING)
                this.kind = Kind.Error;
            else if (type == PrimitiveType.BYTES)
                this.kind = Kind.LowLevel;
            else
                throw new IllegalArgumentException(
                    "Unknown catch clause kind for declared catch variable " + declarations);
        }
    }

    public CatchClause(Block body) {
        this(null, body);
    }

    public CatchClause(ExtList children) {
        this(new ImmutableArray<>(children.collect(StatementVariableDeclaration.class)),
            children.get(Block.class));
    }

    public Kind getKind() {
        return kind;
    }

    public StatementVariableDeclaration getCatchDeclaration() {
        return declarations.get(0);
    }

    public Block getBody() {
        return body;
    }

    @Override
    public int getChildCount() {
        int count = declarations == null ? 0 : 1;
        count += body.getChildCount();
        return count;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnCatchClause(this);
    }

    @Override
    public SolidityProgramElement getChild(int index) {
        if (declarations != null)
            index -= 1;
        if (index == -1)
            return declarations.get(0);
        return body.getStatements().get(index);
    }

    // TODO: Move this to SolidityProgramElement and should be protected
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
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final CatchClause other = (CatchClause) obj;
        return Objects.equals(declarations, other.declarations) && Objects.equals(body, other.body);
    }

    @Override
    public String toString() {
        String catchString = "catch ";

        switch (kind) {
            case Error, Panic:
                catchString += kind.toString();
                break;
        }

        if (declarations != null) {
            catchString += "(" + declarations.stream().map(StatementVariableDeclaration::toString)
                    .collect(Collectors.joining(", "))
                + ")";
        }

        catchString += " " + body.toString();

        return catchString;
    }
}
