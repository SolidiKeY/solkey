/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic;

import org.key_project.logic.Name;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.util.collection.ImmutableArray;

public class SFunction extends Function {
    public SFunction(Name name, Sort sort) {
        super(name, new ImmutableArray<>(), sort, new ImmutableArray<>(), true, true, true);
    }
}
