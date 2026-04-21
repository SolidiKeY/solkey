/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.ghost;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class FunctionCallArguments implements SolidityProgramElement {
    private final ImmutableArray<Expression> args;

    public FunctionCallArguments(ExpressionList expList) {
        this.args = expList.getExpressions();
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        return args.get(n);
    }

    @Override
    public int getChildCount() {
        return args.size();
    }

    @Override
    public void visit(Visitor v) {

    }

    public ImmutableArray<Expression> getArgs() {
        return args;
    }
}
