/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;

import org.jspecify.annotations.NonNull;

public class StructDeclaration extends Declaration {
    List<FieldDeclaration> fields;

    public StructDeclaration(@NonNull Name name, List<FieldDeclaration> fields) {
        super(name);
        this.fields = fields;
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
}
