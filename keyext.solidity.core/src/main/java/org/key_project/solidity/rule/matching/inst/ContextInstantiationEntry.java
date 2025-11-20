/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.matching.inst;

import org.key_project.prover.rules.instantiation.InstantiationEntry;
import org.key_project.solidity.program.PosInProgram;
import org.key_project.solidity.program.ast.SolidityProgramElement;

/// This class is used to store the information about a matched context of a dl formula. (the pi and
/// omega part)
public class ContextInstantiationEntry
        extends InstantiationEntry<ContextBlockExpressionInstantiation> {

    /// creates a new ContextInstantiationEntry
    ///
    /// @param pi the PosInProgram describing the position of the first statement after the prefix
    /// @param omega the PosInProgram describing the position of the statement just before the
    /// suffix
    /// starts
    /// @param pe the ProgramElement the context positions are related to
    ContextInstantiationEntry(PosInProgram pi, PosInProgram omega,
            SolidityProgramElement pe) {
        super(new ContextBlockExpressionInstantiation(pi, omega, pe));
    }

    /// returns the position of the first statement after the prefix
    ///
    /// @return the position of the first statement after the prefix
    public PosInProgram prefix() {
        return getInstantiation().prefix();
    }


    /// returns the position of the statement just before the suffix starts
    ///
    /// @return the position of the statement just before the suffix starts
    public PosInProgram suffix() {
        return getInstantiation().suffix();
    }

    /// returns the context program with an ignorable part between prefix and suffix position
    public SolidityProgramElement contextProgram() {
        return getInstantiation().programElement();
    }

    /// toString
    public String toString() {
        return "[\npi:" + prefix() + "\nomega:" + suffix() + "\n]";
    }
}
