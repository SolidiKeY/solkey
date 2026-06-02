/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.literals;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.SourceData;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;

import org.jspecify.annotations.NonNull;
import org.key_project.solidity.rule.matching.inst.MatchConditions;

public abstract class Literal extends SolidityExpression {
    protected Literal(Type type) {
        super(type);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public MatchConditions match(SourceData source, MatchConditions matchCond) {
        final SolidityProgramElement src = source.getSource();
        if (this.equals(src)) {
            source.next();
            return matchCond;
        }
        return null;
    }

}
