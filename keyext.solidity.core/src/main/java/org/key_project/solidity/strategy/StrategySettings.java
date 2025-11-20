/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.strategy;

import org.key_project.logic.Name;

public class StrategySettings {
    public StrategyProperties getActiveStrategyProperties() {
        throw new RuntimeException("Not implemented yet");
    }

    public Name getStrategy() {
        throw new RuntimeException("Not implemented yet");
    }

    public void setStrategy(Name strategy) {
        throw new RuntimeException("Not implemented yet");
    }

    public void setActiveStrategyProperties(StrategyProperties p) {
        throw new RuntimeException("Not implemented yet");
    }
}
