/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.SFunction;
import org.key_project.solidity.parser.ParserForTesting;
import org.key_project.solidity.parser.varcond.SameAsTermCondition;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.references.FieldReference;
import org.key_project.solidity.rule.matching.inst.MatchConditions;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.rule.sv.SchemaVariableFactory;
import org.key_project.solidity.rule.sv.TermSV;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Unit tests for the `\sameAsTerm(v, s)` variable condition ([SameAsTermCondition]): it must
/// bind a free term schema variable `s` to the logic conversion of the program schema variable
/// `v`, and check equality when `s` is already bound.
public class SameAsTermConditionTest {

    private Services services;
    private Sort fieldSort;
    private ProgramSV progSV;
    private TermSV termSV;
    private FieldReference fieldRef;
    private Term expectedValue;
    private SameAsTermCondition condition;

    private Function registerFieldConstant(String name) {
        SFunction c = new SFunction(new Name(name), fieldSort, true, true);
        services.getNamespaces().functions().addSafely(c);
        return c;
    }

    @BeforeEach
    void setUp() {
        services = ParserForTesting.load().getServices();
        fieldSort = services.getNamespaces().sorts().lookup(new Name("Field"));
        assertNotNull(fieldSort, "the Field sort must be available");

        Function balance = registerFieldConstant("Bank$balance");
        StateVariableDeclaration decl = new StateVariableDeclaration(new Name("balance"),
            new KeYSolidityType(fieldSort), new Name("Bank$balance"), null, Visibility.Public);
        fieldRef = new FieldReference(decl, decl.getType());
        expectedValue = services.getTermBuilder().func(balance);

        progSV = SchemaVariableFactory.createProgramSV(new Name("v"),
            ProgramSVSort.FIELD_REFERENCE, false);
        termSV = SchemaVariableFactory.createTermSV(new Name("s"), fieldSort);
        condition = new SameAsTermCondition(progSV, termSV);
    }

    private MatchConditions bind(MatchConditions mc, org.key_project.logic.op.sv.SchemaVariable sv,
            Term t) {
        SVInstantiations inst = (SVInstantiations) mc.getInstantiations();
        return mc.setInstantiations(inst.add(sv, t, services));
    }

    @Test
    void bindsFreeTermVariableToConversionOfProgramVariable() {
        MatchConditions result = (MatchConditions) condition.check(progSV, fieldRef,
            MatchConditions.EMPTY_MATCHCONDITIONS, services);
        assertNotNull(result, "condition should hold and bind the term variable");
        Object sInst = ((SVInstantiations) result.getInstantiations()).getInstantiation(termSV);
        assertEquals(expectedValue, sInst,
            "s must be instantiated with the field's logic constant");
    }

    @Test
    void acceptsMatchingAlreadyBoundTermVariable() {
        MatchConditions bound = bind(MatchConditions.EMPTY_MATCHCONDITIONS, termSV, expectedValue);
        var result = condition.check(progSV, fieldRef, bound, services);
        assertSame(bound, result, "an already-equal binding of s should pass unchanged");
    }

    @Test
    void rejectsConflictingAlreadyBoundTermVariable() {
        Function other = registerFieldConstant("Bank$owner");
        Term otherValue = services.getTermBuilder().func(other);
        MatchConditions bound = bind(MatchConditions.EMPTY_MATCHCONDITIONS, termSV, otherValue);
        var result = condition.check(progSV, fieldRef, bound, services);
        assertNull(result, "a conflicting binding of s must make the condition fail");
    }

    @Test
    void ignoresUnrelatedSchemaVariables() {
        TermSV unrelated = SchemaVariableFactory.createTermSV(new Name("x"), fieldSort);
        var result = condition.check(unrelated, expectedValue,
            MatchConditions.EMPTY_MATCHCONDITIONS, services);
        assertSame(MatchConditions.EMPTY_MATCHCONDITIONS, result,
            "the condition should not react to unrelated schema variables");
    }
}
