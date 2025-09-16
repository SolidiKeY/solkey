package org.key_project.solidity.program.ast.declarations;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;

import static org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation.Default;

public class StatementVariableDeclaration extends Declaration {
    private final Type type;
    private String struct;
    private final DataLocation dataLocation;

    public StatementVariableDeclaration(@NonNull Name name, Type type, String struct, DataLocation dataLocation) {
        super(name);
        this.type = type;
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
        if(struct != null)
            return struct + " " + dataLocation + " " + name();
        if(dataLocation == Default)
            return type + " " + name();
        return type + " " + dataLocation + " " + name();
    }
}
