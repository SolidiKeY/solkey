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
    /// Sub-sort of `Field` for mapping members (preserved by `delete`).
    public static final Name MAP_FIELD_SORT_NAME = new Name("MapField");
    /// Sub-sort of `Field` for struct/array reference members (recursed by `delete`).
    public static final Name ID_FIELD_SORT_NAME = new Name("IdField");
    /// Sub-sort of `Field` for primitive-valued members (reset to their default by `delete`).
    public static final Name PRIM_FIELD_SORT_NAME = new Name("PrimField");
    /// The contract-storage program variable, declared in the struct theory `.key` files.
    public static final Name STORAGE_NAME = new Name("storage");
    public static final String FIELD_SEPARATOR = "$";

    private final Function mt;
    private final Sort fieldSort;
    private final Sort mapFieldSort;
    private final Sort idFieldSort;
    private final Sort primFieldSort;
    private final ProgramVariable storage;

    public StructLDT(Services services) {
        super(NAME, services);

        mt = addFunction(services, "mt");
        storage = services.getNamespaces().programVariables().lookup(STORAGE_NAME);
        fieldSort = services.getNamespaces().sorts().lookup(FIELD_SORT_NAME);
        mapFieldSort = services.getNamespaces().sorts().lookup(MAP_FIELD_SORT_NAME);
        idFieldSort = services.getNamespaces().sorts().lookup(ID_FIELD_SORT_NAME);
        primFieldSort = services.getNamespaces().sorts().lookup(PRIM_FIELD_SORT_NAME);
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

    /// Sub-sort of `Field` for mapping members, or `null` if the struct theory is not loaded.
    public Sort getMapFieldSort() {
        return mapFieldSort;
    }

    /// Sub-sort of `Field` for struct/array reference members, or `null` if not loaded.
    public Sort getIdFieldSort() {
        return idFieldSort;
    }

    /// Sub-sort of `Field` for primitive-valued members, or `null` if not loaded.
    public Sort getPrimFieldSort() {
        return primFieldSort;
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
