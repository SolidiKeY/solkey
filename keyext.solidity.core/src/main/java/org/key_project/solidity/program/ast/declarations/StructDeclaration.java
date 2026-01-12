/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.key_project.util.collection.ImmutableArray;

public class StructDeclaration extends Declaration {
    public final @NonNull Name name;
    List<FieldDeclaration> fields;
    

    public StructDeclaration(@NonNull Name name, List<FieldDeclaration> fields) {
        super(new ImmutableArray<>());
        this.name = name;
        this.fields = fields;
    }

    public StructDeclaration(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(ImmutableArray.class)));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
        this.fields = Objects.requireNonNull(children.removeFirstOccurrence(List.class));
    }

    public List<FieldDeclaration> getFields() {
        return fields;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (0 <= n && n < getChildCount())
            return fields.get(n);
        throw new RuntimeException("Child " + n + " out of bound");
    }

    @Override
    public int getChildCount() {
        return fields.size();
    }

    public void visit(Visitor v) {
        v.performActionOnStructDeclaration(this);
    }
}
