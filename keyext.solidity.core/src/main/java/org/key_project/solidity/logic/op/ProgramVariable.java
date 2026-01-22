/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.AbstractSortedOperator;
import org.key_project.logic.op.Modifier;
import org.key_project.logic.op.UpdateableOperator;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;


/**
 * function ... {
 * uint a; // use programvariable as child of the declaration
 * a = 5; // a on left hand side must be a child of type ProgramVariable
 * }
 */

public class ProgramVariable extends AbstractSortedOperator
        implements Expression, UpdateableOperator, IProgramVariable {
    private final KeYSolidityType type;
    // private final DataLocation dataLocation;

    public ProgramVariable(Name name, Sort s, KeYSolidityType type) {
        super(name, s, Modifier.NONE);
        this.type = type;
    }

    public ProgramVariable(Name name, KeYSolidityType type) {
        this(name, Objects.requireNonNull(type.getSort(), name.toString()), type);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Program variable does not have a child");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnProgramVariable(this);
    }

    public KeYSolidityType getKeYSolidityType() {
        return type;
    }

    @Override
    public Type getType() {
        return type.getSolidityType();
    }

    // TODO: Verify why this is needed
    @Override
    public int hashCode() {
        return Objects.hash(name().hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Same memory address
        if (obj == null || getClass() != obj.getClass()) return false; // Null or different class

        ProgramVariable prog = (ProgramVariable) obj;
        return this.name().toString().equals(prog.name().toString());
    }

    /// TODO: implement
    // @Override
    // public void visit(Visitor v) {
    // v.performActionOnProgramVariable(this);
    // }
    //
    // @Override
    // public Type type(Services services) {
    // return type.getSolidityType();
    // }
}
