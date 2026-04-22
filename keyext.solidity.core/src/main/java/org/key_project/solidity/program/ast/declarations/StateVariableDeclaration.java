/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.stream.Collectors;

import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

public class StateVariableDeclaration extends DeclarationClass {

    private final @Nullable Expression initializer;
    private final Visibility visibility;

    public ProgramVariable getProgramVariable() {
        return programVariable;
    }

    private ProgramVariable programVariable;

    public StateVariableDeclaration(ProgramVariable programVariable,
            @Nullable Expression initializer, Visibility visibility) {
        super(new ImmutableArray<>());
        this.programVariable = programVariable;
        this.initializer = initializer;
        this.visibility = visibility;
    }

    public @Nullable Expression getInitializer() {
        return initializer;
    }

    // Syntax Element interface
    @Override
    public int getChildCount() {
        return 1 + (initializer == null ? 0 : 1);
    }

    @Override
    public SolidityProgramElement getChild(int i) {
        if (i < 0 || i >= getChildCount()) {
            throw new IndexOutOfBoundsException("Index should be 0 <= " + i + " < " + getChildCount());
        }
        if (i == 0 && programVariable != null) {
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
