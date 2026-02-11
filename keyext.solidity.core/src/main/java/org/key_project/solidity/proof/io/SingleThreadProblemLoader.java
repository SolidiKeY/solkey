/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.io;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.List;

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
    public SingleThreadProblemLoader(File file, @Nullable List<File> includes,
            @Nullable Profile profileOfNewProofs,
            @Nullable ProblemLoaderControl control) {
        super(file, includes, profileOfNewProofs,
            control);
    }
}
