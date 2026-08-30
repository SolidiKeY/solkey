/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.references;

import java.util.HashMap;
import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.Resolver;
import org.key_project.solidity.program.ast.SourceData;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.expressions.SolidityExpression;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.solidity.rule.matching.inst.MatchConditions;

import org.jspecify.annotations.Nullable;

public class FunctionReference extends SolidityExpression implements Resolver, VariableReference {

    public final int id;
    private @Nullable FunctionDeclaration referencedDeclaration;

    public @Nullable FunctionDeclaration getReferencedDeclaration() {
        return referencedDeclaration;
    }

    public FunctionReference(int id, Type type) {
        super(type);
        this.id = id;
        this.referencedDeclaration = null;
    }

    public FunctionReference(@Nullable FunctionDeclaration referencedDeclaration, Type type) {
        super(type);
        this.referencedDeclaration = referencedDeclaration;
        this.id = -1;
    }

    @Override
    public String toString() {
        return referencedDeclaration == null ? "<unresolved function>"
                : referencedDeclaration.name().toString();
    }

    @Override
    public void resolve(HashMap<Integer, SyntaxElement> id2Name) {
        if (this.referencedDeclaration == null)
            this.referencedDeclaration =
                Objects.requireNonNull((FunctionDeclaration) id2Name.get(id));
        else
            throw new IllegalStateException(
                "function " + referencedDeclaration.name() + " has already been resolved");
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw outOfBounds(n);
    }

    @Override
    public int getChildCount() {
        return 0;
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
