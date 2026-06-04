/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.io;

import java.io.File;
import java.nio.file.Path;

import org.key_project.logic.Named;
import org.key_project.solidity.common.Profile;
import org.key_project.solidity.proof.init.Includes;
import org.key_project.solidity.proof.init.InitConfig;
import org.key_project.solidity.proof.init.ProofInputException;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.Nullable;

public interface EnvInput extends Named {
    /// Returns the total numbers of chars that can be read in this input.
    int getNumberOfChars();

    /// Sets the initial configuration the read environment input should be added to. Must be called
    /// before calling any of the read* methods.
    void setInitConfig(InitConfig initConfig);

    /// Reads the include section and returns an Includes object.
    Includes readIncludes() throws ProofInputException;

    /// Reads the Rust path.
    Path readSolidityPath() throws ProofInputException;

    /// Returns the file path to specific requested Rust file.
    default Path getSolidityFile() throws ProofInputException {
        return null;
    }

    /// Reads the input using the given modification strategy, i.e., parts of the input do not
    /// modify
    /// the initial configuration while others do.
    ///
    /// @return The found warnings or an empty [ImmutableSet] if no warnings occurred.
    @Nullable
    ImmutableSet<String> read() throws ProofInputException;

    /// Returns the [Profile] to use.
    ///
    /// @return The [Profile] to use.
    Profile getProfile();

    /// Returns the initial [File] which is loaded if available.
    ///
    /// @return The initial [File] which is loaded or `null` otherwise.
    Path getInitialFile();
}
