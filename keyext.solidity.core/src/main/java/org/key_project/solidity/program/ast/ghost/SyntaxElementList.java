/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.ghost;

import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class SyntaxElementList extends ElementList<@NonNull SyntaxElement> {

    public SyntaxElementList(List<@NonNull SyntaxElement> elements) {
        super(new ImmutableArray<>(elements));
    }
}
