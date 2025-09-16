/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.AbstractSortedOperator;
import org.key_project.logic.op.Modifier;
import org.key_project.logic.op.UpdateableOperator;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.SoliditiyExpression;

import java.util.Objects;

public class ProgramVariable extends AbstractSortedOperator
        implements SoliditiyExpression, UpdateableOperator, IProgramVariable {
    private final KeYSolidityType type;

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

    public KeYSolidityType getKeYSolidityType() {
        return type;
    }

    @Override
    public Type getType() {
        return type.getSolidityType();
    }

    /// TODO: implement
//    @Override
//    public void visit(Visitor v) {
//        v.performActionOnProgramVariable(this);
//    }
//
//    @Override
//    public Type type(Services services) {
//        return type.getSolidityType();
//    }
}
