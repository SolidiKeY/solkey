/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgramVariableSVSortTest {

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

    @Test
    void memoryFilterMatchesOnlyMemoryVariables() {
        ProgramSVSort sort = ProgramSVSort.VARIABLE.createInstance("memory");

        assertTrue(sort.canStandFor(variable("mp", DataLocation.Memory), services));
        assertFalse(sort.canStandFor(variable("sp", DataLocation.Storage), services));
        assertFalse(sort.canStandFor(variable("x", DataLocation.Default), services));
    }

    @Test
    void memoryLocalAliasUsesMemoryFilter() {
        ProgramVariableSVSort sort =
            (ProgramVariableSVSort) ProgramSVSort.VARIABLE.createInstance("memory,local");

        assertEquals(ProgramVariableSVSort.Filter.MEMORY_LOCAL, sort.getFilter());
    }

    @Test
    void variableFilterRejectsUnknownFlagsWithMemoryInHint() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> ProgramSVSort.VARIABLE.createInstance("calldata"));

        assertTrue(exception.getMessage().contains("memory"));
    }

    @Test
    void variableFilterRejectsRemovedValueFlag() {
        assertThrows(IllegalArgumentException.class,
            () -> ProgramSVSort.VARIABLE.createInstance("value"));
        assertThrows(IllegalArgumentException.class,
            () -> ProgramSVSort.VARIABLE.createInstance("non-storage"));
    }
}
