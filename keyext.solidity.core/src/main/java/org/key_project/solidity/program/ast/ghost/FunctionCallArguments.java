/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.ghost;

import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class FunctionCallArguments extends ElementList<@NonNull Expression> {

    public FunctionCallArguments(ExpressionList expList) {
        super(expList.getExpressions());
    }

    public ImmutableArray<@NonNull Expression> getArgs() {
        return getElements();
    }
}
