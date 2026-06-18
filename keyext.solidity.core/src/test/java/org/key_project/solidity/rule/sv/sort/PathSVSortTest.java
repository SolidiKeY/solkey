/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import java.math.BigInteger;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.declarations.StateVariableDeclaration;
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

    private FieldReference storageField(String name) {
        StateVariableDeclaration decl = new StateVariableDeclaration(new Name(name),
            uintType, new Name("Store$" + name), null, Visibility.Public);
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
    }

    @Test
    void memberPathKeepsBaseLocationAndSimplicity() {
        MemberExp storageMember = new MemberExp(storageField("alice"), field("age"),
            PrimitiveType.UINT256);
        MemberExp memoryMember = new MemberExp(variable("account", DataLocation.Memory),
            field("age"), PrimitiveType.UINT256);

        assertTrue(ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(storageMember, services));
        assertFalse(ProgramSVSort.MEMORY_PATH.canStandFor(storageMember, services));

        assertTrue(ProgramSVSort.SIMPLE_MEMORY_PATH.canStandFor(memoryMember, services));
        assertFalse(ProgramSVSort.STORAGE_PATH.canStandFor(memoryMember, services));
    }

    @Test
    void indexPathIsSimpleOnlyWhenIndexExpressionIsSimple() {
        ProgramVariable simpleIndex = variable("i", DataLocation.Default);
        IndexExpression simplePath = new IndexExpression(storageField("balances"), simpleIndex);

        ProgramVariable j = variable("j", DataLocation.Default);
        BinaryExpression complexIndex =
            new BinaryExpression(Operator.ADD, j, new Uint256Literal(BigInteger.ONE));
        IndexExpression complexPath = new IndexExpression(storageField("balances"), complexIndex);

        assertTrue(ProgramSVSort.SIMPLE_STORAGE_PATH.canStandFor(simplePath, services));
        assertFalse(ProgramSVSort.COMPLEX_STORAGE_PATH.canStandFor(simplePath, services));

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
}
