/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast;

import java.util.HashSet;
import java.util.Set;

import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;

import org.jspecify.annotations.Nullable;

public final class StorageDeleteTypes {
    private StorageDeleteTypes() {
    }

    public static boolean hasFixedArrayElement(@Nullable Type type) {
        return hasFixedArrayElement(type, new HashSet<>());
    }

    public static boolean pathResetsFixedArrayElement(@Nullable Type pathType) {
        Type type = StaticTypes.unwrap(pathType);
        if (type instanceof MappingType mapping) {
            Type value = StaticTypes.unwrap(mapping.valueType());
            return value instanceof ArrayType || hasFixedArrayElement(value);
        }
        return hasFixedArrayElement(type);
    }

    public static boolean hasMappingElement(@Nullable Type pathType) {
        Type type = StaticTypes.unwrap(pathType);
        Type element = switch (type) {
            case MappingType mapping -> mapping.valueType();
            case DynamicArrayType array -> array.getElementType();
            case ArrayType array -> array.getElementType();
            case null, default -> null;
        };
        return StaticTypes.unwrap(element) instanceof MappingType;
    }

    private static boolean hasFixedArrayElement(@Nullable Type type, Set<Type> visited) {
        Type t = StaticTypes.unwrap(type);
        if (t == null || !visited.add(t)) {
            return false;
        }
        Type element = switch (t) {
            case DynamicArrayType array -> StaticTypes.unwrap(array.getElementType());
            case ArrayType array -> StaticTypes.unwrap(array.getElementType());
            default -> null;
        };
        if (element instanceof ArrayType) {
            return true;
        }
        if (element != null) {
            return hasFixedArrayElement(element, visited);
        }
        if (t instanceof StructDeclaration struct) {
            for (FieldDeclaration field : struct.getFields()) {
                Type member = StaticTypes.unwrap(field.getTypeReference().resolvedType());
                if (!(member instanceof MappingType) && hasFixedArrayElement(member, visited)) {
                    return true;
                }
            }
        }
        return false;
    }
}
