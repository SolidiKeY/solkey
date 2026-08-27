/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.control;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.key_project.solidity.common.Profile;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.init.InitConfig;
import org.key_project.solidity.proof.init.ProofAggregate;
import org.key_project.solidity.proof.init.ProofInputException;
import org.key_project.solidity.proof.init.ProofOblInput;
import org.key_project.solidity.proof.init.SolidityProblemSpec;
import org.key_project.solidity.proof.io.AbstractProblemLoader;
import org.key_project.solidity.proof.io.ProblemLoaderException;

/// Provides the user interface independent logic to manage multiple proofs. This includes:
///
/// - Functionality to load files via
/// [#load(Profile,File,List,File,List,Properties,boolean,Consumer)].
/// - Functionality to instantiate new [Proof]s via
/// [#createProof(InitConfig,ProofOblInput)].
/// - Functionality to register existing [Proof]s in the user interface via
/// [#registerProofAggregate(ProofAggregate)].
///
///
/// @author Martin Hentschel
public interface UserInterfaceControl {
    ///
    /// Opens a java file in this [UserInterfaceControl] and returns the instantiated
    /// [AbstractProblemLoader] which can be used to instantiated proofs programmatically.
    ///
    ///
    /// **The loading is performed in the [Thread] of the caller!**
    ///
    ///
    /// @param profile An optional [Profile] to use. If it is `null` the default profile
    /// [#getDefaultProfile()] is used.
    /// @param file The file to open.
    /// @param includes Optional includes to consider.
    /// @param poPropertiesToForce Some optional [Properties] for the PO which extend or
    /// overwrite saved PO [Properties].
    /// @param forceNewProfileOfNewProofs `` true
    /// `AbstractProblemLoader.profileOfNewProofs` will be used as [Profile] of
    /// new proofs, `false` [Profile] specified by problem file will be used for
    /// new proofs.
    /// @param callback receives the proof after it is loaded, but before it is replayed
    /// @return The opened [AbstractProblemLoader].
    /// @throws ProblemLoaderException Occurred Exception.
    default AbstractProblemLoader load(Profile profile, Path file, List<Path> includes,
            Properties poPropertiesToForce,
            boolean forceNewProfileOfNewProofs,
            Consumer<Proof> callback) throws ProblemLoaderException {
        return load(profile, file, includes, poPropertiesToForce, forceNewProfileOfNewProofs,
            callback, null);
    }

    /// Opens a file as [#load(Profile,Path,List,Properties,boolean,Consumer)] does, selecting
    /// which function to prove when `file` is a Solidity source rather than a problem file.
    ///
    /// @param solidityProblem the function of a `.sol` `file` to prove; `null` to infer it
    AbstractProblemLoader load(Profile profile, Path file, List<Path> includes,
            Properties poPropertiesToForce,
            boolean forceNewProfileOfNewProofs,
            Consumer<Proof> callback,
            SolidityProblemSpec solidityProblem) throws ProblemLoaderException;

    /// Instantiates a new [Proof] in this [UserInterfaceControl] for the given
    /// [ProofOblInput] based on the [InitConfig].
    ///
    /// @param initConfig The [InitConfig] which provides the source code.
    /// @param input The description of the [Proof] to instantiate.
    /// @return The instantiated [Proof].
    /// @throws ProofInputException Occurred Exception.
    Proof createProof(InitConfig initConfig, ProofOblInput input) throws ProofInputException;

    /// Returns the used [ProofControl].
    ///
    /// @return The used [ProofControl].
    ProofControl getProofControl();

    /// Registers an already created [ProofAggregate] in this [UserInterfaceControl].
    ///
    /// @param pa The [ProofAggregate] to register.
    void registerProofAggregate(ProofAggregate pa);


}
