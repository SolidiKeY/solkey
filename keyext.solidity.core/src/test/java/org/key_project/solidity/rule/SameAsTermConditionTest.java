/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.SFunction;
import org.key_project.solidity.parser.ParserForTesting;
import org.key_project.solidity.parser.varcond.SameAsTermCondition;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.references.FieldReference;
import org.key_project.solidity.program.ast.references.TypeReference;
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
        Function existing = services.getNamespaces().functions().lookup(new Name(name));
        if (existing != null) {
            return existing;
        }
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

    @Test
    void bindsStructMemberPathToLogicList() {
        Sort listSort = services.getNamespaces().sorts().lookup(new Name("List"));
        assertNotNull(listSort, "the List sort must be available");
        Sort structSort = services.getNamespaces().sorts().lookup(new Name("Struct"));
        assertNotNull(structSort, "the Struct sort must be available");

        Function age = registerFieldConstant("age");
        Function cons = services.getNamespaces().functions().lookup(new Name("cons"));
        Function nil = services.getNamespaces().functions().lookup(new Name("nil"));
        assertNotNull(cons, "the cons list constructor must be available");
        assertNotNull(nil, "the nil list constructor must be available");

        FieldDeclaration ageField =
            new FieldDeclaration(new Name("age"), new TypeReference(new Name("uint")));
        StructDeclaration structType =
            new StructDeclaration(new Name("Person"), List.of(ageField), -1);
        ProgramVariable st = new ProgramVariable(new Name("st"),
            new KeYSolidityType(structType, structSort), DataLocation.Default);
        MemberExp memberPath = new MemberExp(st, ageField, st.getType());

        ProgramSV pathSV = SchemaVariableFactory.createProgramSV(new Name("path"),
            ProgramSVSort.EXPRESSION, false);
        TermSV pathTermSV = SchemaVariableFactory.createTermSV(new Name("pathTerm"), listSort);
        SameAsTermCondition pathCondition = new SameAsTermCondition(pathSV, pathTermSV);

        MatchConditions result = (MatchConditions) pathCondition.check(pathSV, memberPath,
            MatchConditions.EMPTY_MATCHCONDITIONS, services);
        assertNotNull(result, "member path should lower to a logic list");
        Object pathTerm =
            ((SVInstantiations) result.getInstantiations()).getInstantiation(pathTermSV);
        Term expectedPath = services.getTermBuilder().func(cons,
            services.getTermBuilder().func(age), services.getTermBuilder().func(nil));
        assertEquals(expectedPath, pathTerm, "st.age should lower to cons(age, nil)");
    }
}
