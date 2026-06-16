/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation.Default;

/// A single variable declaration `T v` (optionally part of a `T v = e;` statement).
///
/// In a concrete program the variable is a [ProgramVariable]. In a taclet pattern the variable
/// position may instead be a program schema variable ([ProgramSV]); such a schematic declaration
/// is only used in a `\find` (matching binds the schema variable to the declared variable), it is
/// never produced by a `\replacewith`.
public class StatementVariableDeclaration implements Declaration, SolidityProgramElement {
    /// the declared variable in a concrete program, or `null` for a schematic declaration
    private final @Nullable ProgramVariable programVariable;
    /// the schema variable standing for the declared variable in a taclet, or `null` otherwise
    private final @Nullable ProgramSV schemaVariable;
    /// the declared type of a schematic declaration (kept for display only; not matched)
    private final @Nullable Object schemaType;

    public StatementVariableDeclaration(ProgramVariable programVariable) {
        this.programVariable = programVariable;
        this.schemaVariable = null;
        this.schemaType = null;
    }

    /// Creates a schematic declaration whose variable position is a program schema variable.
    ///
    /// @param type the declared type (for display only)
    /// @param schemaVariable the schema variable standing for the declared variable
    public StatementVariableDeclaration(@Nullable Object type, ProgramSV schemaVariable) {
        this.programVariable = null;
        this.schemaVariable = schemaVariable;
        this.schemaType = type;
    }

    public StatementVariableDeclaration(ExtList extList) {
        this.programVariable =
            Objects.requireNonNull(extList.removeFirstOccurrence(ProgramVariable.class));
        this.schemaVariable = null;
        this.schemaType = null;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0) {
            return schemaVariable != null ? schemaVariable
                    : Objects.requireNonNull(programVariable);
        }
        throw new IndexOutOfBoundsException(
            "Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        if (schemaVariable != null) {
            return (schemaType == null ? "" : schemaType + " ") + schemaVariable;
        }
        ProgramVariable pv = Objects.requireNonNull(programVariable);
        DataLocation dataLocation = pv.getDataLocation();
        String name = pv.name().toString();
        String type = pv.getType().toString();
        if (dataLocation == Default)
            return type + " " + name;
        return type + " " + dataLocation + " " + name;
    }

    public void visit(Visitor v) {
        v.performActionOnStatementVariableDeclaration(this);
    }

    /// @return the declared program variable for a concrete declaration; `null` for a schematic
    /// declaration (taclet pattern). Callers that only handle concrete programs may treat this as
    /// non-null.
    public ProgramVariable getProgramVariable() {
        return Objects.requireNonNull(programVariable,
            "schematic declaration has no concrete program variable");
    }
}
