/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.visitor;

import java.util.Objects;

import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.StatementVariableDeclaration;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.metaconstruct.ProgramTransformer;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

import org.checkerframework.checker.nullness.qual.Nullable;

public class ProgramReplaceVisitor extends CreatingASTVisitor {
    private @Nullable SolidityProgramElement result = null;

    private final SVInstantiations svinsts;

    /// create the ProgramReplaceVisitor
    ///
    /// @param root the ProgramElement where to begin
    /// @param services The Services object.
    /// @param svi Schema Variable Instantiations
    public ProgramReplaceVisitor(SolidityProgramElement root, Services services,
            SVInstantiations svi) {
        super(root, false, services);
        svinsts = svi;
    }

    /// starts the walker
    @Override
    public void start() {
        assert result == null : "ProgramReplaceVisitor is not designed for multiple walks";
        stack.push(new ExtList());
        walk(root());
        final ExtList astList = getTop();
        for (int i = 0, sz = astList.size(); result == null && i < sz; i++) {
            final Object element = astList.get(i);
            if (element instanceof SolidityProgramElement pe) {
                result = pe;
            }
        }
    }

    /// @return The result.
    public SolidityProgramElement result() {
        return Objects.requireNonNull(result);
    }

    /// the implemented default action is called if a program element is, and if it has children all
    /// its children too are left unchanged
    @Override
    protected void doDefaultAction(SolidityProgramElement x) {
        addChild(x);
    }

    @Override
    public void performActionOnSchemaVariable(SchemaVariable sv) {
        final Object inst = svinsts.getInstantiation(sv);
        if (inst instanceof SolidityProgramElement pe) {
            addChild(pe);
        } else if (inst instanceof ImmutableArray/* <ProgramElement> */) {
            @SuppressWarnings("unchecked")
            final var instArray = (ImmutableArray<SolidityProgramElement>) inst;
            // the assertion ensures the intended instanceof check from above
            addChildren(instArray);
        } else if (inst instanceof Term t && t.op() instanceof ProgramVariable pv) {
            addChild(pv);
        } else {
            throw new IllegalStateException(
                "program-replace-visitor: Instantiation missing " + "for schema variable " + sv);
        }
        changed();
    }

    /// Schematic statement declarations (`s#aliasType storage s#lp;`) appear in `\replacewith`
    /// templates. The inherited default uses the AST's `ExtList` constructor which extracts a
    /// `ProgramVariable` from the children — but only after the SV has actually been substituted
    /// in the change list. This override resolves the schema variable directly and emits a
    /// concrete `StatementVariableDeclaration` bound to the matched `ProgramVariable`, mirroring
    /// what Java's `ProgVarReplaceVisitor` does for `LocalVariableDeclaration`.
    @Override
    public void performActionOnStatementVariableDeclaration(StatementVariableDeclaration x) {
        if (x.getSchemaVariable() == null) {
            super.performActionOnStatementVariableDeclaration(x);
            return;
        }
        final SchemaVariable sv = x.getSchemaVariable();
        final Object inst = svinsts.getInstantiation(sv);
        final ProgramVariable pv;
        if (inst instanceof ProgramVariable p) {
            pv = p;
        } else if (inst instanceof Term t && t.op() instanceof ProgramVariable p) {
            pv = p;
        } else {
            throw new IllegalStateException(
                "program-replace-visitor: schematic StatementVariableDeclaration SV " + sv
                    + " not instantiated to a ProgramVariable (got: " + inst + ")");
        }
        // Discard whatever the SV-walk left on this node's child ExtList and emit a fresh
        // concrete decl into the parent's list.
        addChild(new StatementVariableDeclaration(pv));
        changed();
    }

    private void addChildren(ImmutableArray<SolidityProgramElement> arr) {
        stack.pop();
        for (int i = 0, sz = arr.size(); i < sz; i++) {
            addToTopOfStack(arr.get(i));
        }
    }

    @Override
    public void performActionOnProgramMetaConstruct(ProgramTransformer x) {
        final ExtList changeList = getTop();

        SolidityProgramElement body = null;
        for (Object element : changeList) {
            if (element instanceof SolidityProgramElement pe) {
                body = pe;
            }
        }

        assert body != null : "A program transformer without program to transform?";

        final SolidityProgramElement[] result =
            x.transform(Objects.requireNonNull(body), services, svinsts);
        if (result == null) {
            addChild(null);
        } else {
            addChildren(new ImmutableArray<>(result));
        }
        changed();
    }
}
