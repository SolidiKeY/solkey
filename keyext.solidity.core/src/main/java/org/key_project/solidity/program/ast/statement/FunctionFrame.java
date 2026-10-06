/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.PosInProgram;
import org.key_project.solidity.program.ProgramPrefix;
import org.key_project.solidity.program.ProgramPrefixUtil;
import org.key_project.solidity.program.ast.HashCachingElement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.jspecify.annotations.Nullable;

public class FunctionFrame extends HashCachingElement implements Statement, ProgramPrefix {

    private final ImmutableArray<Statement> statements;
    private final int prefixLength;

    public FunctionFrame(ImmutableArray<Statement> statements) {
        this.statements = statements;
        prefixLength = ProgramPrefixUtil.computeEssentials(this).length();
    }

    public FunctionFrame(List<Statement> statements) {
        this(new ImmutableArray<>(statements));
    }

    public FunctionFrame(ExtList children) {
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

    public ImmutableArray<Statement> getStatements() {
        return statements;
    }

    @Override
    public String toString() {
        StringBuilder body = new StringBuilder("function-frame {\n");
        for (Statement statement : statements) {
            body.append(statement).append("\n");
        }
        return body.append("}\n").toString();
    }

    @Override
    @SuppressWarnings("method.invocation.invalid")
    public boolean isPrefix(@UnknownInitialization FunctionFrame this) {
        return getChildCount() != 0;
    }

    @Override
    @SuppressWarnings("method.invocation.invalid")
    public boolean hasNextPrefixElement(@UnknownInitialization FunctionFrame this) {
        return getChildCount() != 0 && getChild(0) instanceof ProgramPrefix;
    }

    @Override
    @SuppressWarnings("method.invocation.invalid")
    public ProgramPrefix getNextPrefixElement(@UnknownInitialization FunctionFrame this) {
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
        return Block.computePrefixElements(this);
    }

    @Override
    public PosInProgram getFirstActiveChildPos() {
        return PosInProgram.ZERO;
    }

    @Override
    public int getPrefixLength(@UnknownInitialization FunctionFrame this) {
        return prefixLength;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnFunctionFrame(this);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FunctionFrame that)) {
            return false;
        }
        return statements.equals(that.statements);
    }
}
