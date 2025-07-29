/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast;

import org.key_project.logic.Name;
import org.key_project.solidity.logic.ast.abstractions.Type;

/** This class resolves the type of a reference or expression */
public class TypeResolver {

    private final SolidityModel solidityModel;

    public TypeResolver(SolidityModel solidityModel) {
        this.solidityModel = solidityModel;
    }

    public Type resolveTypeByName(Name typeName) {
        return solidityModel.getType(typeName);
    }
}
