/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.HashMap;
import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NonNull;

public class ContractReference extends SolidityExpression implements Resolver, VariableReference {

    public final int id;

    public ContractDeclaration getContractDeclaration() {
        return Objects.requireNonNull(contractDeclaration, "contract reference is not resolved");
    }

    private @MonotonicNonNull ContractDeclaration contractDeclaration;

    public ContractReference(int id, Type type) {
        super(type);
        this.id = id;
    }

    public ContractReference(ContractDeclaration contractDeclaration, Type type, int id) {
        super(type);
        this.id = id;
        this.contractDeclaration = contractDeclaration;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return contractDeclaration == null ? "<unresolved contract>"
                : contractDeclaration.name().toString();
    }

    @Override
    public void resolve(HashMap<Integer, SyntaxElement> id2Name) {
        if (this.contractDeclaration == null)
            this.contractDeclaration =
                Objects.requireNonNull((ContractDeclaration) id2Name.get(id));
        else
            throw new IllegalStateException(
                "contract " + contractDeclaration.name() + " has already been resolved");
    }

    @Override
    public ContractDeclaration mainProgramElement() {
        return Objects.requireNonNull(contractDeclaration, "contract reference is not resolved");
    }

    public void visit(Visitor v) {
        v.performActionOnContractReference(this);
    }
}
