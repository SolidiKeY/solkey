/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import java.util.Set;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;

/// A [ProgramVariableCollector] that stops walking as soon as it has found every variable in a
/// given *interested* set.
///
/// Collecting all program variables of a large program is the dominant cost of symbolic-execution
/// rule costing (it walks the whole remaining program, repeated once per step — quadratic). When
/// only a few variables matter — e.g. the left-hand-side variables of an update being simplified
/// by `\dropEffectlessElementaries` — this collector early-exits once they have all been seen, via
/// the [#done()] short-circuit of the walker.
public class BoundedProgramVariableCollector extends ProgramVariableCollector {

    private final Set<ProgramVariable> interested;

    /// @param root the program element to collect from
    /// @param services the services
    /// @param interested the variables whose presence we want to determine; the walk stops once
    /// all of them have been found
    public BoundedProgramVariableCollector(SolidityProgramElement root, Services services,
            Set<ProgramVariable> interested) {
        super(root, services);
        this.interested = interested;
    }

    @Override
    protected boolean done() {
        return result().containsAll(interested);
    }
}
