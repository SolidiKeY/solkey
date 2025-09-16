/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.util.collection.ImmutableArray;

public interface IObserverFunction {
    /// Returns the result type of this symbol.
    KeYSolidityType getType();

    /// Gives the number of parameters of the observer symbol.
    int getNumParams();

    /// Gives the type of the i-th parameter of this observer symbol.
    KeYSolidityType getParamType(int i);

    /// Returns the parameter types of this observer symbol.
    ImmutableArray<KeYSolidityType> getParamTypes();
}
