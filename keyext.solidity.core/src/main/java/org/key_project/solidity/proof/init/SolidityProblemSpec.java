/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import org.jspecify.annotations.Nullable;

/// Which function of a `.sol` file to build a proof obligation for.
///
/// A `null` component means "infer it": the only contract in the file, or — when the caller
/// wants the whole file — every provable function in turn.
public record SolidityProblemSpec(@Nullable String contract, @Nullable String function) {

    public static SolidityProblemSpec of(String contract, String function) {
        return new SolidityProblemSpec(contract, function);
    }
}
