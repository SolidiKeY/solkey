/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.ast;

import org.key_project.logic.SyntaxElement;

public interface SolidityProgramElement extends SyntaxElement {

    @Override
    public abstract SyntaxElement getChild(int n);

    @Override
    public int getChildCount();

}
