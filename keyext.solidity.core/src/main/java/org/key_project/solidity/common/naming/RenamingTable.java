/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.common.naming;

import java.util.HashMap;
import java.util.Map;

import org.key_project.solidity.logic.op.ProgramVariable;

/// Records a single `\addprogvars`-induced rename so the proof node can be replayed without
/// re-running the variable namer. Mirrors Java's `de.uka.ilkd.key.logic.RenamingTable`.
public final class RenamingTable {
    private final Map<ProgramVariable, ProgramVariable> renamingMap;

    private RenamingTable(Map<ProgramVariable, ProgramVariable> renamingMap) {
        this.renamingMap = renamingMap;
    }

    public static RenamingTable getRenamingTable(
            HashMap<ProgramVariable, ProgramVariable> renamingMap) {
        return new RenamingTable(Map.copyOf(renamingMap));
    }

    public Map<ProgramVariable, ProgramVariable> getMap() {
        return renamingMap;
    }
}
