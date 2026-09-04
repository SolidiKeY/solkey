/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof;

import java.util.List;

public class Statistics {
    private final Proof proof;

    public Statistics(Proof proof) {
        this.proof = proof;
    }

    /// One measurement: its label and its value, already rendered.
    public record Entry(String label, String value) {
    }

    /// The measurements in display order, so every report of them — the CLI's printout and the
    /// GUI's proof-closed dialog — shows the same numbers.
    public List<Entry> getSummary() {
        return List.of(
            new Entry("Nodes", Integer.toString(proof.countNodes())),
            new Entry("Branches", Integer.toString(proof.root().getLeaves().size())),
            new Entry("Automode Time (in ms)", Long.toString(proof.getAutoModeTime())));
    }

    public String toString() {
        StringBuilder stats = new StringBuilder("Statistics for " + proof.name() + "\n");
        for (Entry entry : getSummary()) {
            stats.append(entry.label()).append(": ").append(entry.value()).append("\n");
        }
        return stats.toString();
    }
}
