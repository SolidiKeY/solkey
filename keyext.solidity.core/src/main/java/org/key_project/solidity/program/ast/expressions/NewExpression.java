/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.Objects;

import org.key_project.solidity.program.ast.LeafProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;

import org.jspecify.annotations.Nullable;

public class NewExpression extends SolidityExpression implements LeafProgramElement {

    public NewExpression(Type type) {
        super(type);
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnNewExpression(this);
    }

    @Override
    public String toString() {
        return "new " + type.name();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NewExpression that))
            return false;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
