/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.matching.inst;

import org.key_project.prover.rules.instantiation.InstantiationEntry;
import org.key_project.solidity.program.ast.SolidityProgramElement;

/// This class is used to store the instantiation of a schemavarible if it is a ProgramElement.
public class ProgramInstantiation extends InstantiationEntry<SolidityProgramElement> {

    /// creates a new ContextInstantiationEntry
    ///
    /// @param pe the ProgramElement the SchemaVariable is instantiated with
    ProgramInstantiation(SolidityProgramElement pe) {
        super(pe);
    }
}
