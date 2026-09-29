/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import org.key_project.solidity.program.ast.LeafProgramElement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import static org.key_project.solidity.program.ast.SolidityProgramElement.takeChild;


public class UnresolvedTypeException extends RuntimeException implements LeafProgramElement {
    public UnresolvedTypeException(String s) {
        super(s);
    }

    public UnresolvedTypeException(ExtList children) {
        super(takeChild(children, String.class));
    }

    public void visit(Visitor v) {
        v.performActionOnUnresolvedTypeException(this);
    }
}
