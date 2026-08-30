/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;


import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;


public class ArrayType implements Type, SyntaxElement {
    private final Name name;
    private final Type type;
    private final int length;

    public ArrayType(Type type, int length) {
        this.type = type;
        this.length = length;
        this.name = new Name(type + "[" + length + "]");
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0)
            return type;
        throw new IndexOutOfBoundsException(
            "Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        return type + "[" + length + "]";
    }

    public Type getElementType() { return type; }

    public int length() {
        return length;
    }
}
