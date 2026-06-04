/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;


/// TODO: fix new expression:
/// Constructor arguments are missing
/// The type name (called function here?) should not be stored and not be static
public class NewExpression extends SolidityExpression {
    static String function;

    public NewExpression(String function, Type type) {
        super(type);
        this.function = function;
        /// The implementation of new expression must be fixed first before using it. See comments
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnNewExpression(this);
    }

    @Override
    public String toString() {
        return function;
    }

    public String getFunction() {
        return function;
    }

    @Override
    public int computeHashCode() {
        return 37 * super.computeHashCode() + function.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NewExpression that))
            return false;
        return Objects.equals(function, that.function) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(function, type);
    }
}
