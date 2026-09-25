/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.Type;

public class TypedField extends SFunction {
    private final Type type;

    public TypedField(Name name, Sort sort, Type type) {
        super(name, sort, true, true);
        this.type = unwrap(type);
    }

    public Type type() {
        return type;
    }

    public static Type unwrap(Type type) {
        return type instanceof KeYSolidityType kst && kst.getSolidityType() != null
                ? kst.getSolidityType()
                : type;
    }
}
