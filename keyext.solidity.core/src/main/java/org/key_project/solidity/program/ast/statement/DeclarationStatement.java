/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.statement;

import java.util.List;
import java.util.stream.Collectors;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.expressions.Expression;

public class DeclarationStatement implements Statement {
    private final List<Declaration> declarations;
    private final Expression initialValue;

    public DeclarationStatement(List<Declaration> declarations, Expression initialValue) {
        this.declarations = declarations;
        this.initialValue = initialValue;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public List<Declaration> getDeclarations() {
        return declarations;
    }

    @Override
    public String toString() {
        String s = declarations.stream().map(Declaration::toString).collect(Collectors.joining(""));
        if (initialValue != null)
            s += " = " + initialValue.toString();
        return s + ";";
    }
}
