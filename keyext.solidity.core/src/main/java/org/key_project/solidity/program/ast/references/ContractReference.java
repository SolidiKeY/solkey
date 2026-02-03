/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.HashMap;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;

public class ContractReference extends SolidityExpression implements Resolver, VariableReference {

    public int id;
    public final Name name;

    public ContractDeclaration getContractDeclaration() {
        return contractDeclaration;
    }

    private ContractDeclaration contractDeclaration;

    public ContractReference(int id, Name name, Type type) {
        super(type);
        this.id = id;
        this.name = name;
    }

    public ContractReference(ContractDeclaration contractDeclaration, Type type, int id,
            Name name) {
        super(type);
        this.id = id;
        this.name = name;
        this.contractDeclaration = contractDeclaration;
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public void resolve(HashMap<Integer, Declaration> id2Name) {
        this.contractDeclaration = (ContractDeclaration) id2Name.get(id);
    }

    public void visit(Visitor v) {
        v.performActionOnContractReference(this);
    }
}
