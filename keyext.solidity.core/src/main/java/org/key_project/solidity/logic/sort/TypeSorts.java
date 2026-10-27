/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.sort;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.TypedField;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.theory.StructLDT;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.Nullable;

public final class TypeSorts {
    public static final Name FIXED_ARRAY = new Name("FixedArray");
    public static final Name DYNAMIC_ARRAY = new Name("DynamicArray");
    public static final Name MAPPING = new Name("Mapping");
    public static final Name STRUCT_TYPE = new Name("StructType");

    private TypeSorts() {}

    public static Sort of(@Nullable Type type, Services services) {
        Type unwrapped = StaticTypes.unwrap(type);
        return switch (unwrapped) {
            case ArrayType array ->
                fixedArray(componentOf(array.getElementType(), services), array.length(), services);
            case DynamicArrayType array ->
                dynamicArray(componentOf(array.getElementType(), services), services);
            case MappingType mapping -> mapping(componentOf(mapping.keyType(), services),
                componentOf(mapping.valueType(), services), services);
            case StructDeclaration struct -> struct(struct,
                struct.getContract() == null ? null : struct.getContract().name().toString(),
                services);
            case null, default -> structSort(services);
        };
    }

    public static Sort fixedArray(Sort element, int length, Services services) {
        return getOrAdd(services,
            new ArraySort(element, length, marker(services, FIXED_ARRAY)));
    }

    public static Sort dynamicArray(Sort element, Services services) {
        return getOrAdd(services, new DynamicArraySort(element, marker(services, DYNAMIC_ARRAY)));
    }

    public static Sort mapping(Sort key, Sort value, Services services) {
        return getOrAdd(services, new MappingSort(key, value, marker(services, MAPPING)));
    }

    public static Sort struct(StructDeclaration struct, @Nullable String contract,
            Services services) {
        Name name = new Name(contract == null ? StructLDT.fieldConstantName("struct", struct.name())
                : StructLDT.fieldConstantName(contract, struct.name()));
        return getOrAdd(services, new StructSort(name, struct, marker(services, STRUCT_TYPE)));
    }

    public static Sort structSort(Services services) {
        return services.getTheoryInfo().getStructLDT().targetSort();
    }

    public static @Nullable Sort elementOf(Sort sort, Services services) {
        Sort element = switch (sort) {
            case ArraySort array -> array.elementSort();
            case DynamicArraySort array -> array.elementSort();
            case MappingSort mapping -> mapping.valueSort();
            default -> null;
        };
        return element == null ? null : node(element, services);
    }

    public static int lengthOf(Sort sort) {
        if (sort instanceof ArraySort array) {
            return array.length();
        }
        throw new IllegalArgumentException("Not a fixed-size array sort: " + sort);
    }

    public static List<TypedField> membersOf(Sort sort, Services services) {
        List<TypedField> members = new ArrayList<>();
        if (sort instanceof StructSort struct) {
            for (FieldDeclaration field : struct.declaration().getFields()) {
                if (services.getNamespaces().functions()
                        .lookup(memberName(struct.declaration(), field)) instanceof TypedField t) {
                    members.add(t);
                }
            }
        }
        return members;
    }

    public static Sort memberOf(TypedField member, Services services) {
        return node(of(member.type(), services), services);
    }

    public static boolean isTrivial(Sort sort, Services services) {
        return isTrivial(sort, services, new HashSet<>());
    }

    private static boolean isTrivial(Sort sort, Services services, Set<Sort> visiting) {
        return switch (sort) {
            case ArraySort ignored -> false;
            case DynamicArraySort ignored -> false;
            case MappingSort mapping -> isTrivial(node(mapping.valueSort(), services), services,
                visiting);
            case StructSort struct -> !visiting.add(struct)
                    || membersOf(struct, services).stream()
                            .allMatch(m -> isTrivial(memberOf(m, services), services, visiting));
            default -> true;
        };
    }

    private static Sort node(Sort sort, Services services) {
        Sort struct = structSort(services);
        return sort.extendsTrans(struct) ? sort : struct;
    }

    private static Sort componentOf(Type type, Services services) {
        Type unwrapped = StaticTypes.unwrap(type);
        if (unwrapped instanceof ArrayType || unwrapped instanceof DynamicArrayType
                || unwrapped instanceof MappingType || unwrapped instanceof StructDeclaration) {
            return of(unwrapped, services);
        }
        KeYSolidityType kst =
            unwrapped == null ? null : services.getSolidityInfo().getKeYSolidityType(unwrapped);
        Sort sort = kst == null ? null : kst.getSort();
        return sort == null ? structSort(services) : sort;
    }

    private static Name memberName(StructDeclaration struct, FieldDeclaration field) {
        return struct.getContract() == null
                ? new Name(StructLDT.fieldConstantName(struct.name(), field.name()))
                : new Name(StructLDT.fieldConstantName(struct.getContract().name(),
                    struct.name(), field.name()));
    }

    private static ImmutableSet<Sort> marker(Services services, Name name) {
        Sort sort = services.getNamespaces().sorts().lookup(name);
        return sort == null ? DefaultImmutableSet.<Sort>nil().add(structSort(services))
                : DefaultImmutableSet.<Sort>nil().add(sort);
    }

    private static Sort getOrAdd(Services services, Sort sort) {
        Sort existing = services.getNamespaces().sorts().lookup(sort.name());
        if (existing != null) {
            return existing;
        }
        services.getNamespaces().sorts().addSafely(sort);
        return sort;
    }
}
