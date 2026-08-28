/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.io;

import java.nio.file.Path;
import java.util.List;

import org.key_project.solidity.common.Profile;
import org.key_project.solidity.proof.init.SolidityProblemSpec;

import org.jspecify.annotations.Nullable;

/// This single threaded problem loader is used by the Eclipse integration of KeY.
///
/// @author Martin Hentschel
public class SingleThreadProblemLoader extends AbstractProblemLoader {
    /// Constructor.
    ///
    /// @param file The file or folder to load.
    /// @param includes Optional includes to consider.
    /// @param profileOfNewProofs The [Profile] to use for new [Proof]s.
    /// @param control The [ProblemLoaderControl] to use.
    public SingleThreadProblemLoader(Path file, @Nullable List<Path> includes,
            @Nullable Profile profileOfNewProofs,
            @Nullable ProblemLoaderControl control) {
        super(file, includes, profileOfNewProofs, control);
    }

    /// Constructor.
    ///
    /// @param solidityProblem which function of a `.sol` `file` to prove; `null` to infer it
    public SingleThreadProblemLoader(Path file, @Nullable List<Path> includes,
            @Nullable Profile profileOfNewProofs,
            @Nullable ProblemLoaderControl control,
            @Nullable SolidityProblemSpec solidityProblem) {
        super(file, includes, profileOfNewProofs, control, solidityProblem);
    }
}
