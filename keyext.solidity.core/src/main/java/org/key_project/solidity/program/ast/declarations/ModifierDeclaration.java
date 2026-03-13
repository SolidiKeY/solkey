/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

public class ModifierDeclaration extends DeclarationClass {

    private final List<ParameterDeclaration> inputParameters;
    private final Block body;
    private final Visibility visibility;
    private final Name name;

    public ModifierDeclaration(Name name, List<ParameterDeclaration> inputParameters, Block body,
            Visibility visibility) {
        super(new ImmutableArray<>());
        this.name = name;
        this.inputParameters = inputParameters;
        this.body = body;
        this.visibility = visibility;
    }

    public ModifierDeclaration(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(ImmutableArray.class)));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
        this.inputParameters = Objects.requireNonNull(children.removeFirstOccurrence(List.class));
        this.body = Objects.requireNonNull(children.removeFirstOccurrence(Block.class));
        this.visibility = Objects.requireNonNull(children.removeFirstOccurrence(Visibility.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
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
