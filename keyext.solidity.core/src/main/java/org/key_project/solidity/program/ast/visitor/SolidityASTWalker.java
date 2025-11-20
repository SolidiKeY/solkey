/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;

public class SolidityASTWalker {
    public SolidityASTWalker(SolidityProgramElement program, Services services) {
    }

    public int depth() {
        throw new RuntimeException("Not implemented yet");
    }

    protected void walk(SolidityProgramElement node) {

    }

    public void run() {

    }
}
