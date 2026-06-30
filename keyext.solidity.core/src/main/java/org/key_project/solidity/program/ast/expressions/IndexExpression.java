/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;

public class IndexExpression extends SolidityExpression {

    @NonNull
    Expression leftExp;
    @NonNull
    Expression indexExp;

    public IndexExpression(Expression leftExp, Expression indexExp) {
        super(elementTypeOf(leftExp.getType()));
        this.leftExp = leftExp;
        this.indexExp = indexExp;
    }

    // A schematic container expression (e.g. `s#indexedRoot` in a taclet template) has no
    // resolved type, so `containerType` may be null here; in that case the element type is also
    // unknown (null). The @NonNull-by-default field tolerates this for templates, so we suppress
    // the resulting return-type warning rather than make every expression's type @Nullable.
    @SuppressWarnings("return.type.incompatible")
    private static Type elementTypeOf(Type containerType) {
        if (containerType == null) {
            return null;
        }
        return switch (containerType) {
            case MappingType m -> m.valueType();
            case ArrayType a -> a.getElementType();
            case DynamicArrayType d -> d.getElementType();
            default -> containerType;
        };
    }

    public IndexExpression(ExtList children, Type type) {
        super(type);
        this.leftExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
        this.indexExp = Objects.requireNonNull(children.removeFirstOccurrence(Expression.class));
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> leftExp;
            case 1 -> indexExp;
            default -> throw new IndexOutOfBoundsException(
                "Index should be 0 <= " + n + " < " + getChildCount());
        };
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    public @NonNull Expression getLeftExp() { return leftExp; }

    public @NonNull Expression getIndexExp() { return indexExp; }

    public String toString() {
        return leftExp + "[" + indexExp + "]";
    }

    public void visit(Visitor v) {
        v.performActionOnIndexExpression(this);
    }
}
