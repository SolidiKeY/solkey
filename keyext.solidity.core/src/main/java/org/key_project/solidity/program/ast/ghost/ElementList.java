/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.ghost;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

/// Ghost program element wrapping a fixed list of syntax elements. Ghost elements exist only as
/// intermediate parser results; they are never visited.
public abstract class ElementList<E extends SyntaxElement> implements SolidityProgramElement {

    private final ImmutableArray<@NonNull E> elements;

    protected ElementList(ImmutableArray<@NonNull E> elements) {
        this.elements = elements;
    }

    public ImmutableArray<@NonNull E> getElements() {
        return elements;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return Objects.requireNonNull(elements.get(n));
    }

    @Override
    public int getChildCount() {
        return elements.size();
    }

    @Override
    public void visit(Visitor v) {
    }
}
