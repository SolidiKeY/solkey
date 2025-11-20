/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.matching.inst;

import org.key_project.prover.rules.instantiation.InstantiationEntry;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.util.collection.ImmutableArray;

/// This class is used to store the instantiation of a schemavariable if it is a ProgramElement.
public class ProgramListInstantiation
        extends InstantiationEntry<ImmutableArray<SolidityProgramElement>> {

    /// creates a new ContextInstantiationEntry
    ///
    /// @param pes the ProgramElement array the SchemaVariable is instantiated with
    ProgramListInstantiation(ImmutableArray<SolidityProgramElement> pes) {
        super(pes);
    }
}
