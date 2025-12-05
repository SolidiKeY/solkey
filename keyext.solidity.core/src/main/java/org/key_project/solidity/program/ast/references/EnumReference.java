/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import org.key_project.logic.Name;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.declarations.EnumDeclaration;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import java.util.Objects;

public class EnumReference extends VariableReference {
    private final int id;
    private final Name name;
    private final EnumDeclaration enumDeclaration;

    public EnumReference(int id, Name name, EnumDeclaration enumDeclaration, Type type) {
        super(type);
        this.id = id;
        this.name = name;
        this.enumDeclaration = enumDeclaration;
    }

    public EnumReference(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.id = Objects.requireNonNull(children.removeFirstOccurrence(int.class));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
        this.enumDeclaration = Objects.requireNonNull(children.removeFirstOccurrence(EnumDeclaration.class));
    }

    @Override
    public Name name() {
        return null;
    }

    @Override
    public Declaration getDeclaration() {
        return null;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    public void visit(Visitor v){
        v.performActionOnEnumReference(this);
    }
}
