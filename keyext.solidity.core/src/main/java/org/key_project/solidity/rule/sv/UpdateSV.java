/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv;

import org.key_project.logic.Name;
import org.key_project.logic.TerminalSyntaxElement;
import static org.key_project.solidity.logic.SolidityDLTheory.UPDATE;

import org.jspecify.annotations.NonNull;

public class UpdateSV extends OperatorSV implements TerminalSyntaxElement {
    UpdateSV(Name name) {
        super(name, UPDATE, false, true);
    }


    @Override
    public @NonNull String toString() {
        return toString("update");
    }
}
