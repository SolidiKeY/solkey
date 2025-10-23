/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.inst;

import org.key_project.prover.rules.instantiation.SVInstantiations;

/// TO Be Implemented
public abstract class SolSVInstantiations implements SVInstantiations {
    public static final SVInstantiations EMPTY_SVINSTANTIATIONS = null;

    public GenericSortInstantiations getGenericSortInstantiations() {
        throw new RuntimeException("Not implemented yet");
    }
}
