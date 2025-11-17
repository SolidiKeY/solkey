/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.theory;

import java.util.HashMap;
import java.util.Map;

import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;

public class TheoryInfo {

    private final Map<String, LDT> name2LDT = new HashMap<>();

    public TheoryInfo(Services services) {
        // initialize
    }

    public LDT getLDTFor(Sort sort) {
        throw new RuntimeException("Not implemented yet");
    }

    public IntLDT getIntLDT() {
        throw new RuntimeException("Not implemented yet");
    }
}
