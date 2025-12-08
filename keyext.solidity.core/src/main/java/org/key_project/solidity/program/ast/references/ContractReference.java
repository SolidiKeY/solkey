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
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;

public class ContractReference extends VariableReference implements Resolver {

    private int id;
    private final Name name;
    private ContractDeclaration contractDeclaration;

    public ContractReference(int id, Name name, Type type,
            ContractDeclaration contractDeclaration) {
        super(type);
        this.id = id;
        this.name = name;
        this.contractDeclaration = contractDeclaration;
    }

    public ContractReference(int id, Name name, Type type) {
        super(type);
        this.id = id;
        this.name = name;
    }

    public ContractReference(ExtList children) {
        super(Objects.requireNonNull(children.removeFirstOccurrence(Type.class)));
        this.id = Objects.requireNonNull(children.removeFirstOccurrence(int.class));
        this.name = Objects.requireNonNull(children.removeFirstOccurrence(Name.class));
        this.contractDeclaration =
            Objects.requireNonNull(children.removeFirstOccurrence(ContractDeclaration.class));
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public Declaration getDeclaration() {
        return contractDeclaration;
    }

    @Override
    public SyntaxElement getChild(int n) {
        if (contractDeclaration == null)
            throw new RuntimeException("There is no contract to reference");
        if (n == 0)
            return contractDeclaration;
        throw new RuntimeException("Element " + n + " is different than 0");
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
