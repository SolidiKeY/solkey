/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;


import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.Declaration;

public interface VariableReference extends SolidityProgramElement {
    Declaration mainProgramElement();

    @Override
    default int computeHashCode() {
        return 37 * SolidityProgramElement.super.computeHashCode()
                + mainProgramElement().hashCode();
    }
}
