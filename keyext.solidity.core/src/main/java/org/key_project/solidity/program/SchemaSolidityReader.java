package org.key_project.solidity.program;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Namespace;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.op.ProgramVariable;

public class SchemaSolidityReader extends SolidityReader {

    private Namespace schemaVariables;

    public SchemaSolidityReader(Services services, NamespaceSet nss) {
        super(services,nss);
    }

    public void setSVNamespace(Namespace<@NonNull SchemaVariable> schemaVariables) {
        this.schemaVariables = schemaVariables;
    }
}
