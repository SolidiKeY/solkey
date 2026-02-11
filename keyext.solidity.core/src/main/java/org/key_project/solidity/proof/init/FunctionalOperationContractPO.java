/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.io.IOException;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.logic.op.ProgramFunction;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.settings.Configuration;
import org.key_project.solidity.speclang.FunctionalOperationContract;

/// TODO: This class is just a rough template; NEEDS TO BE IMPLEMENTED FROM SCRATCH
///
/// The proof obligation for operation contracts.
///
///
/// The generated [Sequent] has the following form:
/// <pre>
///
/// `==><generalAssumptions>
/// &<preconditions>-><updatesToStoreInitialValues><modalityStart>exc=null;try{<methodBodyExpand>}catch(java.lang.Throwable
/// e){exc = e}<modalityEnd>(exc = null & <postconditions > & <optionalUninterpretedPredicate>)`
/// </pre>
///
public class FunctionalOperationContractPO extends AbstractOperationPO implements ContractPO {
    private final FunctionalOperationContract contract;
    private Term mbyAtPre;

    public FunctionalOperationContractPO(InitConfig initConfig,
            FunctionalOperationContract contract) {
        super(initConfig, new Name(contract.getName()));
        this.contract = contract;
    }

    @Override
    protected ProgramFunction getProgramFunction() {
        return getContract().getTarget();
    }

    @Override
    protected SModality.SolidityModalityKind getTerminationMarker() {
        return getContract().getModalityKind();
    }


    /// {@inheritDoc}
    @Override
    public FunctionalOperationContract getContract() {
        return contract;
    }

    @Override
    public Configuration createLoaderConfig() throws IOException {
        var c = super.createLoaderConfig();
        c.set("contract", contract.getName());
        return c;
    }

    /// {@inheritDoc}
    @Override
    public Term getMbyAtPre() {
        return mbyAtPre;
    }
}
