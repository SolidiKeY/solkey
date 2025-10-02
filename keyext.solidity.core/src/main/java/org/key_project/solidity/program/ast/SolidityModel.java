/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast;

import java.util.HashMap;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.Type;

public class SolidityModel {

    private final Map<Name, Type> typeMap = new HashMap<>();

    public Type getType(Name typeName) {
        return typeMap.get(typeName);
    }

}
