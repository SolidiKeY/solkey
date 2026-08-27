/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import java.math.BigInteger;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.BinaryExpression;
import org.key_project.solidity.program.ast.expressions.operators.Operator;
import org.key_project.solidity.program.ast.references.FieldReference;
import org.key_project.solidity.program.ast.references.TypeReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PathSVSortTest {

    private Services services;
    private KeYSolidityType uintType;
    private Sort uintSort;

    @BeforeEach
    void setUp() {
        services = new Services();
        uintSort = new SortImpl(new Name("uint256"), false);
        uintType = new KeYSolidityType(PrimitiveType.UINT256, uintSort);
    }

    private ProgramVariable variable(String name, DataLocation location) {
        return new ProgramVariable(new Name(name), uintType, location);
    }

    private ProgramVariable variable(String name, Type type, DataLocation location) {
        return new ProgramVariable(new Name(name), new KeYSolidityType(type, uintSort), location);
    }

    private FieldReference storageField(String name) {
        return storageField(name, uintType);
    }

    private FieldReference storageField(String name, Type type) {
        return storageField(name, new KeYSolidityType(type, uintSort));
    }

    private FieldReference storageField(String name, KeYSolidityType type) {
        StateVariableDeclaration decl = new StateVariableDeclaration(new Name(name),
            type, new Name("Store$" + name), null, Visibility.Public);
        return new FieldReference(decl, decl.getType());
    }

    private FieldDeclaration field(String name) {
        return new FieldDeclaration(new Name(name), new TypeReference(new Name("uint256")));
    }

    @Test
    void stateFieldIsSimpleStoragePath() {
        FieldReference field = storageField("balance");

        assertTrue(ProgramSVSort.STORAGE_PATH.canStandFor(field, services));
        assertTrue(ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(field, services));
        assertFalse(ProgramSVSort.COMPLEX_STORAGE_PATH.canStandFor(field, services));
        assertFalse(ProgramSVSort.MEMORY_PATH.canStandFor(field, services));
    }

    @Test
    void explicitStorageAndMemoryVariablesAreSimplePaths() {
        ProgramVariable storage = variable("sp", DataLocation.Storage);
        ProgramVariable memory = variable("mp", DataLocation.Memory);

        assertTrue(ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(storage, services));
        assertFalse(ProgramSVSort.MEMORY_PATH.canStandFor(storage, services));

        assertTrue(ProgramSVSort.SIMPLE_MEMORY_PATH.canStandFor(memory, services));
        assertFalse(ProgramSVSort.STORAGE_PATH.canStandFor(memory, services));
    }

    @Test
    void defaultLocationVariableIsNotAPath() {
        ProgramVariable local = variable("x", DataLocation.Default);

        assertFalse(ProgramSVSort.STORAGE_PATH.canStandFor(local, services));
        assertFalse(ProgramSVSort.MEMORY_PATH.canStandFor(local, services));
        assertTrue(ProgramSVSort.PATH.createInstance("local")
                .canStandFor(local, services));
    }

    @Test
    void memberPathKeepsBaseLocationAndIsComplex() {
        MemberExp storageMember = new MemberExp(storageField("alice"), field("age"),
            PrimitiveType.UINT256);
        MemberExp memoryMember = new MemberExp(variable("account", DataLocation.Memory),
            field("age"), PrimitiveType.UINT256);

        assertTrue(ProgramSVSort.COMPLEX_STORAGE_PATH.canStandFor(storageMember, services));
        assertFalse(ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(storageMember, services));
        assertFalse(ProgramSVSort.MEMORY_PATH.canStandFor(storageMember, services));

        assertTrue(ProgramSVSort.COMPLEX_MEMORY_PATH.canStandFor(memoryMember, services));
        assertFalse(ProgramSVSort.SIMPLE_MEMORY_PATH.canStandFor(memoryMember, services));
        assertFalse(ProgramSVSort.STORAGE_PATH.canStandFor(memoryMember, services));
    }

    @Test
    void indexPathIsComplexWithSimpleIndexAndNoPathWithNonSimpleIndex() {
        ProgramVariable simpleIndex = variable("i", DataLocation.Default);
        IndexExpression simplePath = new IndexExpression(storageField("balances"), simpleIndex);

        ProgramVariable j = variable("j", DataLocation.Default);
        BinaryExpression complexIndex =
            new BinaryExpression(Operator.ADD, j, new Uint256Literal(BigInteger.ONE));
        IndexExpression complexPath = new IndexExpression(storageField("balances"), complexIndex);

        assertTrue(ProgramSVSort.COMPLEX_STORAGE_PATH.canStandFor(simplePath, services));
        assertFalse(ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(simplePath, services));

        assertFalse(ProgramSVSort.STORAGE_PATH.canStandFor(complexPath, services));
        assertFalse(ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(complexPath, services));
        assertFalse(ProgramSVSort.COMPLEX_STORAGE_PATH.canStandFor(complexPath, services));
    }

    @Test
    void typeKindFlagsSeparatePrimitiveAndReferencePaths() {
        StructDeclaration accountStruct =
            new StructDeclaration(new Name("Account"), List.of(field("balance")), -1);
        FieldReference alice = storageField("alice", accountStruct);
        MemberExp age = new MemberExp(alice, field("age"), PrimitiveType.UINT256);
        MemberExp account = new MemberExp(alice, field("account"), accountStruct);

        ProgramSVSort complexReference =
            ProgramSVSort.PATH.createInstance("storage,complex,reference");
        ProgramSVSort complexPrimitive = ProgramSVSort.PATH.createInstance("complex,primitive");

        assertTrue(complexReference.canStandFor(account, services));
        assertFalse(complexReference.canStandFor(age, services));
        assertFalse(complexReference.canStandFor(alice, services));
        assertTrue(complexPrimitive.canStandFor(age, services));
        assertFalse(complexPrimitive.canStandFor(account, services));
    }

    @Test
    void nonSimpleExpressionValueAcceptsOperatorShapedValueExpressionsOnly() {
        ProgramSVSort nseValue = ProgramSVSort.NON_SIMPLE_EXPRESSION.createInstance("value");
        ProgramVariable a = variable("a", DataLocation.Default);
        BinaryExpression sum =
            new BinaryExpression(Operator.ADD, a, new Uint256Literal(BigInteger.ONE));
        StructDeclaration accountStruct =
            new StructDeclaration(new Name("Account"), List.of(field("balance")), -1);
        FieldReference alice = storageField("alice", accountStruct);
        MemberExp age = new MemberExp(alice, field("age"), PrimitiveType.UINT256);
        MemberExp account = new MemberExp(alice, field("account"), accountStruct);

        assertTrue(nseValue.canStandFor(sum, services));
        assertFalse(nseValue.canStandFor(a, services));
        assertFalse(nseValue.canStandFor(new Uint256Literal(BigInteger.ONE), services));
        assertFalse(nseValue.canStandFor(age, services));
        assertFalse(nseValue.canStandFor(account, services));
        assertFalse(nseValue.canStandFor(alice, services));
    }

    @Test
    void nonPathExpressionDoesNotMatchAnyPathSort() {
        BinaryExpression expression = new BinaryExpression(Operator.ADD,
            variable("x", DataLocation.Default), new Uint256Literal(BigInteger.ONE));

        assertFalse(ProgramSVSort.STORAGE_PATH.canStandFor(expression, services));
        assertFalse(ProgramSVSort.MEMORY_PATH.canStandFor(expression, services));
    }

    @Test
    void parameterizedPathSortRejectsUnknownAndConflictingFlags() {
        assertThrows(IllegalArgumentException.class,
            () -> ProgramSVSort.PATH.createInstance("storage,memory"));
        assertThrows(IllegalArgumentException.class,
            () -> ProgramSVSort.PATH.createInstance("memory,global"));
        assertThrows(IllegalArgumentException.class,
            () -> ProgramSVSort.PATH.createInstance("storage,outer"));
        assertThrows(IllegalArgumentException.class,
            () -> ProgramSVSort.PATH.createInstance("array,mapping"));
    }

    @Test
    void arrayFilterMatchesDynamicArrayFieldReferenceOnly() {
        FieldReference values = storageField("values",
            new DynamicArrayType(PrimitiveType.UINT));
        FieldReference balances = storageField("balances",
            new MappingType(PrimitiveType.UINT, PrimitiveType.UINT));

        assertTrue(ProgramSVSort.PATH.createInstance("storage,array")
                .canStandFor(values, services));
        assertFalse(ProgramSVSort.PATH.createInstance("storage,array")
                .canStandFor(balances, services));
    }

    @Test
    void mappingFilterMatchesMappingFieldReferenceOnly() {
        FieldReference values = storageField("values",
            new DynamicArrayType(PrimitiveType.UINT));
        FieldReference balances = storageField("balances",
            new MappingType(PrimitiveType.UINT, PrimitiveType.UINT));

        assertFalse(ProgramSVSort.PATH.createInstance("storage,mapping")
                .canStandFor(values, services));
        assertTrue(ProgramSVSort.PATH.createInstance("storage,mapping")
                .canStandFor(balances, services));
    }

    @Test
    void arrayFilterMatchesStorageLocalArrays() {
        ProgramVariable localArray = variable("localArray",
            new DynamicArrayType(PrimitiveType.UINT), DataLocation.Storage);

        assertTrue(ProgramSVSort.PATH.createInstance("storage,array")
                .canStandFor(localArray, services));
        assertFalse(ProgramSVSort.PATH.createInstance("storage,mapping")
                .canStandFor(localArray, services));
    }

    @Test
    void nestedArrayIndexKeepsArrayKindForNextIndex() {
        FieldReference matrix = storageField("matrix",
            new DynamicArrayType(new DynamicArrayType(PrimitiveType.UINT)));
        IndexExpression matrixRow = new IndexExpression(matrix, new Uint256Literal(BigInteger.TWO));

        assertTrue(ProgramSVSort.PATH.createInstance("storage,array")
                .canStandFor(matrixRow, services));
        assertFalse(ProgramSVSort.PATH.createInstance("storage,mapping")
                .canStandFor(matrixRow, services));
    }
}
