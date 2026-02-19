/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.ghost;


import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.visitor.Visitor;

import java.util.List;

public class SyntaxElementList implements SolidityProgramElement {
    public List<SyntaxElement> getElements() {
        return elements;
    }

    List<SyntaxElement> elements;

    public SyntaxElementList(List<SyntaxElement> Elements) {
        this.elements = Elements;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return elements.get(n);
    }

    @Override
    public int getChildCount() {
        return elements.size();
    }

    @Override
    public void visit(Visitor v) {
    }


}
