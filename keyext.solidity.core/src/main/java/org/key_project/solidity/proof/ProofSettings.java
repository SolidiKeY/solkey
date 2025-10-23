/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof;

import org.key_project.solidity.parser.Configuration;

public class ProofSettings {
    public static final ProofSettings DEFAULT_SETTINGS = new ProofSettings();


    public ProofSettings() {
    }

    public ProofSettings(ProofSettings defaultSettings) {
        // copy passed settings
    }


    public void readSettings(Configuration c) {

    }
}
