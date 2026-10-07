/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.statement.DeclarationStatement;
import org.key_project.solidity.program.ast.statement.Statement;
import org.key_project.solidity.program.ast.visitor.ProgVarReplaceVisitor;

import org.jspecify.annotations.Nullable;

final class FreshVariables {

    private final Map<ProgramVariable, ProgramVariable> renaming = new HashMap<>();

    ProgramVariable declare(ProgramVariable original, Name name,
            @Nullable Expression initializer, List<Statement> statements) {
        ProgramVariable fresh = copy(original, name);
        renaming.put(original, fresh);
        statements.add(new DeclarationStatement(
            List.of(new StatementVariableDeclaration(fresh)), initializer));
        return fresh;
    }

    void addLocalsOf(SyntaxElement element) {
        if (element instanceof StatementVariableDeclaration declaration
                && declaration.getSchemaVariable() == null) {
            ProgramVariable local = declaration.getProgramVariable();
            renaming.putIfAbsent(local, copy(local, local.name()));
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            addLocalsOf(element.getChild(i));
        }
    }

    SolidityProgramElement rename(SolidityProgramElement body, @Nullable Name dispatchContract,
            Services services) {
        Renaming visitor = new Renaming(body, renaming, dispatchContract, services);
        visitor.start();
        return visitor.result();
    }

    private static ProgramVariable copy(ProgramVariable original, Name name) {
        return new ProgramVariable(name, original.getKeYSolidityType(),
            original.getDataLocation());
    }

    private static final class Renaming extends ProgVarReplaceVisitor {

        private final @Nullable Name dispatchContract;

        Renaming(SolidityProgramElement body, Map<ProgramVariable, ProgramVariable> renaming,
                @Nullable Name dispatchContract, Services services) {
            super(body, renaming, true, services);
            this.dispatchContract = dispatchContract;
        }

        @Override
        public void performActionOnFunctionReference(FunctionReference x) {
            if (dispatchContract == null) {
                super.performActionOnFunctionReference(x);
                return;
            }
            addChild(new FunctionReference(x.getReferencedDeclaration(), x.getType(),
                dispatchContract));
            changed();
        }
    }
}
