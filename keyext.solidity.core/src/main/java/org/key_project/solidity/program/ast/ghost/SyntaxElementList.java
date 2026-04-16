/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.ghost;


import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.collection.ImmutableArray;

public class SyntaxElementList implements SolidityProgramElement {
    public ImmutableArray<SyntaxElement> getElements() {
        return elements;
    }

    private final ImmutableArray<SyntaxElement> elements;

    public SyntaxElementList(List<SyntaxElement> elements) {
        this.elements = new ImmutableArray<>(elements);
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
