/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.io.IOException;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.settings.Configuration;

import org.jspecify.annotations.Nullable;

/// The proof obligation for verifying a single Solidity function directly from a `.sol`
/// file, without a hand-written `.key` problem file.
///
/// The leading `require` statements of the function body act as preconditions and every
/// `assert` statement is an obligation: the generated problem is `[{ body }] true`, so the
/// false branch of a `require` closes trivially via `revert` while the false branch of an
/// `assert` remains to be refuted.
public class FunctionVerificationPO extends AbstractPO {
    private final @Nullable String contractName;
    private final String functionName;

    public FunctionVerificationPO(InitConfig initConfig, @Nullable String contractName,
            String functionName) {
        super(initConfig,
            new Name((contractName != null ? contractName + "::" : "") + functionName));
        this.contractName = contractName;
        this.functionName = functionName;
    }

    @Override
    public void readProblem() throws ProofInputException {
        tb = environmentServices.getTermBuilder();
        FunctionDeclaration fd = findFunction();

        if (fd.getBody() == null) {
            throw new ProofInputException("Function '" + functionName
                + "' has no body to verify.");
        }

        for (var pv : fd.getInputParameters()) {
            register(pv, environmentServices);
        }
        for (var pv : fd.getReturnParameters()) {
            register(pv, environmentServices);
        }

        assignPOTerm(tb.box(new SolidityBlock(fd.getBody()), tb.tt()));
    }

    private FunctionDeclaration findFunction() throws ProofInputException {
        FunctionDeclaration fd;
        if (contractName != null) {
            ContractDeclaration contract = solidityModel.getContract(new Name(contractName));
            if (contract == null) {
                throw new ProofInputException("Unknown contract '" + contractName
                    + "'. Available contracts: " + availableContracts());
            }
            fd = contract.getFunctions().stream()
                    .filter(f -> f.name().toString().equals(functionName))
                    .findFirst().orElse(null);
            if (fd == null) {
                throw new ProofInputException("Unknown function '" + functionName
                    + "' in contract '" + contractName + "'. Available functions: "
                    + availableFunctions(contract));
            }
        } else {
            fd = solidityModel.getFunctionDeclaration(new Name(functionName));
            if (fd == null) {
                throw new ProofInputException("Unknown function '" + functionName
                    + "'. Available contracts: " + availableContracts());
            }
        }
        return fd;
    }

    private String availableContracts() {
        return solidityModel.getContracts().stream()
                .map(c -> c.name().toString())
                .collect(Collectors.joining(", "));
    }

    private String availableFunctions(ContractDeclaration contract) {
        return contract.getFunctions().stream()
                .map(f -> f.name().toString())
                .collect(Collectors.joining(", "));
    }

    @Override
    protected InitConfig getCreatedInitConfigForSingleProof() {
        return environmentConfig;
    }

    @Override
    public Configuration createLoaderConfig() throws IOException {
        var c = super.createLoaderConfig();
        if (contractName != null) {
            c.set("contract", contractName);
        }
        c.set("function", functionName);
        return c;
    }
}
