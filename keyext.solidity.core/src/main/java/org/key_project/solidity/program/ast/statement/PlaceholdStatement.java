/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import org.key_project.solidity.program.ast.LeafProgramElement;
import org.key_project.solidity.program.ast.visitor.Visitor;


public class PlaceholdStatement implements Statement, LeafProgramElement {

    @Override
    public String toString() {
        return "_;";
    }

    public void visit(Visitor v) {
        v.performActionOnPlaceholdStatement(this);
    }
}
