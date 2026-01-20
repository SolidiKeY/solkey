/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.HashMap;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class ContractReference extends Expression implements Resolver, VariableReference {

    public int id;
    public final Name name;
    private ContractDeclaration contractDeclaration;

    public ContractReference(int id, Name name, Type type) {
        super(type);
        this.id = id;
        this.name = name;
    }

    public ContractReference(ExtList children, Type type, int id, Name name) {
        super(type);
        this.id = id;
        this.name = name;
        this.contractDeclaration =
            Objects.requireNonNull(children.removeFirstOccurrence(ContractDeclaration.class));
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (contractDeclaration == null)
            throw new IndexOutOfBoundsException("There is no contract to reference");
        if (n == 0)
            return contractDeclaration;
        throw new IndexOutOfBoundsException("Element " + n + " is different than 0");
    }

    @Override
    public int getChildCount() {
        return contractDeclaration == null ? 0 : 1;
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
