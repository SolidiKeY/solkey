/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast.abstractions;

import org.key_project.logic.Name;

import org.jspecify.annotations.NonNull;


public interface Type {
    @NonNull
    Name getName();
}
