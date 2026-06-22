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
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
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
        Sort listSort = services.getNamespaces().sorts().lookup(new Name("List"));
        assertNotNull(listSort, "the List sort must be available");

        Function balance = registerFieldConstant("Bank$balance");
        StateVariableDeclaration decl = new StateVariableDeclaration(new Name("balance"),
            new KeYSolidityType(fieldSort), new Name("Bank$balance"), null, Visibility.Public);
        fieldRef = new FieldReference(decl, decl.getType());
        // With unified path representation, FieldReference now converts to cons(field, nil)
        Term fieldTerm = services.getTermBuilder().func(balance);
        expectedValue = list(fieldTerm);

        progSV = SchemaVariableFactory.createProgramSV(new Name("v"),
            ProgramSVSort.FIELD_REFERENCE, false);
        // Term SV must now be List-sorted since FieldReference converts to a List
        termSV = SchemaVariableFactory.createTermSV(new Name("s"), listSort);
        condition = new SameAsTermCondition(progSV, termSV);
    }

    private Function function(String name) {
        Function result = services.getNamespaces().functions().lookup(new Name(name));
        assertNotNull(result, "function must be available: " + name);
        return result;
    }

    private Sort sort(String name) {
        Sort result = services.getNamespaces().sorts().lookup(new Name(name));
        assertNotNull(result, "sort must be available: " + name);
        return result;
    }

    private Term fieldTerm(String name) {
        return services.getTermBuilder().func(registerFieldConstant(name));
    }

    private Term nil() {
        return services.getTermBuilder().func(function("nil"));
    }

    private Term cons(Term head, Term tail) {
        return services.getTermBuilder().func(function("cons"), head, tail);
    }

    private Term consr(Term list, Term last) {
        return services.getTermBuilder().func(function("consr"), list, last);
    }

    private Term at(Term index) {
        return services.getTermBuilder().func(function("at"), index);
    }

    private Term list(Term... segments) {
        Term result = nil();
        for (int i = segments.length - 1; i >= 0; i--) {
            result = cons(segments[i], result);
        }
        return result;
    }

    private KeYSolidityType uintType() {
        return new KeYSolidityType(PrimitiveType.UINT256,
            services.getTheoryInfo().getIntLDT().targetSort());
    }

    private FieldDeclaration field(String name) {
        return new FieldDeclaration(new Name(name), new TypeReference(new Name("uint")));
    }

    private FieldReference stateField(String name, KeYSolidityType type) {
        registerFieldConstant(name);
        StateVariableDeclaration decl = new StateVariableDeclaration(new Name(name), type,
            new Name(name), null, Visibility.Public);
        return new FieldReference(decl, decl.getType());
    }

    private Term bindPath(SolidityProgramElement path) {
        Sort listSort = sort("List");
        ProgramSV pathSV = SchemaVariableFactory.createProgramSV(new Name("path"),
            ProgramSVSort.EXPRESSION, false);
        TermSV pathTermSV = SchemaVariableFactory.createTermSV(new Name("pathTerm"), listSort);
        SameAsTermCondition pathCondition = new SameAsTermCondition(pathSV, pathTermSV);

        MatchConditions result = (MatchConditions) pathCondition.check(pathSV, path,
            MatchConditions.EMPTY_MATCHCONDITIONS, services);
        assertNotNull(result, "path should lower to a logic list");
        return result.getInstantiations().getInstantiation(pathTermSV);
    }

    private MatchConditions bind(MatchConditions mc, org.key_project.logic.op.sv.SchemaVariable sv,
            Term t) {
        SVInstantiations inst = mc.getInstantiations();
        return mc.setInstantiations(inst.add(sv, t, services));
    }

    @Test
    void bindsFreeTermVariableToConversionOfProgramVariable() {
        MatchConditions result = (MatchConditions) condition.check(progSV, fieldRef,
            MatchConditions.EMPTY_MATCHCONDITIONS, services);
        assertNotNull(result, "condition should hold and bind the term variable");
        Object sInst = result.getInstantiations().getInstantiation(termSV);
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
        // Now that termSV is List-sorted, the conflicting value should also be a List
        Term otherValue = list(services.getTermBuilder().func(other));
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
        Sort structSort = sort("Struct");

        registerFieldConstant("age");
        FieldDeclaration ageField = field("age");
        StructDeclaration structType =
            new StructDeclaration(new Name("Person"), List.of(ageField), -1);
        ProgramVariable st = new ProgramVariable(new Name("st"),
            new KeYSolidityType(structType, structSort), DataLocation.Default);
        MemberExp memberPath = new MemberExp(st, ageField, st.getType());

        assertEquals(list(fieldTerm("age")), bindPath(memberPath),
            "st.age should lower to cons(age, nil)");
    }

    @Test
    void convertsRootFieldReferencesToListPaths() {
        // Global roots are now represented as single-element List paths: cons(field, nil)
        // expectedValue is already cons(Bank$balance, nil) as set up in setUp()
        assertEquals(expectedValue, Services.convertToLogicElement(fieldRef, services),
            "root storage fields must now lower to single-element List paths");
    }

    @Test
    void bindsNestedMemberPathToLogicList() {
        Sort structSort = sort("Struct");
        KeYSolidityType personType = new KeYSolidityType(
            new StructDeclaration(new Name("Person"), List.of(field("account")), -1),
            structSort);
        FieldReference alice = stateField("alice", personType);
        FieldDeclaration account = field("account");
        FieldDeclaration balance = field("balance");
        registerFieldConstant("account");
        registerFieldConstant("balance");

        MemberExp accountPath = new MemberExp(alice, account, personType);
        MemberExp balancePath = new MemberExp(accountPath, balance, uintType());

        // With unified path representation, alice is now cons(alice, nil), and each subsequent
        // field is appended using consr. So alice.account.balance becomes:
        // consr(consr(cons(alice, nil), account), balance)
        Term alicePath = list(fieldTerm("alice"));
        Term aliceAccount = consr(alicePath, fieldTerm("account"));
        assertEquals(consr(aliceAccount, fieldTerm("balance")), bindPath(balancePath),
            "alice.account.balance should append fields in source order using consr");
    }

    @Test
    void bindsIndexedPathToLogicList() {
        KeYSolidityType uintType = uintType();
        ProgramVariable tokens = new ProgramVariable(new Name("tokens"),
            new KeYSolidityType(new ArrayType(PrimitiveType.UINT256, 3), uintType.getSort()),
            DataLocation.Storage);
        ProgramVariable i = new ProgramVariable(new Name("i"), uintType, DataLocation.Default);
        IndexExpression indexedPath = new IndexExpression(tokens, i);

        assertEquals(
            list(services.getTermBuilder().var(tokens), at(services.getTermBuilder().var(i))),
            bindPath(indexedPath),
            "tokens[i] should lower to a path list with an at(index) segment");
    }

    @Test
    void bindsMixedFieldAndIndexPathToLogicList() {
        Sort structSort = sort("Struct");
        KeYSolidityType mappingType = new KeYSolidityType(
            new MappingType(PrimitiveType.UINT256, PrimitiveType.UINT256), structSort);
        KeYSolidityType ledgerType = new KeYSolidityType(
            new StructDeclaration(new Name("Ledger"), List.of(field("balances")), -1),
            structSort);
        FieldReference ledger = stateField("ledger", ledgerType);
        FieldDeclaration balances = field("balances");
        registerFieldConstant("balances");
        ProgramVariable k = new ProgramVariable(new Name("k"), uintType(), DataLocation.Default);

        MemberExp balancesPath = new MemberExp(ledger, balances, mappingType);
        IndexExpression indexedPath = new IndexExpression(balancesPath, k);

        // With unified representation, ledger is cons(ledger, nil), then balances is appended
        // using consr, then at(k) is appended using consr
        Term ledgerPath = list(fieldTerm("ledger"));
        Term ledgerBalances = consr(ledgerPath, fieldTerm("balances"));
        assertEquals(consr(ledgerBalances, at(services.getTermBuilder().var(k))),
            bindPath(indexedPath),
            "ledger.balances[k] should preserve field and index order");
    }
}
