/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.util.collection.ImmutableArray;

public class FunctionDeclaration extends Declaration {
    private final ImmutableArray<ParameterDeclaration> returnParameters;
    private final ImmutableArray<ParameterDeclaration> inputParameters;
    private final Block body;

    public FunctionDeclaration(Name name, List<ParameterDeclaration> returnParameters,
            List<ParameterDeclaration> inputParameters, Block body) {
        super(name);
        this.returnParameters = new ImmutableArray<>(returnParameters);
        this.inputParameters = new ImmutableArray<>(inputParameters);
        this.body = body;
    }

    public Block getBody() {
        return body;
    }

    public ImmutableArray<ParameterDeclaration> getReturnParameters() {
        return returnParameters;
    }

    public ImmutableArray<ParameterDeclaration> getInputParameters() {
        return inputParameters;
    }

    // Interface SolidityProgramElement

    @Override
    public SyntaxElement getChild(int n) {
        if (n < returnParameters.size()) {
            return returnParameters.get(n);
        } else {
            n -= returnParameters.size();
            if (n < inputParameters.size()) {
                return inputParameters.get(n);
            } else {
                n -= inputParameters.size();
                if (n == 0) {
                    return body;
                }
                throw new IndexOutOfBoundsException("Index out of bounds");
            }
        }
    }

    @Override
    public int getChildCount() {
        return returnParameters.size() + inputParameters.size();
    }

    @Override
    public String toString() {
        StringBuffer strBuffer = new StringBuffer();
        strBuffer.append("function ");
        strBuffer.append(name);
        strBuffer.append(" ()");
        strBuffer.append(getBody());
        return strBuffer.toString();
    }
}
