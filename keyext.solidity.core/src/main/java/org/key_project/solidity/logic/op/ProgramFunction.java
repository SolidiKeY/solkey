/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.key_project.logic.op.Function;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.SourceData;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.solidity.rule.matching.inst.MatchConditions;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;

import java.util.Objects;

public class ProgramFunction extends ObserverFunction implements SolidityProgramElement {
    /// The referenced function.
    private final @NonNull FunctionDeclaration function;

    private final @NonNull KeYSolidityType returnType;

    public ProgramFunction(FunctionDeclaration function, KeYSolidityType returnType) {
        super(function.getName().toString(), Objects.requireNonNull(returnType.getSort()), returnType,
            getParamTypes(function));
        this.function = function;
        this.returnType = returnType;
    }

    // -------------------------------------------------------------------------
    // internal methods
    // -------------------------------------------------------------------------

    /// Get the rusty types of the parameters required by the function fn.
    ///
    /// @param fn some function declaration
    /// @return java types of the parameters required by fn
    private static ImmutableArray<KeYSolidityType> getParamTypes(FunctionDeclaration fn) {
        throw new RuntimeException("Not implemented yet");
    }

    public @NonNull FunctionDeclaration getFunction() {
        return function;
    }

    public Block getBody() {
        return function.getBody();
    }


    @Override
    public void visit(Visitor v) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public @Nullable MatchConditions match(SourceData source, @Nullable MatchConditions matchCond) {
        throw new RuntimeException("Not implemented yet");
    }

    public ImmutableList<ProgramVariable> collectParameters() {
        throw new RuntimeException("Not implemented yet");
    }

    public static ImmutableList<ProgramVariable> collectParameters(Function function) {
       throw new RuntimeException("Not implemented yet");
    }
}
