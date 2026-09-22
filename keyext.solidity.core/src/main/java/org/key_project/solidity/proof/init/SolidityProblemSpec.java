/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// Which function of a `.sol` file to build a proof obligation for.
///
/// A `null` component means "infer it": the only contract in the file, or — when the caller
/// wants the whole file — every provable function in turn.
///
/// `choices` names the taclet options the obligation is proved under, each as
/// `category:choice`. A synthesized obligation has no `.key` file to carry a `\\withOptions`
/// declaration of its own, so this is how a caller reaches a non-default calculus.
public record SolidityProblemSpec(@Nullable String contract, @Nullable String function,
        List<String> choices) {

    public SolidityProblemSpec {
        choices = List.copyOf(choices);
    }

    public SolidityProblemSpec(@Nullable String contract, @Nullable String function) {
        this(contract, function, List.of());
    }

    public static SolidityProblemSpec of(String contract, String function) {
        return new SolidityProblemSpec(contract, function);
    }

    public SolidityProblemSpec withChoices(List<String> newChoices) {
        return new SolidityProblemSpec(contract, function, newChoices);
    }
}
