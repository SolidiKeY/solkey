/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import org.key_project.solidity.program.ast.SolidityProgramElement;

public abstract class SolidityASTWalker {
    private final SolidityProgramElement root;

    private int depth = -1;

    public SolidityASTWalker(SolidityProgramElement root) {
        this.root = root;
    }

    public SolidityProgramElement root() {
        return root;
    }

    public void start() {
        walk(root);
    }

    public int depth() {
        return depth;
    }

    protected void walk(SolidityProgramElement node) {
        if (done()) {
            return;
        }
        if (node.getChildCount() > 0) {
            depth++;
            for (int i = 0; i < node.getChildCount() && !done(); i++) {
                walk((SolidityProgramElement) node.getChild(i));
            }
            depth--;
        }
        // Otherwise, the node is left, so perform the action
        doAction(node);
    }

    /// Hook for subclasses to short-circuit the walk early (e.g. once a collector has found
    /// everything it cares about). The default never stops early.
    protected boolean done() {
        return false;
    }

    public void run() {

    }

    protected abstract void doAction(SolidityProgramElement node);
}
