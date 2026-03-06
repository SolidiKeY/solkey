/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import java.util.HashMap;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.AbstractSortedOperator;
import org.key_project.logic.op.Modifier;
import org.key_project.logic.op.UpdateableOperator;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
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
        implements Expression, UpdateableOperator, IProgramVariable, Resolver {
    private KeYSolidityType type;
    private int contractId = -1;
    // private final DataLocation dataLocation;

    public ProgramVariable(Name name, Sort s, KeYSolidityType type) {
        super(name, s, Modifier.NONE);
        this.type = type;
    }

    public ProgramVariable(Name name, int contractId) {
        super(name, null, Modifier.NONE);
        this.type = null;
        this.contractId = contractId;
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
        return type == null ? null : type.getSolidityType();
    }

    @Override
    public void resolve(HashMap<Integer, Declaration> id2Name) {
        if (contractId != -1) {
            Type type = (Type) id2Name.get(contractId);
            Sort sort = new SortImpl(type.name(), false);
            this.sort = sort;
            this.type = new KeYSolidityType(type, sort);
        }
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
