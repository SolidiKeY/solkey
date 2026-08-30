/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.program.ast.references.TypeReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StorageReferenceTypesTest {

    private static final MappingType UINT_TO_UINT =
        new MappingType(PrimitiveType.UINT, PrimitiveType.UINT);

    private static FieldDeclaration field(String name, Type type) {
        return new FieldDeclaration(new Name(name), new TypeReference(type));
    }

    private static StructDeclaration struct(String name, FieldDeclaration... fields) {
        return new StructDeclaration(new Name(name), List.of(fields), -1);
    }

    @Test
    void mappingItselfContainsMapping() {
        assertTrue(StorageReferenceTypes.containsMapping(UINT_TO_UINT));
    }

    @Test
    void structWithDirectMappingMember() {
        StructDeclaration ledger = struct("Ledger",
            field("nonce", PrimitiveType.UINT), field("balances", UINT_TO_UINT));
        assertTrue(StorageReferenceTypes.containsMapping(ledger));
    }

    @Test
    void mapFreeStructDoesNotContainMapping() {
        StructDeclaration token = struct("Token", field("value", PrimitiveType.UINT));
        StructDeclaration account = struct("Account",
            field("balance", PrimitiveType.UINT), field("token", token));
        assertFalse(StorageReferenceTypes.containsMapping(account));
    }

    @Test
    void nestedStructWithMappingMember() {
        StructDeclaration ledger = struct("Ledger", field("balances", UINT_TO_UINT));
        StructDeclaration use = struct("LedgerUse", field("ledger", ledger));
        assertTrue(StorageReferenceTypes.containsMapping(use));
    }

    @Test
    void arraysOfMappingCarryingStructs() {
        StructDeclaration ledger = struct("Ledger", field("balances", UINT_TO_UINT));
        assertTrue(StorageReferenceTypes.containsMapping(new DynamicArrayType(ledger)));
        assertTrue(StorageReferenceTypes.containsMapping(new ArrayType(ledger, 3)));
        assertFalse(StorageReferenceTypes.containsMapping(
            new DynamicArrayType(PrimitiveType.UINT)));
    }

    @Test
    void unwrapsKeYSolidityType() {
        StructDeclaration ledger = struct("Ledger", field("balances", UINT_TO_UINT));
        KeYSolidityType wrapped =
            new KeYSolidityType(ledger, new SortImpl(new Name("Struct")));
        assertTrue(StorageReferenceTypes.containsMapping(wrapped));
    }

    @Test
    void nullTypeDoesNotContainMapping() {
        assertFalse(StorageReferenceTypes.containsMapping(null));
    }
}
