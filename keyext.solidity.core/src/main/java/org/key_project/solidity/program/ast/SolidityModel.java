/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast;

import java.util.HashMap;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.Type;

/**
 * This class is responsible to answer queries about the solidity program model
 * Such queries are for instance: all declared and known contracts, all functions for
 * a contract, finding a contract by name or a function declaration by its signature,
 * providing the type and KeYSolidityType by name etc.
 */
public class SolidityModel {
    private final Map<Name, Type> typeMap = new HashMap<>();

    public Type getType(Name typeName) {
        return typeMap.get(typeName);
    }

    public KeYSolidityType getKeYSolidityType(String type) {
        throw new RuntimeException("Not implemented yet");
    }
}
