package org.key_project.solidity.logic.ast.declarations;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.ast.abstractions.Type;
import org.key_project.solidity.logic.ast.expressions.Expression;

import java.util.List;

public class FunctionDeclaration extends Declaration {
    private final List<ParameterDeclaration> returnParameters;
    private final List<ParameterDeclaration> inputParameters;

    public FunctionDeclaration(Name name, List<ParameterDeclaration> returnParameters, List<ParameterDeclaration> inputParameters) {
        super(name);
        this.returnParameters = returnParameters;
        this.inputParameters = inputParameters;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public List<ParameterDeclaration> getReturnParameters() {
        return returnParameters;
    }

    public List<ParameterDeclaration> getInputParameters() {
        return inputParameters;
    }
}
