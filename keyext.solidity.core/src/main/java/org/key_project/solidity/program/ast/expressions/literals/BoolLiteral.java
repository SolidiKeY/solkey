/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.literals;

import java.util.Objects;

import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class BoolLiteral extends Literal {

    public static final BoolLiteral TRUE = new BoolLiteral(true);
    public static final BoolLiteral FALSE = new BoolLiteral(false);

    private final boolean value;

    private BoolLiteral(boolean value) {
        super(PrimitiveType.BOOL);
        this.value = value;
    }

    public BoolLiteral(ExtList children) {
        super(PrimitiveType.BOOL);
        this.value = Objects.requireNonNull(children.removeFirstOccurrence(boolean.class));
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value ? "true" : "false";
    }

    public void visit(Visitor v) {
        v.performActionOnBoolLiteral(this);
    }
}
