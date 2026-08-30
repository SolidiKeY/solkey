/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions.operators;

import org.key_project.solidity.program.ast.SourceData;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.rule.matching.inst.MatchConditions;

import org.jspecify.annotations.Nullable;

public interface OperatorExpression extends Expression {
    Operator getOperator();

    @Override
    default @Nullable MatchConditions match(SourceData sourceData, @Nullable MatchConditions mc) {
        final var src = sourceData.getSource();

        if (src == null)
            return null;

        if (src.getClass() != this.getClass()) {
            return null;
        }

        if (!getOperator().equals(((OperatorExpression) src).getOperator())) {
            return null;
        }

        final SourceData newSource = new SourceData(src, 0, sourceData.getServices());
        mc = matchChildren(newSource, mc, 0);

        if (mc == null) {
            return null;
        }

        sourceData.next();
        return mc;
    }
}
