/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

public class StateVariableDeclaration extends DeclarationClass {

    private final @Nullable Expression initializer;
    private final Visibility visibility;

    public ProgramVariable getProgramVariable() {
        return programVariable;
    }

    private final ProgramVariable programVariable;

    public StateVariableDeclaration(ProgramVariable programVariable,
            @Nullable Expression initializer, Visibility visibility) {
        super(new ImmutableArray<>());
        this.programVariable = programVariable;
        this.initializer = initializer;
        this.visibility = visibility;
    }

    public StateVariableDeclaration(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(ImmutableArray.class)));
        this.programVariable =
            Objects.requireNonNull(children.removeFirstOccurrence(ProgramVariable.class));
        this.initializer = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.visibility = Objects.requireNonNull(children.removeFirstOccurrence(Visibility.class));
    }

    public @Nullable Expression getInitializer() {
        return initializer;
    }

    // Syntax Element interface
    @Override
    public int getChildCount() {
        return initializer == null ? 1 : 2;
    }

    @Override
    public SolidityProgramElement getChild(int i) {
        if (i < 0 || i >= getChildCount()) {
            throw new IndexOutOfBoundsException(
                "No child at index " + i + " in " + programVariable.name());
        }
        if (i == 0) {
            return programVariable;
        }
        return initializer;
    }


    // common interface
    public String toString() {
        String name = programVariable.name().toString();
        String typeReference =
            programVariable.getType() == null ? "" : programVariable.getType().name().toString();
        return List.of(typeReference, visibility.toString(), name,
            initializer != null ? " = " + initializer : "").stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "))
            + ";";
    }
}
