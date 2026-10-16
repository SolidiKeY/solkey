/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.ModifierDeclaration;
import org.key_project.solidity.program.ast.references.ModifierReference;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.FunctionFrame;
import org.key_project.solidity.program.ast.statement.PlaceholdStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.ast.visitor.CreatingASTVisitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

final class ModifierInlining extends CreatingASTVisitor {

    private final Statement replacement;
    private @Nullable SolidityProgramElement result;

    private ModifierInlining(Block body, Statement replacement, Services services) {
        super(body, services);
        this.replacement = replacement;
    }

    static boolean inlinable(ModifierReference modifier) {
        ModifierDeclaration declaration = modifier.getDeclaration();
        return declaration != null
                && declaration.getInputParameters().size() == modifier.getArguments().size()
                && hasPlaceholder(declaration.getBody());
    }

    static Statement wrap(ImmutableArray<ModifierReference> modifiers, Statement body,
            Services services) {
        Statement inner = body;
        for (int i = modifiers.size() - 1; i >= 0; i--) {
            inner = enter(modifiers.get(i), inner, services);
        }
        return inner;
    }

    private static Statement enter(ModifierReference modifier, Statement inner,
            Services services) {
        ModifierDeclaration declaration = Objects.requireNonNull(modifier.getDeclaration(),
            () -> "unresolved modifier " + modifier.name);
        ImmutableArray<ProgramVariable> parameters = declaration.getInputParameters();
        FreshVariables fresh = new FreshVariables();
        List<Statement> statements = new ArrayList<>(parameters.size() + 1);
        for (int i = 0; i < parameters.size(); i++) {
            fresh.declare(parameters.get(i), parameters.get(i).name(),
                modifier.getArguments().get(i), statements);
        }
        fresh.addLocalsOf(declaration.getBody());
        Block renamed = (Block) fresh.rename(declaration.getBody(), null, services);
        ModifierInlining inlining =
            new ModifierInlining(renamed, inner, services);
        inlining.start();
        statements.add(new FunctionFrame(
            ((Block) Objects.requireNonNull(inlining.result)).getStatements()));
        return new Block(statements);
    }

    private static boolean hasPlaceholder(SyntaxElement element) {
        if (element instanceof PlaceholdStatement) {
            return true;
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            if (hasPlaceholder(element.getChild(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        stack.push(new ExtList());
        walk(root());
        for (Object element : getTop()) {
            if (element instanceof SolidityProgramElement pe) {
                result = pe;
                return;
            }
        }
    }

    @Override
    public void performActionOnPlaceholdStatement(PlaceholdStatement x) {
        FreshVariables fresh = new FreshVariables();
        fresh.addLocalsOf(replacement);
        addChild(fresh.rename(replacement, null, services));
        changed();
    }
}
