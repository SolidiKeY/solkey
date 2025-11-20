/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.common;

import org.key_project.logic.Name;
import org.key_project.solidity.strategy.StrategyFactory;

public class Profile {
    public boolean supportsStrategyFactory(Name strategy) {
        throw new RuntimeException("Not implemented yet");
    }

    public StrategyFactory getStrategyFactory(Name strategy) {
        throw new RuntimeException("Not implemented yet");
    }

    public StrategyFactory getDefaultStrategyFactory() {
        throw new RuntimeException("Not implemented yet");
    }
}
