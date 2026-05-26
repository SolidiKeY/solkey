/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.PosInProgram;
import org.key_project.solidity.program.ProgramPrefix;
import org.key_project.solidity.program.ProgramPrefixUtil;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class Block implements Statement, ProgramPrefix {

    private final @NonNull ImmutableArray<Statement> statements;
    private final int prefixLength;

    public Block(ImmutableArray<Statement> statements) {
        this.statements = statements;
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        prefixLength = info.length();
    }

    public Block(List<Statement> statements) {
        this.statements = new ImmutableArray<>(statements);
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        prefixLength = info.length();
    }

    public Block(ExtList children) {
        this.statements = new ImmutableArray<>(children.collect(Statement.class));
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        prefixLength = info.length();
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
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

    public @NonNull ImmutableArray<Statement> getStatements() { return statements; }

    @Override
    public boolean isPrefix(@UnknownInitialization Block this) {
        return getChildCount() != 0;
    }

    @Override
    public boolean hasNextPrefixElement(@UnknownInitialization Block this) {
        return getChildCount() != 0 && getChild(0) instanceof ProgramPrefix;
    }

    @Override
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
}
