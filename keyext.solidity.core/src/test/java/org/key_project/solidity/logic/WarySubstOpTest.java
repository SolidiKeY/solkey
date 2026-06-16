/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.BoundVariable;
import org.key_project.solidity.logic.op.LogicVariable;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.SFunction;
import org.key_project.solidity.logic.op.WarySubstOp;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.parser.ParserForTesting;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.statement.Block;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.UINT;

/// Tests the wary first-order substitution: a *non-rigid* replacement (a program variable) must
/// not be pushed across a modality, but a *rigid* one is pushed through, and ordinary (no-modality)
/// substitution works.
public class WarySubstOpTest {

    private Services services;
    private TermBuilder tb;
    private Sort mySort;
    private BoundVariable x; // the substituted variable
    private Term lvX; // its de Bruijn occurrence (index 1) in the body
    private ProgramVariable v; // a non-rigid replacement
    private Term vTerm;
    private Term cTerm; // a rigid replacement

    @BeforeEach
    void setUp() {
        services = ParserForTesting.load().getServices();
        tb = services.getTermBuilder();
        mySort = new SortImpl(new Name("MySort"));
        services.getNamespaces().sorts().addSafely(mySort);

        x = new BoundVariable(new Name("x"), mySort);
        lvX = tb.var(LogicVariable.create(1, mySort));

        v = new ProgramVariable(new Name("v"),
            new KeYSolidityType(UINT, mySort), null);
        vTerm = tb.var(v);

        SFunction c = new SFunction(new Name("c"), mySort, true, false);
        services.getNamespaces().functions().addSafely(c);
        cTerm = tb.func(c);
    }

    private Term emptyModality(Term post) {
        return tb.dia(new SolidityBlock(new Block(List.of())), post);
    }

    @Test
    void nonRigidReplacementIsNotPushedAcrossModality() {
        // {x <- v} <{}>(x = v), v a program variable (non-rigid)
        Term body = emptyModality(tb.equals(lvX, vTerm));
        Term substTerm = tb.subst(WarySubstOp.SUBST, x, vTerm, body);

        Term result = WarySubstOp.SUBST.apply(substTerm, tb);

        // the substitution is left as a residual — v is not pushed inside the modality
        assertEquals(substTerm, result,
            "a non-rigid replacement must not be pushed across a modality");
    }

    @Test
    void nonRigidReplacementIsNotPushedIntoUpdate() {
        // {x <- v}( {y := x}(x = v) ), v a program variable (non-rigid)
        ProgramVariable y = new ProgramVariable(new Name("y"),
            new KeYSolidityType(UINT, mySort), null);
        Term update = tb.elementary(y, lvX); // y := x
        Term body = tb.apply(update, tb.equals(lvX, vTerm)); // {y := x}(x = v)
        Term substTerm = tb.subst(WarySubstOp.SUBST, x, vTerm, body);

        Term result = WarySubstOp.SUBST.apply(substTerm, tb);

        // conservative: v is not pushed into the update application at all (the
        // update-simplification taclets handle that); the substitution is left as a residual
        assertEquals(substTerm, result,
            "a non-rigid replacement must not be pushed into an update application");
    }

    @Test
    void ordinarySubstitutionWithoutModalityIsApplied() {
        // {x <- v}(x = v) with no modality: x is replaced by v
        Term body = tb.equals(lvX, vTerm);
        Term substTerm = tb.subst(WarySubstOp.SUBST, x, vTerm, body);

        Term result = WarySubstOp.SUBST.apply(substTerm, tb);

        assertEquals(tb.equals(vTerm, vTerm), result,
            "without a modality the substitution is carried out");
    }

    @Test
    void rigidReplacementIsPushedThroughModality() {
        // {x <- c} <{}>(x = c), c a rigid constant: c IS pushed inside
        Term body = emptyModality(tb.equals(lvX, cTerm));
        Term substTerm = tb.subst(WarySubstOp.SUBST, x, cTerm, body);

        Term result = WarySubstOp.SUBST.apply(substTerm, tb);

        assertEquals(emptyModality(tb.equals(cTerm, cTerm)), result,
            "a rigid replacement is pushed through the modality");
    }
}
