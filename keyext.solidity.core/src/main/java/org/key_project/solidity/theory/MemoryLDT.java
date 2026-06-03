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
import org.key_project.solidity.program.ast.expressions.operators.BinaryExpression;

import org.jspecify.annotations.Nullable;

public class MemoryLDT extends LDT {
    public static final Name NAME = new Name("Memory");

    private final Function mtMem;
    private final ProgramVariable memory;
    private final Sort identitySort;

    public MemoryLDT(Services services) {
        super(NAME, services);
        identitySort = services.getNamespaces().sorts().lookup("Identity");
        mtMem = addFunction(services, "mtMem");
        memory = services.getNamespaces().programVariables().lookup(new Name("memory"));
    }

    // -------------------------------------------------------------------------
    // internal methods
    // -------------------------------------------------------------------------


    // -------------------------------------------------------------------------
    // public interface
    // -------------------------------------------------------------------------

    public Function getEmptyMemory() {
        return mtMem;
    }

    public ProgramVariable getBaseMemory() {
        return memory;
    }

    @Override
    public @Nullable Term translateLiteral(Literal lit, Services services) {
        return null;
    }

    @Override
    public @Nullable Function getFunctionFor(BinaryExpression op, Services services) {
        return null;
    }

    @Override
    public boolean isResponsible(BinaryExpression op, Term[] subs,
            Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(BinaryExpression op, Term sub, Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(BinaryExpression op, Term left, Term right,
            Services services) {
        return false;
    }

    @Override
    public @Nullable Function getFunctionFor(String operationName, Services services) {
        return switch (operationName) {
            case "mtMem" -> getEmptyMemory();
            default -> null;
        };
    }

    public Sort getIdentitySort() {
        return identitySort;
    }
}
