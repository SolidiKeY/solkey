/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

public abstract class ProgramTransformer implements Statement, SolidityProgramElement {
    /// the name of the meta construct
    private final Name name;
    /// the encapsulated program element
    private final SolidityProgramElement body;

    protected ProgramTransformer(Name name, SolidityProgramElement body) {
        this.name = name;
        this.body = body;
    }

    /// performs the program transformation needed for symbolic program transformation
    ///
    /// @param pe the SolidityProgramElement on which the execution is performed
    /// @param services the Services with all necessary information about the java programs
    /// @param svInst the instantiations of the schemavariables
    /// @return the transformated program
    public abstract SolidityProgramElement[] transform(SolidityProgramElement pe, Services services,
            SVInstantiations svInst);

    /// returns the name of the meta construct
    ///
    /// @return the name of the meta construct
    public Name name() {
        return name;
    }

    public SolidityProgramElement body() {
        return body;
    }


    @Override
    public void visit(Visitor v) {
        v.performActionOnProgramMetaConstruct(this);
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (n == 0) {
            return body;
        }
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    /// to String
    public String toString() {
        return name + "( " + body + ");";
    }
}
