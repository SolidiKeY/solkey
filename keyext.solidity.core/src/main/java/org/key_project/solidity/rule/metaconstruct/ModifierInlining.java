/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.ModifierDeclaration;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.references.ModifierReference;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.PlaceholdStatement;
import org.key_project.solidity.program.ast.statement.ReturnStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.ast.visitor.CreatingASTVisitor;
import org.key_project.solidity.program.ast.visitor.ProgVarReplaceVisitor;
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
                && count(declaration.getBody(), PlaceholdStatement.class) == 1
                && count(declaration.getBody(), ReturnStatement.class) == 0;
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
        Map<ProgramVariable, ProgramVariable> fresh = new HashMap<>();
        List<Statement> statements = new ArrayList<>(parameters.size() + 1);
        for (int i = 0; i < parameters.size(); i++) {
            ProgramVariable parameter = parameters.get(i);
            ProgramVariable local = new ProgramVariable(parameter.name(),
                parameter.getKeYSolidityType(), parameter.getDataLocation());
            fresh.put(parameter, local);
            statements.add(new DeclarationStatement(
                List.of(new StatementVariableDeclaration(local)), modifier.getArguments().get(i)));
        }
        ProgVarReplaceVisitor renaming =
            new ProgVarReplaceVisitor(declaration.getBody(), fresh, true, services);
        renaming.start();
        ModifierInlining inlining =
            new ModifierInlining((Block) renaming.result(), inner, services);
        inlining.start();
        statements.addAll(((Block) Objects.requireNonNull(inlining.result)).getStatements()
                .toList());
        return new Block(statements);
    }

    private static int count(SyntaxElement element, Class<?> kind) {
        int n = kind.isInstance(element) ? 1 : 0;
        for (int i = 0; i < element.getChildCount(); i++) {
            n += count(element.getChild(i), kind);
        }
        return n;
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
        addChild(replacement);
        changed();
    }
}
