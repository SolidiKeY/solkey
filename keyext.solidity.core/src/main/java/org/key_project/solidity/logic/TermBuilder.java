/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic;

import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.solidity.common.Services;

public class TermBuilder {
    private final TermFactory tf;

    public TermBuilder(Services services) {
        this.tf = services.getTermFactory();
    }

    public Term func(Function op) {
        return tf.createTerm(op);
    }
}
