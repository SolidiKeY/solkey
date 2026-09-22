/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import java.util.Map;
import java.util.Objects;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.util.ExtList;

import org.checkerframework.checker.nullness.qual.Nullable;

/// Walks a Solidity AST and rebuilds it with every [ProgramVariable] occurrence replaced
/// according to `replaceMap`. Mirrors Java's `de.uka.ilkd.key.java.visitor.ProgVarReplaceVisitor`.
/// Term-level replacement (modality programs, `ElementaryUpdate` LHS, etc.) lives in
/// [org.key_project.solidity.rule.execution.ProgVarReplacer], matching the Java split.
public class ProgVarReplaceVisitor extends CreatingASTVisitor {
    protected boolean replaceAllByNew = true;

    /// stores the program variables to be replaced as keys and the new program variables as values
    protected final Map<ProgramVariable, ProgramVariable> replaceMap;

    private @Nullable SolidityProgramElement result = null;

    /// creates a visitor that replaces the program variables in the given statement by new ones
    /// with
    /// the same name
    ///
    /// @param st the statement where the prog vars are replaced
    /// @param map the HashMap with the replacements
    /// @param services the services instance
    public ProgVarReplaceVisitor(SolidityProgramElement st,
            Map<ProgramVariable, ProgramVariable> map,
            boolean replaceAllByNew,
            Services services) {
        super(st, services);
        this.replaceAllByNew = replaceAllByNew;
        this.replaceMap = map;
        assert services != null;
    }

    /// the action that is performed just before leaving the node the last time
    ///
    /// @param node the node described above
    @Override
    protected void doAction(SolidityProgramElement node) {
        node.visit(this);
    }

    /// starts the walker
    @Override
    public void start() {
        stack.push(new ExtList());
        walk(root());
        ExtList el = stack.peek();
        assert el != null;
        int i = 0;
        while (!(el.get(i) instanceof SolidityProgramElement)) {
            i++;
        }
        result = (SolidityProgramElement) Objects.requireNonNull(stack.peek()).get(i);
    }

    public SolidityProgramElement result() {
        return Objects.requireNonNull(result);
    }

    @Override
    public void performActionOnProgramVariable(ProgramVariable x) {
        SolidityProgramElement newPV = replaceMap.get(x);
        if (newPV != null) {
            addChild(newPV);
            changed();
        } else {
            doDefaultAction(x);
        }
    }
}
