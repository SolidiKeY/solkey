/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program;

import org.key_project.logic.Namespace;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.op.ProgramVariable;

import org.jspecify.annotations.NonNull;

public class SolidityReader {
    protected final Services services;
    protected final NamespaceSet nss;

    public SolidityReader(Services services, NamespaceSet nss) {
        this.services = services;
        this.nss = nss;
    }

    public SolidityBlock readBlockWithProgramVariables(
            Namespace<@NonNull ProgramVariable> programVariableNamespace, String solidity) {
        throw new RuntimeException("Not implemented yet");
    }

    public SolidityBlock readBlockWithEmptyContext(String solidity) {
        throw new RuntimeException("Not implemented yet");
    }
}
