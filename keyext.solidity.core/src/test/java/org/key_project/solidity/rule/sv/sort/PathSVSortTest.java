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
import org.key_project.solidity.program.ast.abstractions.ArrayType;
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

    @BeforeEach
    void setUp() {
        services = new Services();
        Sort uintSort = new SortImpl(new Name("uint256"), false);
        uintType = new KeYSolidityType(PrimitiveType.UINT256, uintSort);
    }

    private ProgramVariable variable(String name, DataLocation location) {
        return new ProgramVariable(new Name(name), uintType, location);
    }

    private ProgramVariable variable(String name, KeYSolidityType type, DataLocation location) {
        return new ProgramVariable(new Name(name), type, location);
    }

    private KeYSolidityType type(String sortName, Type type) {
        return new KeYSolidityType(type, new SortImpl(new Name(sortName), false));
    }

    private FieldReference storageField(String name) {
        StateVariableDeclaration decl = new StateVariableDeclaration(new Name(name),
            uintType, new Name("Store$" + name), null, Visibility.Public);
        return new FieldReference(decl, decl.getType());
    }

    private FieldReference storageField(String name, Type type) {
        KeYSolidityType keyType = type("Store$" + name + "Sort", type);
        StateVariableDeclaration decl = new StateVariableDeclaration(new Name(name),
            keyType, new Name("Store$" + name), null, Visibility.Public);
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
        assertTrue(ProgramSVSort.PATH.createInstance("root.local.primitive")
                .canStandFor(local, services));
    }

    @Test
    void memberPathKeepsBaseLocationAndSimplicityDependsOnBaseShape() {
        // Depth-2 path: alice.age (base is ROOT) → simple
        MemberExp shallowStorageMember = new MemberExp(storageField("alice"), field("age"),
            PrimitiveType.UINT256);
        MemberExp shallowMemoryMember = new MemberExp(variable("account", DataLocation.Memory),
            field("age"), PrimitiveType.UINT256);

        // Depth-2 paths should be simple (base is ROOT)
        assertTrue(ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(shallowStorageMember, services));
        assertFalse(ProgramSVSort.COMPLEX_STORAGE_PATH.canStandFor(shallowStorageMember, services));
        assertFalse(ProgramSVSort.MEMORY_PATH.canStandFor(shallowStorageMember, services));

        assertTrue(ProgramSVSort.SIMPLE_MEMORY_PATH.canStandFor(shallowMemoryMember, services));
        assertFalse(ProgramSVSort.COMPLEX_MEMORY_PATH.canStandFor(shallowMemoryMember, services));
        assertFalse(ProgramSVSort.STORAGE_PATH.canStandFor(shallowMemoryMember, services));

        // Depth-3 path: alice.account.balance (base is FIELD) → complex
        MemberExp aliceAccount = new MemberExp(storageField("alice"), field("account"),
            PrimitiveType.UINT256);
        MemberExp deepStorageMember = new MemberExp(aliceAccount, field("balance"),
            PrimitiveType.UINT256);

        assertTrue(ProgramSVSort.COMPLEX_STORAGE_PATH.canStandFor(deepStorageMember, services));
        assertFalse(ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(deepStorageMember, services));
    }

    @Test
    void indexPathIsComplexEvenWithSimpleIndexExpression() {
        ProgramVariable simpleIndex = variable("i", DataLocation.Default);
        IndexExpression simplePath = new IndexExpression(storageField("balances"), simpleIndex);

        ProgramVariable j = variable("j", DataLocation.Default);
        BinaryExpression complexIndex =
            new BinaryExpression(Operator.ADD, j, new Uint256Literal(BigInteger.ONE));
        IndexExpression complexPath = new IndexExpression(storageField("balances"), complexIndex);

        assertTrue(ProgramSVSort.COMPLEX_STORAGE_PATH.canStandFor(simplePath, services));
        assertFalse(ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(simplePath, services));

        assertTrue(ProgramSVSort.STORAGE_PATH.canStandFor(complexPath, services));
        assertFalse(ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(complexPath, services));
        assertTrue(ProgramSVSort.COMPLEX_STORAGE_PATH.canStandFor(complexPath, services));
    }

    @Test
    void nonPathExpressionDoesNotMatchAnyPathSort() {
        BinaryExpression expression = new BinaryExpression(Operator.ADD,
            variable("x", DataLocation.Default), new Uint256Literal(BigInteger.ONE));

        assertFalse(ProgramSVSort.STORAGE_PATH.canStandFor(expression, services));
        assertFalse(ProgramSVSort.MEMORY_PATH.canStandFor(expression, services));
    }

    @Test
    void parameterizedPathSortFiltersRootOriginAndValueKind() {
        ProgramSVSort storageGlobalPrimitive =
            ProgramSVSort.PATH.createInstance("storage.root.global.primitive");
        ProgramSVSort storageLocalPrimitive =
            ProgramSVSort.PATH.createInstance("storage.root.local.primitive");
        ProgramSVSort memoryLocalReference =
            ProgramSVSort.PATH.createInstance("memory.root.local.reference");

        FieldReference globalField = storageField("balance");
        ProgramVariable storageLocal = variable("slot", DataLocation.Storage);
        ProgramVariable memoryStruct = variable("person",
            type("Person", new StructDeclaration(new Name("Person"), List.of(field("age")), -1)),
            DataLocation.Memory);

        assertTrue(storageGlobalPrimitive.canStandFor(globalField, services));
        assertFalse(storageGlobalPrimitive.canStandFor(storageLocal, services));
        assertTrue(storageLocalPrimitive.canStandFor(storageLocal, services));
        assertFalse(storageLocalPrimitive.canStandFor(globalField, services));
        assertTrue(memoryLocalReference.canStandFor(memoryStruct, services));
        assertFalse(memoryLocalReference.canStandFor(storageLocal, services));
    }

    @Test
    void parameterizedPathSortFiltersFieldTerminal() {
        ProgramSVSort storageFieldPath =
            ProgramSVSort.PATH.createInstance("storage.field.global.primitive");
        ProgramSVSort storageRoot =
            ProgramSVSort.PATH.createInstance("storage.root.global.primitive");

        MemberExp member = new MemberExp(storageField("alice",
            new StructDeclaration(new Name("Person"), List.of(field("age")), -1)), field("age"),
            PrimitiveType.UINT256);

        assertTrue(storageFieldPath.canStandFor(member, services));
        assertFalse(storageRoot.canStandFor(member, services));
    }

    @Test
    void parameterizedPathSortFiltersArrayAndMappingIndexes() {
        ProgramSVSort arrayIndex =
            ProgramSVSort.PATH.createInstance("storage.index.array.primitive");
        ProgramSVSort mappingIndex =
            ProgramSVSort.PATH.createInstance("storage.index.mapping.primitive");
        ProgramSVSort referenceMappingIndex =
            ProgramSVSort.PATH.createInstance("storage.index.mapping.reference");

        ProgramVariable i = variable("i", DataLocation.Default);
        ProgramVariable array = variable("arr",
            type("UintArray", new ArrayType(PrimitiveType.UINT256, 3)), DataLocation.Storage);
        ProgramVariable mapping = variable("ledger",
            type("UintMapping", new MappingType(PrimitiveType.UINT256, PrimitiveType.UINT256)),
            DataLocation.Storage);
        ProgramVariable referenceMapping = variable("people",
            type("PersonMapping", new MappingType(PrimitiveType.UINT256,
                new StructDeclaration(new Name("Person"), List.of(field("age")), -1))),
            DataLocation.Storage);

        IndexExpression arrayPath = new IndexExpression(array, i);
        IndexExpression mappingPath = new IndexExpression(mapping, i);
        IndexExpression referenceMappingPath = new IndexExpression(referenceMapping, i);

        assertTrue(arrayIndex.canStandFor(arrayPath, services));
        assertFalse(mappingIndex.canStandFor(arrayPath, services));

        assertTrue(mappingIndex.canStandFor(mappingPath, services));
        assertFalse(arrayIndex.canStandFor(mappingPath, services));

        assertTrue(referenceMappingIndex.canStandFor(referenceMappingPath, services));
        assertFalse(mappingIndex.canStandFor(referenceMappingPath, services));
    }

    @Test
    void parameterizedPathSortRejectsUnknownAndConflictingFlags() {
        assertThrows(IllegalArgumentException.class,
            () -> ProgramSVSort.PATH.createInstance("storage.memory"));
        assertThrows(IllegalArgumentException.class,
            () -> ProgramSVSort.PATH.createInstance("memory.global"));
        assertThrows(IllegalArgumentException.class,
            () -> ProgramSVSort.PATH.createInstance("storage.outer"));
    }
}
