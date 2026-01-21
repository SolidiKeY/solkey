/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.ParameterDeclaration;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class ParameterVariableReference extends SolidityExpression implements VariableReference {
    public final Name name;
    private final ParameterDeclaration referencedDeclaration;

    public ParameterVariableReference(Name name, ParameterDeclaration referencedDeclaration,
            Type type) {
        super(type);
        this.name = name;
        this.referencedDeclaration = referencedDeclaration;
    }

    public ParameterVariableReference(ExtList children, Type type, Name name) {
        super(type);
        this.name = name;
        this.referencedDeclaration =
            Objects.requireNonNull(children.removeFirstOccurrence(ParameterDeclaration.class));
    }

    public ParameterDeclaration getDeclaration() {
        return referencedDeclaration;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0)
            return referencedDeclaration;
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    public void visit(Visitor v) {
        v.performActionOnParameterVariableReference(this);
    }
}
