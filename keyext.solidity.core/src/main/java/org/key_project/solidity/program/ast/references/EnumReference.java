/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import org.key_project.solidity.program.ast.LeafProgramElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.EnumDeclaration;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import static org.key_project.solidity.program.ast.SolidityProgramElement.takeChild;


/// TODO: Unclear what an enumreference is? Is it the name of a member or of the type
/// In the first case, why is it a variable reference?
public class EnumReference extends SolidityExpression
        implements VariableReference, LeafProgramElement {
    private final EnumDeclaration enumDeclaration;

    public EnumReference(EnumDeclaration enumDeclaration, Type type) {
        super(type);
        this.enumDeclaration = enumDeclaration;
    }

    public EnumReference(ExtList children, Type type) {
        super(type);
        this.enumDeclaration =
            takeChild(children, EnumDeclaration.class);
    }

    @Override
    public String toString() {
        return enumDeclaration.getName().toString();
    }

    @Override
    public EnumDeclaration mainProgramElement() {
        return enumDeclaration;
    }

    public void visit(Visitor v) {
        v.performActionOnEnumReference(this);
    }
}
