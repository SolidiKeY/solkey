/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.statement.Block;

public class ModifierDeclaration extends Declaration {

    private final List<ParameterDeclaration> inputParameters;
    private final Block body;
    private final Visibility visibility;

    public ModifierDeclaration(Name name, List<ParameterDeclaration> inputParameters, Block body,
            Visibility visibility) {
        super(name);
        this.inputParameters = inputParameters;
        this.body = body;
        this.visibility = visibility;
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
