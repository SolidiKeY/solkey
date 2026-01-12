/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

public class ArrayDeclaration extends Declaration {
    int length;
    String struct;
    Name name;

    public ArrayDeclaration(Name name, String struct, int length) {
        super(new ImmutableArray<>());
        this.name = name;
        this.struct = struct;
        this.length = length;
    }

    public ArrayDeclaration(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(ImmutableArray.class)));
        this.struct = Objects.requireNonNull(children.removeFirstOccurrence(String.class));;
        this.length = Objects.requireNonNull(children.removeFirstOccurrence(int.class));;
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

    public void visit(Visitor v) {
        v.performActionOnArrayDeclaration(this);
    }
}
