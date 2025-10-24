package org.key_project.solidity.program;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.op.ProgramVariable;

public class SolidityReader {
    protected final Services services;
    protected final NamespaceSet nss;

    public SolidityReader(Services services, NamespaceSet nss) {
        this.services = services;
        this.nss = nss;
    }

    public SolidityBlock readBlockWithProgramVariables(Namespace<@NonNull ProgramVariable> programVariableNamespace, String solidity) {
        throw new RuntimeException("Not implemented yet");
    }

    public SolidityBlock readBlockWithEmptyContext(String solidity) {
        throw new RuntimeException("Not implemented yet");
    }
}
