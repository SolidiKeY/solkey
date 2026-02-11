/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramFunction;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.util.collection.ImmutableList;

///
/// This abstract implementation of [ProofOblInput] extends the functionality of
/// [AbstractPO] to execute some code.
///
///
/// The generated [Sequent] has the following form:
/// <pre>
///
/// `==><generalAssumptions>
/// &<preconditions>-><updatesToStoreInitialValues><modalityStart>panics=false;<customCode><modalityEnd>(<postconditions
/// > & <optionalUninterpretedPredicate>)`
/// </pre>
///
///
/// If [#isAddUninterpretedPredicate()] an uninterpreted predicate is added to the
/// postcondition which contains the heap and all parameters as argument. This predicate can be used
/// to filter out invalid execution paths because its branches are closed while still open branches
/// contains valid execution paths.
///
///
/// @author Martin Hentschel
public abstract class AbstractOperationPO extends AbstractPO {
    protected InitConfig proofConfig;

    public AbstractOperationPO(InitConfig proofConfig, Name name) {
        super(proofConfig, name);
    }

    protected Services postInit() {
        proofConfig = environmentConfig.deepCopy();
        final Services proofServices = proofConfig.getServices();
        tb = proofServices.getTermBuilder();
        return proofServices;
    }

    @Override
    public void readProblem() throws ProofInputException {
        assert proofConfig == null;
        final Services proofServices = postInit();
        final ProgramFunction fn = getProgramFunction();

        // prepare variables, program method
        boolean makeNamesUnique = isMakeNamesUnique();
        final ImmutableList<ProgramVariable> paramVars = null; // tb.paramVars(fn, makeNamesUnique);
        final ProgramVariable resultVar = null;// tb.resultVar(fn, makeNamesUnique);

        register(paramVars, new ProgramVariable[] { resultVar }, proofServices);

        final Term termPO = createPOTerm(fn, paramVars, resultVar, proofServices);

        assignPOTerm(termPO);
    }

    protected abstract ProgramFunction getProgramFunction();

    @Override
    protected InitConfig getCreatedInitConfigForSingleProof() {
        return proofConfig;
    }

    /// Checks if result variable and call arguments should
    /// be renamed to make sure that their names are unique in the whole KeY application.
    ///
    /// @return `true` use unique names, `false` use original names even if they are not
    /// unique in whole KeY application.
    protected boolean isMakeNamesUnique() {
        // Changing this behaviour to fix #1552.
        // return true;
        return false;
    }

    /// Checks if a copy of the call arguments are used instead of the original
    /// arguments.
    ///
    /// @return `true` use copy of method call arguments, `false` use original method
    /// call arguments.
    protected boolean isCopyOfArgumentsUsed() {
        return true;
    }

    private void register(final ImmutableList<ProgramVariable> paramVars,
            final ProgramVariable[] vars,
            final Services proofServices) {
        // register the variables so they are declared in proof header
        // if the proof is saved to a file
        register(paramVars, proofServices);
        for (ProgramVariable var : vars) {
            register(var, proofServices);
        }
    }

    private Term createPOTerm(ProgramFunction fn, final ImmutableList<ProgramVariable> paramVars,
            final ProgramVariable resultVar, final Services proofServices) {
        throw new RuntimeException("Not implemented yet");
    }


    /// Returns the [SModality.SolidityModalityKind] to use as termination
    /// marker.
    ///
    /// @return The [SModality.SolidityModalityKind] to use as termination marker.
    protected abstract SModality.SolidityModalityKind getTerminationMarker();


}
