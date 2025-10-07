/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.common;


import org.jspecify.annotations.NonNull;
import org.key_project.logic.LogicServices;
import org.key_project.prover.proof.ProofServices;
import org.key_project.prover.proof.SessionCaches;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.program.ast.SolidityModel;

public class Services implements LogicServices, ProofServices {

    /**
     * proof specific namespaces (functions, predicates, sorts, variables)
     */
    private NamespaceSet namespaces = new NamespaceSet();
    private SolidityModel solidityModel;

    public Services() {}

    public @NonNull NamespaceSet getNamespaces() {
        return namespaces;
    }

    public SolidityModel getSolidityInfo() {
        return solidityModel;
    }

    @Override
    public SessionCaches getCaches() {
        return null;
    }
}
