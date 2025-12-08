/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class UnresolvedTypeException extends RuntimeException implements SolidityProgramElement {
    public UnresolvedTypeException(String s) {
        super(s);
    }

    public UnresolvedTypeException(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(String.class)));
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public void visit(Visitor v) {
        v.performActionOnUnresolvedTypeException(this);
    }
}
