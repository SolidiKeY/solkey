/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class ModifierDeclaration extends DeclarationClass {

    private final ImmutableArray<ProgramVariable> inputParameters;
    private final Block body;
    private final Visibility visibility;
    private final Name name;

    public ModifierDeclaration(Name name, List<ProgramVariable> inputParameters, Block body,
            Visibility visibility) {
        super(new ImmutableArray<>());
        this.name = name;
        this.inputParameters = new ImmutableArray<>(inputParameters);
        this.body = body;
        this.visibility = visibility;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return name + " () " + visibility + " " + body;
    }
}
