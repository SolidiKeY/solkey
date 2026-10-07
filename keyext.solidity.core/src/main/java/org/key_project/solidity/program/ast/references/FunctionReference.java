/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.HashMap;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.LeafProgramElement;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.SourceData;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.solidity.rule.matching.inst.MatchConditions;

import org.jspecify.annotations.Nullable;

public class FunctionReference extends SolidityExpression
        implements Resolver, VariableReference, LeafProgramElement {

    public final int id;
    private @Nullable FunctionDeclaration referencedDeclaration;
    private final @Nullable Name dispatchContract;

    public @Nullable FunctionDeclaration getReferencedDeclaration() {
        return referencedDeclaration;
    }

    public FunctionReference(int id, Type type) {
        super(type);
        this.id = id;
        this.referencedDeclaration = null;
        this.dispatchContract = null;
    }

    public FunctionReference(@Nullable FunctionDeclaration referencedDeclaration, Type type) {
        this(referencedDeclaration, type, null);
    }

    public FunctionReference(@Nullable FunctionDeclaration referencedDeclaration, Type type,
            @Nullable Name dispatchContract) {
        super(type);
        this.referencedDeclaration = referencedDeclaration;
        this.id = -1;
        this.dispatchContract = dispatchContract;
    }

    public @Nullable Name getDispatchContract() {
        return dispatchContract;
    }

    @Override
    public String toString() {
        return referencedDeclaration == null ? "<unresolved function>"
                : referencedDeclaration.name().toString();
    }

    @Override
    public void resolve(HashMap<Integer, SyntaxElement> id2Name) {
        if (id == -1)
            return;
        if (this.referencedDeclaration == null)
            this.referencedDeclaration =
                Objects.requireNonNull((FunctionDeclaration) id2Name.get(id));
        else
            throw new IllegalStateException(
                "function " + referencedDeclaration.name() + " has already been resolved");
    }

    @Override
    public FunctionDeclaration mainProgramElement() {
        return Objects.requireNonNull(referencedDeclaration, "function reference is not resolved");
    }

    @Override
    public @Nullable MatchConditions match(SourceData sourceData, @Nullable MatchConditions mc) {
        if (!(sourceData.getSource() instanceof FunctionReference sourceReference)) {
            return null;
        }
        final FunctionDeclaration thisDecl = referencedDeclaration;
        final FunctionDeclaration sourceDecl = sourceReference.referencedDeclaration;
        if (thisDecl == null || sourceDecl == null) {
            return null;
        }
        if (!thisDecl.name().equals(sourceDecl.name())) {
            return null;
        }
        sourceData.next();
        return mc;
    }

    public void visit(Visitor v) {
        v.performActionOnFunctionReference(this);
    }
}
