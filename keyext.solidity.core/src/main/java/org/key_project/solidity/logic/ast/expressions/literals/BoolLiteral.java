/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.expressions.literals;

import org.key_project.solidity.logic.ast.abstractions.PrimitiveType;

public class BoolLiteral extends Literal {

    public static final BoolLiteral TRUE = new BoolLiteral(true);
    public static final BoolLiteral FALSE = new BoolLiteral(false);

    private final boolean value;

    private BoolLiteral(boolean value) {
        super(PrimitiveType.BOOL);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value ? "true" : "false";
    }
}
