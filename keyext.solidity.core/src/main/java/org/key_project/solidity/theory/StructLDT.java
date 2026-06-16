/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.theory;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.expressions.literals.Literal;
import org.key_project.solidity.program.ast.expressions.operators.OperatorExpression;

import org.jspecify.annotations.Nullable;

public class StructLDT extends LDT {
    public static final Name NAME = new Name("Struct");
    /// The sort of struct field constants, declared in the struct theory `.key` files.
    public static final Name FIELD_SORT_NAME = new Name("Field");
    public static final String FIELD_SEPARATOR = "$";

    private final Function mt;
    private final Sort fieldSort;
    private final ProgramVariable storage;

    public StructLDT(Services services) {
        super(NAME, services);

        mt = addFunction(services, "mt");
        storage = services.getNamespaces().programVariables().lookup(new Name("storage"));
        fieldSort = services.getNamespaces().sorts().lookup(FIELD_SORT_NAME);
    }

    // -------------------------------------------------------------------------
    // internal methods
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // public interface
    // -------------------------------------------------------------------------



    public Function getEmptyStorage() {
        return mt;
    }

    public ProgramVariable getStorage() {
        return storage;
    }

    public Sort getFieldSort() {
        return fieldSort;
    }


    @Override
    public @Nullable Term translateLiteral(Literal lit, Services services) {
        return null;
    }

    @Override
    public @Nullable Function getFunctionFor(OperatorExpression op, Services services) {
        return null;
    }

    @Override
    public boolean isResponsible(OperatorExpression op, Term[] subs,
            Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(OperatorExpression op, Term sub, Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(OperatorExpression op, Term left, Term right,
            Services services) {
        return false;
    }

    @Override
    public @Nullable Function getFunctionFor(String operationName, Services services) {
        return switch (operationName) {
            case "mt" -> getEmptyStorage();
            default -> null;
        };
    }

}
