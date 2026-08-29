/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

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
}
