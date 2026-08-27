/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.program.ast.references.TypeReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldSVSortTest {

    private Services services;
    private FieldDeclaration primitiveField;
    private FieldDeclaration structField;
    private FieldDeclaration mappingField;
    private FieldDeclaration arrayField;

    @BeforeEach
    void setUp() {
        services = new Services();
        primitiveField = new FieldDeclaration(new Name("age"),
            new TypeReference(new Name("uint")));
        structField = new FieldDeclaration(new Name("payload"),
            new TypeReference(new StructDeclaration(new Name("Payload"), List.of(), -1)));
        mappingField = new FieldDeclaration(new Name("balances"),
            new TypeReference(new MappingType(PrimitiveType.UINT, PrimitiveType.UINT)));
        arrayField = new FieldDeclaration(new Name("values"),
            new TypeReference(new DynamicArrayType(PrimitiveType.UINT)));
    }

    @Test
    void plainFieldMatchesEveryFieldKind() {
        assertTrue(ProgramSVSort.FIELD.canStandFor(primitiveField, services));
        assertTrue(ProgramSVSort.FIELD.canStandFor(structField, services));
        assertTrue(ProgramSVSort.FIELD.canStandFor(mappingField, services));
    }

    @Test
    void primitiveFlagMatchesPrimitiveFieldsOnly() {
        ProgramSVSort primitive = ProgramSVSort.FIELD.createInstance("primitive");

        assertTrue(primitive.canStandFor(primitiveField, services));
        assertFalse(primitive.canStandFor(structField, services));
        assertFalse(primitive.canStandFor(mappingField, services));
        assertFalse(primitive.canStandFor(arrayField, services));
    }

    @Test
    void referenceFlagMatchesStructArrayAndMappingFields() {
        ProgramSVSort reference = ProgramSVSort.FIELD.createInstance("reference");

        assertFalse(reference.canStandFor(primitiveField, services));
        assertTrue(reference.canStandFor(structField, services));
        assertTrue(reference.canStandFor(mappingField, services));
        assertTrue(reference.canStandFor(arrayField, services));
    }

    @Test
    void unknownFlagIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> ProgramSVSort.FIELD.createInstance("value"));
    }
}
