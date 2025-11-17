/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.theory;

import org.key_project.logic.Name;
import org.key_project.logic.op.Function;
import org.key_project.solidity.common.Services;

/// should not be abstract
public abstract class IntLDT extends LDT {
    protected IntLDT(Name name, Services services) {
        super(name, services);
    }

    public Function getNegativeNumberSign() {
        throw new UnsupportedOperationException();
    }
}
