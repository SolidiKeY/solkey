/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;

public final class MemoryReferenceTypes {
    private MemoryReferenceTypes() {}

    public static boolean isReferenceType(Type type) {
        Type unwrapped = type instanceof KeYSolidityType kst ? kst.getSolidityType() : type;
        return unwrapped instanceof StructDeclaration
                || unwrapped instanceof ArrayType
                || unwrapped instanceof DynamicArrayType;
    }

    public static KeYSolidityType asMemoryReferenceType(KeYSolidityType original,
            DataLocation dataLocation, Services services) {
        if (dataLocation != DataLocation.Memory || original == null
                || !isReferenceType(original)) {
            return original;
        }
        Sort identitySort = services.getTheoryInfo().getMemoryLDT().getIdentitySort();
        if (identitySort == null) {
            return original;
        }
        Type solidityType = original.getSolidityType();
        if (solidityType == null) {
            return original;
        }
        return new KeYSolidityType(solidityType, identitySort);
    }

    /// Retypes a `storage` local to the `List` sort, so it denotes the *path* it is bound to
    /// rather than the value at that path. Applies to every reference-location type held in
    /// storage — structs as well as dynamic/fixed arrays and mappings (e.g.
    /// `Token[] storage bt = ...`). Anything else is returned unchanged.
    public static KeYSolidityType asStorageAliasType(KeYSolidityType original,
            DataLocation dataLocation, Services services) {
        if (dataLocation != DataLocation.Storage || original == null) {
            return original;
        }
        Sort sort = original.getSort();
        if (sort == null || !isStoragePathType(original, sort)) {
            return original;
        }
        Sort listSort =
            services.getNamespaces().sorts().lookup(new org.key_project.logic.Name("List"));
        if (listSort == null) {
            return original;
        }
        return new KeYSolidityType(original.getSolidityType(), listSort);
    }

    public static boolean isStoragePathType(KeYSolidityType original, Sort sort) {
        if ("Struct".equals(sort.name().toString())) {
            return true;
        }
        Type solType = original.getSolidityType();
        return solType instanceof DynamicArrayType || solType instanceof ArrayType
                || solType instanceof MappingType;
    }

    /// The type a local variable declaration gets: a `storage` local denotes a path, a `memory`
    /// local denotes an identity, everything else keeps its declared type.
    public static KeYSolidityType asLocalVariableType(KeYSolidityType original,
            DataLocation dataLocation, Services services) {
        return asMemoryReferenceType(asStorageAliasType(original, dataLocation, services),
            dataLocation, services);
    }
}
