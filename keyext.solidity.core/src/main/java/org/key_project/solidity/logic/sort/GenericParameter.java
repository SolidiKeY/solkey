/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.sort;

import org.jspecify.annotations.NonNull;

/// Abstract parameter for [ParametricFunctionDecl] or [ParametricSortDecl]
public record GenericParameter(GenericSort sort, Variance variance) {
    @Override
    public @NonNull String toString() {
        return sort.toString();
    }

    public enum Variance {
        COVARIANT, CONTRAVARIANT, INVARIANT
    }
}

