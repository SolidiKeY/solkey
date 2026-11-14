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
import org.key_project.solidity.strategy.quantifierHeuristics.Substitution;
import org.key_project.util.collection.DefaultImmutableMap;
import org.key_project.util.collection.ImmutableMap;

import org.jspecify.annotations.NonNull;
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

    private Term lv(int index) {
        return tb.var(LogicVariable.create(index, mySort));
    }

    @Test
    void substLowersVariablesBoundFurtherOut() {
        Term body = tb.equals(lvX, lv(2));
        Term result = new Subst(cTerm, tb).apply(body);

        assertEquals(tb.equals(cTerm, lv(1)), result,
            "the variable bound outside the eliminated binder moves one index down");
    }

    @Test
    void substBelowBinderShiftsReplacementAndLowersOuterVariables() {
        BoundVariable y = new BoundVariable(new Name("y"), mySort);
        Term body = tb.all(y, tb.and(tb.equals(lv(2), lv(1)), tb.equals(lv(3), lv(1))));
        Term result = new Subst(lv(1), tb).apply(body);

        assertEquals(tb.all(y, tb.and(tb.equals(lv(2), lv(1)), tb.equals(lv(2), lv(1)))),
            result, "below a binder the replacement is shifted and outer indices are lowered");
    }

    @Test
    void substOfInnerTargetKeepsLowerIndices() {
        Term body = tb.and(tb.equals(lv(1), lv(2)), tb.equals(lv(3), cTerm));
        Term result = new Subst(cTerm, 2, tb).apply(body);

        assertEquals(tb.and(tb.equals(lv(1), cTerm), tb.equals(lv(2), cTerm)), result,
            "indices below the target stay, the target is replaced, indices above move down");
    }

    @Test
    void substOfInnerTargetShiftsReplacementPastInnerBinders() {
        Term body = tb.and(tb.equals(lv(1), lv(2)), tb.equals(lv(3), cTerm));
        Term result = new Subst(lv(1), 2, tb).apply(body);

        assertEquals(tb.and(tb.equals(lv(1), lv(2)), tb.equals(lv(2), cTerm)), result,
            "a replacement from outside the target binder skips the binders inside it");
    }

    @Test
    void substitutionInstantiatesEveryStrippedQuantifier() {
        SFunction d = new SFunction(new Name("d"), mySort, true, false);
        services.getNamespaces().functions().addSafely(d);
        Term dTerm = tb.func(d);
        ImmutableMap<@NonNull LogicVariable, Term> map = DefaultImmutableMap
                .<@NonNull LogicVariable, Term>nilMap()
                .put(LogicVariable.create(1, mySort), cTerm)
                .put(LogicVariable.create(2, mySort), dTerm);

        Term result = new Substitution(map).apply(tb.equals(lv(2), lv(1)), services);

        assertEquals(tb.equals(dTerm, cTerm), result,
            "each index gets the instance of its own quantifier");
    }

    @Test
    void shiftRaisesEveryFreeVariable() {
        BoundVariable y = new BoundVariable(new Name("y"), mySort);
        Term t = tb.and(tb.equals(lv(1), lv(2)), tb.all(y, tb.equals(lv(1), lv(3))));
        Term result = new Shift(2, tb).apply(t);

        assertEquals(tb.and(tb.equals(lv(3), lv(4)), tb.all(y, tb.equals(lv(1), lv(5)))),
            result, "every free index is raised, the bound one is kept");
    }

    @Test
    void nonRigidSubstitutionLowersVariablesBoundFurtherOut() {
        Term body = tb.equals(lvX, lv(2));
        Term substTerm = tb.subst(WarySubstOp.SUBST, x, vTerm, body);

        Term result = WarySubstOp.SUBST.apply(substTerm, tb);

        assertEquals(tb.equals(vTerm, lv(1)), result,
            "a completed non-rigid substitution removes the binder from the context");
    }
}
