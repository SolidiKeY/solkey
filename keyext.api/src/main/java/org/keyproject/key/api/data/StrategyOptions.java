/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.keyproject.key.api.data;

import java.lang.reflect.Field;

import org.key_project.solidity.proof.Proof;
import org.key_project.solidity.settings.StrategySettings;
import org.key_project.solidity.strategy.StrategyProperties;

/**
 * @author Alexander Weigl
 * @version 1 (13.10.23)
 */
public record StrategyOptions(
        String method,
        String nonLinArith,
        String stopMode,
        int maxSteps) implements KeYDataTransferObject {
    public static StrategyOptions defaultOptions() {
        return new StrategyOptions(
            StrategyProperties.FUNCTION_CONTRACT,
            StrategyProperties.NON_LIN_ARITH_DEF_OPS,
            "STOPMODE_NONCLOSE",
            1000);
    }

    public static StrategyOptions from(StrategySettings settings) {
        var sp = settings.getActiveStrategyProperties();
        return new StrategyOptions(
            sp.getProperty(StrategyProperties.FUNCTION_OPTIONS_KEY),
            sp.getProperty(StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY),
            sp.getProperty(StrategyProperties.STOPMODE_OPTIONS_KEY),
            settings.getMaxSteps());
    }

    private String getVal(String key) {
        Field f = null;
        try {
            f = StrategyProperties.class.getField(key);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Unknown key: " + key);
        }
        Class<?> t = f.getType();
        if (t == String.class) {
            try {
                return (String) f.get(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access field: " + key);
            }
        } else {
            throw new RuntimeException("Type mismatch: " + t);
        }
    }

    public void configure(Proof proof) {
        var defaultOptions = defaultOptions();
        StrategyProperties sp = proof.getSettings().getStrategySettings()
                .getActiveStrategyProperties();
        if (method != null) {
            sp.setProperty(StrategyProperties.FUNCTION_OPTIONS_KEY, getVal(method));
        } else {
            sp.setProperty(StrategyProperties.FUNCTION_OPTIONS_KEY, defaultOptions.method());
        }
        if (nonLinArith != null) {
            sp.setProperty(StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY, getVal(nonLinArith));
        } else {
            sp.setProperty(StrategyProperties.NON_LIN_ARITH_OPTIONS_KEY,
                defaultOptions.nonLinArith());
        }
        if (stopMode != null) {
            sp.setProperty(StrategyProperties.STOPMODE_OPTIONS_KEY, getVal(stopMode));
        } else {
            sp.setProperty(StrategyProperties.STOPMODE_OPTIONS_KEY, defaultOptions.stopMode());
        }
        proof.getSettings().getStrategySettings().setActiveStrategyProperties(sp);
        proof.getSettings().getStrategySettings().setMaxSteps(maxSteps);
    }
}
