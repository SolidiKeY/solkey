/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.ArrayList;
import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.PosInProgram;
import org.key_project.solidity.program.ProgramPrefix;
import org.key_project.solidity.program.ProgramPrefixUtil;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.jspecify.annotations.Nullable;

public class Block implements Statement, ProgramPrefix {

    private final ImmutableArray<Statement> statements;
    private final int prefixLength;
    private int hashcode = -1;

    public Block(ImmutableArray<Statement> statements) {
        this.statements = statements;
        prefixLength = ProgramPrefixUtil.computeEssentials(this).length();
    }

    public Block(List<Statement> statements) {
        this(new ImmutableArray<>(statements));
    }

    public Block(ExtList children) {
        this(new ImmutableArray<>(children.collect(Statement.class)));
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
        StringBuilder body = new StringBuilder("{\n");
        for (Statement statement : statements) {
            body.append(statement).append("\n");
        }
        return body.append("}\n").toString();
    }

    public ImmutableArray<Statement> getStatements() { return statements; }

    // These prefix queries run via computeEssentials(this) from the constructor, where the receiver
    // is only @UnknownInitialization; `statements` is already assigned at that point, so the calls
    // to getChildCount()/getChild() are safe even though the checker cannot prove it.
    @Override
    @SuppressWarnings("method.invocation.invalid")
    public boolean isPrefix(@UnknownInitialization Block this) {
        return getChildCount() != 0;
    }

    @Override
    @SuppressWarnings("method.invocation.invalid")
    public boolean hasNextPrefixElement(@UnknownInitialization Block this) {
        return getChildCount() != 0 && getChild(0) instanceof ProgramPrefix;
    }

    @Override
    @SuppressWarnings("method.invocation.invalid")
    public ProgramPrefix getNextPrefixElement(@UnknownInitialization Block this) {
        if (hasNextPrefixElement()) {
            return (ProgramPrefix) getChild(0);
        }
        throw new IndexOutOfBoundsException("No next prefix element " + this);
    }

    @Override
    public ProgramPrefix getLastPrefixElement() {
        return hasNextPrefixElement() ? getNextPrefixElement().getLastPrefixElement() : this;
    }

    @Override
    public ImmutableArray<ProgramPrefix> getPrefixElements() {
        return computePrefixElements(this);
    }

    @Override
    public PosInProgram getFirstActiveChildPos() {
        return PosInProgram.ZERO;
    }

    @Override
    public int getPrefixLength(@UnknownInitialization Block this) {
        return prefixLength;
    }

    /// computes the prefix elements for the given array of statment block
    public static ImmutableArray<ProgramPrefix> computePrefixElements(ProgramPrefix current) {
        final ArrayList<ProgramPrefix> prefix = new ArrayList<>();
        prefix.add(current);

        while (current.hasNextPrefixElement()) {
            current = current.getNextPrefixElement();
            prefix.add(current);
        }

        return new ImmutableArray<>(prefix);
    }

    public void visit(Visitor v) {
        v.performActionOnBlock(this);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Block that))
            return false;
        return statements.equals(that.statements);
    }

    @Override
    public int hashCode() {
        if (hashcode == -1) {
            int hash = computeHashCode();
            hashcode = hash == -1 ? 0 : hash;
        }
        return hashcode;
    }
}
