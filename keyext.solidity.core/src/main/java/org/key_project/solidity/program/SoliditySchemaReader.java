package org.key_project.solidity.program;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.program.ast.Context;
import org.key_project.solidity.program.ast.HirSolidityReader;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.rule.sv.ProgramSV;

import java.io.IOException;

public class SoliditySchemaReader extends HirSolidityReader {
    private @Nullable Namespace<@NonNull ProgramSV> svNS;

    public SoliditySchemaReader(Services services, NamespaceSet nss) {
        super(services);
        svNS = null;
    }

    public void setSVNamespace(Namespace<@NonNull ProgramSV> ns) {
        this.svNS = ns;
    }

    public Block readBlock(String block, Context context) throws IOException {
        if(svNS == null)
            return super.readBlock(block, context);
        return super.readBlock(block, context, svNS);
    }
}
