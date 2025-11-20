/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.visitor.Visitor;

public class ArrayDeclaration extends Declaration {
    int length;
    String struct;

    public ArrayDeclaration(Name name, String struct, int length) {
        super(name);
        this.struct = struct;
        this.length = length;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return struct + "[" + length + "]" + " memory " + name;
    }

    public void visit(Visitor v){
        v.performActionOnArrayDeclaration(this);
    }
}
