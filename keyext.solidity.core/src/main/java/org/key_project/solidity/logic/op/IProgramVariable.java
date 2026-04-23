/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import org.key_project.logic.op.SortedOperator;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.references.VariableReference;

public interface IProgramVariable
    // TODO: Maybe needs to implement VariableReference
        extends SolidityProgramElement, SortedOperator {
}
