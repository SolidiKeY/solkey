/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;


public class ProgramContextAdder {
    /// singleton instance of the program context adder
    public final static ProgramContextAdder INSTANCE = new ProgramContextAdder();

    /// an empty private constructor to ensure the singleton property
    private ProgramContextAdder() {
    }
}
