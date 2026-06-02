/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof;

public class Statistics {
    private final Proof proof;

    public Statistics(Proof proof) {
        this.proof = proof;
    }

    public String toString() {
        String stats = "Statistics for " + proof.name() + "\n";

        stats += "Nodes: " + proof.countNodes() + "\n";
        stats += "Branches: " + proof.root().getLeaves().size() + "\n";
        stats += "Automode Time (in ms): " + proof.getAutoModeTime() + "\n";
        return stats;
    }
}
