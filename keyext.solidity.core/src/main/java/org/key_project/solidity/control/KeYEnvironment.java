/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.control;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.key_project.solidity.common.Profile;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.proof.init.InitConfig;
import org.key_project.solidity.proof.init.SolidityProblemSpec;
import org.key_project.solidity.proof.io.AbstractProblemLoader;
import org.key_project.solidity.proof.io.AbstractProblemLoader.ReplayResult;
import org.key_project.solidity.proof.io.ProblemLoaderException;

import org.jspecify.annotations.Nullable;

/// Instances of this class are used to collect and access all relevant information for verification
/// with KeY.
///
/// @author Martin Hentschel
public class KeYEnvironment<U extends UserInterfaceControl> {
    /// The [UserInterfaceControl] in which the [Proof] is loaded.
    private final U ui;
    /// The loaded project.
    private final InitConfig initConfig;

    /// An optional [Proof] which was loaded by the specified proof file.
    private final Proof loadedProof;

    /// Indicates that this [KeYEnvironment] is disposed.
    private boolean disposed;

    /// The [ReplayResult] if available.
    private final ReplayResult replayResult;

    /// Constructor
    ///
    /// @param initConfig The loaded project.
    public KeYEnvironment(U ui, InitConfig initConfig, Proof loadedProof,
            ReplayResult replayResult) {
        this.ui = ui;
        this.initConfig = initConfig;
        this.loadedProof = loadedProof;
        this.replayResult = replayResult;
    }

    /// Returns the loaded project.
    ///
    /// @return The loaded project.
    public InitConfig getInitConfig() {
        return initConfig;
    }

    /// Returns the [Services] of [#getInitConfig()].
    ///
    /// @return The [Services] of [#getInitConfig()].
    public Services getServices() {
        return initConfig.getServices();
    }

    public Profile getProfile() {
        return getInitConfig().getProfile();
    }

    /// Returns the loaded [Proof] if a proof file was loaded.
    ///
    /// @return The loaded [Proof] if available and `null` otherwise.
    public Proof getLoadedProof() {
        return loadedProof;
    }

    /// Returns the [ReplayResult] if available.
    ///
    /// @return The [ReplayResult] or `null` if not available.
    public ReplayResult getReplayResult() {
        return replayResult;
    }

    /// Returns the [UserInterfaceControl] in which the [Proof] is loaded.
    ///
    /// @return The [UserInterfaceControl] in which the [Proof] is loaded.
    public U getUi() {
        return ui;
    }

    /// Returns the [ProofControl] of [#getUi()].
    ///
    /// @return The [ProofControl] of [#getUi()].
    public ProofControl getProofControl() {
        return ui != null ? ui.getProofControl() : null;
    }

    /// Loads the given location and returns all required references as [KeYEnvironment]. The
    /// `MainWindow` is not involved in the whole process.
    ///
    /// @param profile The [Profile] to use.
    /// @param location The location to load.
    /// @param includes Optional includes to consider.
    /// @param poPropertiesToForce Some optional PO [Properties] to force.
    /// @param callbackProofLoaded An optional callback (called when the proof is loaded, before
    /// replay)
    /// @param forceNewProfileOfNewProofs `` true
    /// `AbstractProblemLoader.profileOfNewProofs` will be used as [Profile] of
    /// new proofs, `false` [Profile] specified by problem file will be used for
    /// new proofs.
    /// @return The [KeYEnvironment] which contains all references to the loaded location.
    /// @throws ProblemLoaderException Occurred Exception
    public static KeYEnvironment<DefaultUserInterfaceControl> load(@Nullable Profile profile,
            Path location,
            @Nullable List<Path> includes,
            @Nullable Properties poPropertiesToForce,
            @Nullable Consumer<Proof> callbackProofLoaded,
            boolean forceNewProfileOfNewProofs) throws ProblemLoaderException {
        return load(profile, location, includes, poPropertiesToForce, callbackProofLoaded,
            forceNewProfileOfNewProofs, null);
    }

    /// Loads the given location as
    /// [#load(Profile,Path,List,Properties,Consumer,boolean)] does, selecting which function to
    /// prove when `location` is a Solidity source rather than a problem file.
    ///
    /// @param solidityProblem the function of a `.sol` `location` to prove; `null` to infer it
    public static KeYEnvironment<DefaultUserInterfaceControl> load(@Nullable Profile profile,
            Path location,
            @Nullable List<Path> includes,
            @Nullable Properties poPropertiesToForce,
            @Nullable Consumer<Proof> callbackProofLoaded,
            boolean forceNewProfileOfNewProofs,
            @Nullable SolidityProblemSpec solidityProblem) throws ProblemLoaderException {
        DefaultUserInterfaceControl ui = new DefaultUserInterfaceControl();
        AbstractProblemLoader loader = ui.load(profile, location, includes, poPropertiesToForce,
            forceNewProfileOfNewProofs, callbackProofLoaded, solidityProblem);
        InitConfig initConfig = loader.getInitConfig();

        return new KeYEnvironment<>(ui, initConfig, loader.getProof(),
            loader.getResult());
    }

    /// Loads the given location and returns all required references as [KeYEnvironment]. The
    /// `MainWindow` is not involved in the whole process.
    ///
    /// @param profile The [Profile] to use.
    /// @param location The location to load.
    /// @param includes Optional includes to consider.
    /// @param poPropertiesToForce Some optional PO [Properties] to force.
    /// @param forceNewProfileOfNewProofs `` true
    /// `AbstractProblemLoader.profileOfNewProofs` will be used as [Profile] of
    /// new proofs, `false` [Profile] specified by problem file will be used for
    /// new proofs.
    /// @return The [KeYEnvironment] which contains all references to the loaded location.
    /// @throws ProblemLoaderException Occurred Exception
    public static KeYEnvironment<DefaultUserInterfaceControl> load(@Nullable Profile profile,
            Path location,
            @Nullable List<Path> includes,
            @Nullable Properties poPropertiesToForce,
            boolean forceNewProfileOfNewProofs) throws ProblemLoaderException {
        return load(profile, location, includes, poPropertiesToForce,
            null, forceNewProfileOfNewProofs);
    }

    /// Loads the given location and returns all required references as [KeYEnvironment]. The
    /// `MainWindow` is not involved in the whole process.
    ///
    /// @param profile The [Profile] to use.
    /// @param location The location to load.
    /// @param includes Optional includes to consider.
    /// @param forceNewProfileOfNewProofs `` true
    /// `AbstractProblemLoader.profileOfNewProofs` will be used as
    /// [Profile] of new proofs, `false` [Profile] specified by problem file
    /// will be used for new proofs.
    /// @return The [KeYEnvironment] which contains all references to the loaded location.
    /// @throws ProblemLoaderException Occurred Exception
    public static KeYEnvironment<DefaultUserInterfaceControl> load(@Nullable Profile profile,
            Path location,
            @Nullable List<Path> includes,
            boolean forceNewProfileOfNewProofs) throws ProblemLoaderException {
        return load(profile, location, includes, null,
            forceNewProfileOfNewProofs);
    }

    /// Loads the given location and returns all required references as [KeYEnvironment]. The
    /// `MainWindow` is not involved in the whole process.
    ///
    /// @param location The location to load.
    /// @param includes Optional includes to consider.
    /// @return The [KeYEnvironment] which contains all references to the loaded location.
    /// @throws ProblemLoaderException Occurred Exception
    public static KeYEnvironment<DefaultUserInterfaceControl> load(Path location,
            @Nullable List<Path> includes)
            throws ProblemLoaderException {
        return load(null, location, includes, false);
    }

    public static KeYEnvironment<DefaultUserInterfaceControl> load(Path keyFile)
            throws ProblemLoaderException {
        return load(keyFile, null);
    }

    /// Loads the obligation for one function of a Solidity source file: the function is called in
    /// a modality with postcondition `true`, and the `assert` statements in its body carry the
    /// specification. No `.key` problem file is involved.
    ///
    /// @param solFile the `.sol` source to verify
    /// @param contract the contract declaring `function`, or `null` if the file declares one
    /// @param function the function to prove
    /// @throws ProblemLoaderException Occurred Exception
    public static KeYEnvironment<DefaultUserInterfaceControl> load(Path solFile,
            @Nullable String contract, String function) throws ProblemLoaderException {
        return load(null, solFile, null, null, null, false,
            new SolidityProblemSpec(contract, function));
    }

    public void dispose() {
        // TODO
    }
}
