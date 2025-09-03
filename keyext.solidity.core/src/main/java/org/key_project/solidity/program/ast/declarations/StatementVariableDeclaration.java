package org.key_project.solidity.program.ast.declarations;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;

public class StatementVariableDeclaration extends Declaration {
    private String struct;
    private final DataLocation dataLocation;

    public StatementVariableDeclaration(@NonNull Name name, String struct, DataLocation dataLocation) {
        super(name);
        this.struct = struct;
        this.dataLocation = dataLocation;
    }

    @Override
    public SyntaxElement getChild(int n) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public String getStruct() {
        return struct;
    }

    @Override
    public String toString() {
        return getStruct() + " " + dataLocation + " " + getName();
    }
}
