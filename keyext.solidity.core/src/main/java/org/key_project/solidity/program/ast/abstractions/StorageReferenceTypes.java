/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;

import org.jspecify.annotations.Nullable;

public final class StorageReferenceTypes {
    private StorageReferenceTypes() {}

    /// In storage, mappings are reference-typed locations as well, unlike in memory.
    public static boolean isReferenceType(Type type) {
        Type unwrapped = type instanceof KeYSolidityType kst ? kst.getSolidityType() : type;
        if (unwrapped == null) {
            return false;
        }
        return MemoryReferenceTypes.isReferenceType(unwrapped)
                || unwrapped instanceof MappingType;
    }

    /// Whether the type is a mapping or transitively holds one in a struct field or an
    /// array element.
    public static boolean containsMapping(@Nullable Type type) {
        return containsMapping(type, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static boolean containsMapping(@Nullable Type type, Set<Type> visited) {
        if (type == null) {
            return false;
        }
        Type unwrapped = type instanceof KeYSolidityType kst ? kst.getSolidityType() : type;
        if (unwrapped == null || !visited.add(unwrapped)) {
            return false;
        }
        if (unwrapped instanceof MappingType) {
            return true;
        }
        if (unwrapped instanceof StructDeclaration struct) {
            for (FieldDeclaration field : struct.getFields()) {
                Type fieldType = field.getTypeReference().resolvedType();
                if (containsMapping(fieldType, visited)) {
                    return true;
                }
            }
            return false;
        }
        if (unwrapped instanceof ArrayType array) {
            return containsMapping(array.getElementType(), visited);
        }
        if (unwrapped instanceof DynamicArrayType dynArray) {
            return containsMapping(dynArray.getElementType(), visited);
        }
        return false;
    }
}
