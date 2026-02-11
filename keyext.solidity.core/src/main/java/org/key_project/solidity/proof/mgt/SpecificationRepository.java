/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.mgt;

import org.key_project.solidity.program.ast.statement.LoopStatement;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.init.ProofOblInput;
import org.key_project.solidity.speclang.Contract;
import org.key_project.solidity.speclang.LoopSpecification;
import org.key_project.util.collection.ImmutableSet;

public class SpecificationRepository {
    public Contract getContractByName(String baseContractName) {
        throw new RuntimeException("Not implemented yet");
    }

    public ImmutableSet<Contract> getAllContracts() {
        throw new RuntimeException("Not implemented yet");
    }

    public LoopSpecification getLoopSpec(LoopStatement loop) {
        throw new RuntimeException("Not implemented yet");
    }

    public void registerProof(ProofOblInput proofOblInput, Proof p) {
        throw new RuntimeException("Not implemented yet");
    }
}
