/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.common.naming;

import java.util.HashMap;

import org.key_project.solidity.logic.op.ProgramVariable;

public class RenamingTable {
    public static RenamingTable getRenamingTable(
            HashMap<ProgramVariable, ProgramVariable> renamingMap) {
        throw new RuntimeException("Not implemented yet");
    }
}
