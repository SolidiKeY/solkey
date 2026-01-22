/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.Objects;

import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.util.ExtList;

public abstract class SolidityExpression implements SolidityProgramElement, Expression {
    protected final Type type;

    public SolidityExpression(Type type) {
        this.type = type;
    }

    public SolidityExpression(ExtList children) {
        this.type = Objects.requireNonNull(children.removeFirstOccurrence(Type.class));
    }

    @Override
    public Type getType() {
        return type;
    }
}
