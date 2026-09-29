/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.expressions;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.StaticTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import static org.key_project.solidity.program.ast.SolidityProgramElement.takeChild;


public class IndexExpression extends SolidityExpression {

    private final Expression leftExp;
    private final Expression indexExp;

    public IndexExpression(Expression leftExp, Expression indexExp) {
        super(elementTypeOf(leftExp.getType()));
        this.leftExp = leftExp;
        this.indexExp = indexExp;
    }

    // A schematic container expression (e.g. `s#gp` in a taclet template) has no
    // resolved type, so `containerType` may be null here; in that case the element type is also
    // unknown (null). The @NonNull-by-default field tolerates this for templates, so we suppress
    // the resulting return-type warning rather than make every expression's type @Nullable.
    @SuppressWarnings("return.type.incompatible")
    private static Type elementTypeOf(Type containerType) {
        if (containerType == null) {
            return null;
        }
        Type elementType = StaticTypes.elementTypeOf(containerType);
        return elementType != null ? elementType : containerType;
    }

    public IndexExpression(ExtList children, Type type) {
        super(type != null ? type : elementTypeOf(containerTypeOf(children)));
        this.leftExp = takeChild(children, Expression.class);
        this.indexExp = takeChild(children, Expression.class);
    }

    @SuppressWarnings("return.type.incompatible")
    private static Type containerTypeOf(ExtList children) {
        Expression leftExp = children.get(Expression.class);
        return leftExp == null ? null : leftExp.getType();
    }

    @Override
    public SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> leftExp;
            case 1 -> indexExp;
            default -> throw outOfBounds(n);
        };
    }

    @Override
    public int getChildCount() {
        return 2;
    }

    public Expression getLeftExp() { return leftExp; }

    public Expression getIndexExp() { return indexExp; }

    public String toString() {
        return leftExp + "[" + indexExp + "]";
    }

    public void visit(Visitor v) {
        v.performActionOnIndexExpression(this);
    }
}
