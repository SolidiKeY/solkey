/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.Named;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;

public interface Type extends Named {
    @Nullable
    Sort getSort(Services services);
}
