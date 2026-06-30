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
}
