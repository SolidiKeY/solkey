/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.ExtList;

import java.util.Objects;

public abstract class Expression implements SolidityProgramElement, SoliditiyExpression {
    protected final Type type;

    public Expression(Type type) {
        this.type = type;
    }

    public Expression(ExtList children) {
        this.type = Objects.requireNonNull(children.removeFirstOccurrence(Type.class));
    }

    @Override
    public Type getType() {
        return type;
    }
}
