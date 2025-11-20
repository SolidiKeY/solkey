/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.strategy;

import org.key_project.solidity.proof.Goal;
import org.key_project.solidity.proof.Proof;

import org.jspecify.annotations.NonNull;

public class StrategyFactory {
    public Strategy<@NonNull Goal> create(Proof proof, StrategyProperties strategyProperties) {
        throw new RuntimeException("Not implemented yet");
    }
}
