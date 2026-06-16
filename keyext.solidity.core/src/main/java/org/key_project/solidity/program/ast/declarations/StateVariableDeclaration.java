/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.expressions.Expression;

import org.jspecify.annotations.Nullable;

/// A contract state-variable (field) declaration. This node is purely syntactic: it records the
/// field's name, Solidity type and visibility, plus the name under which the field's logic symbol
/// is registered ([#getFieldConstantName]). It deliberately holds **no** logic operator — the
/// field symbol is looked up lazily by the rule that moves a field access into the logic. See
/// [org.key_project.solidity.program.ast.references.FieldReference].
public class StateVariableDeclaration implements Declaration {

    private final Name name;
    private final KeYSolidityType type;
    /// the name under which the field's logic constant is registered (e.g. `Contract$balance`)
    private final Name fieldConstantName;
    private final @Nullable Expression initializer;
    private final Visibility visibility;

    public StateVariableDeclaration(Name name, KeYSolidityType type, Name fieldConstantName,
            @Nullable Expression initializer, Visibility visibility) {
        this.name = name;
        this.type = type;
        this.fieldConstantName = fieldConstantName;
        this.initializer = initializer;
        this.visibility = visibility;
    }

    public Name getName() {
        return name;
    }

    public KeYSolidityType getKeYSolidityType() {
        return type;
    }

    public @Nullable Type getType() {
        return type.getSolidityType();
    }

    public Name getFieldConstantName() {
        return fieldConstantName;
    }

    public @Nullable Expression getInitializer() {
        return initializer;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    // Syntax Element interface
    @Override
    public int getChildCount() {
        return initializer == null ? 0 : 1;
    }

    @Override
    public SolidityProgramElement getChild(int i) {
        if (i != 0 || initializer == null) {
            throw new IndexOutOfBoundsException(
                "Index should be 0 <= " + i + " < " + getChildCount());
        }
        return initializer;
    }

    // common interface
    public String toString() {
        Type solType = type.getSolidityType();
        String typeReference = solType == null ? "" : solType.name().toString();
        return List.of(typeReference, visibility.toString(), name.toString(),
            initializer != null ? " = " + initializer : "").stream()
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "))
            + ";";
    }
}
