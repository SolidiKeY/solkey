/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.theory;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.expressions.literals.Literal;
import org.key_project.solidity.program.ast.expressions.operators.BinaryOperator;

import org.jspecify.annotations.Nullable;

public class StructLDT extends LDT {
    public static final Name NAME = new Name("Struct");

    private final Function mt;
    private final ProgramVariable storage;

    public StructLDT(Services services) {
        super(NAME, services);
        mt = addFunction(services, "mt");
        storage = services.getNamespaces().programVariables().lookup(new Name("storage"));
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



    @Override
    public @Nullable Term translateLiteral(Literal lit, Services services) {
        return null;
    }

    @Override
    public @Nullable Function getFunctionFor(BinaryOperator op, Services services) {
        return null;
    }

    @Override
    public boolean isResponsible(BinaryOperator op, Term[] subs,
            Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(BinaryOperator op, Term sub, Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(BinaryOperator op, Term left, Term right,
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
