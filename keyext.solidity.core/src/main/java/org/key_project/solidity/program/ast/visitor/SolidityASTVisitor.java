/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.expressions.*;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.*;
import org.key_project.solidity.program.ast.statement.*;

public abstract class SolidityASTVisitor extends SolidityASTWalker implements Visitor {
    protected final Services services;

    public SolidityASTVisitor(SolidityProgramElement root, Services services) {
        super(root);
        this.services = services;
    }

    /// the action that is performed just before leaving the node the last time
    @Override
    protected void doAction(SolidityProgramElement node) {
        node.visit(this);
    }


    protected abstract void doDefaultAction(SolidityProgramElement node);

    @Override
    public void performActionOnDefault(SolidityProgramElement x) {
        doDefaultAction(x);
    }

    @Override
    public void performActionOnSchemaVariable(SchemaVariable x) {
        doDefaultAction((SolidityProgramElement) x);
    }
}
