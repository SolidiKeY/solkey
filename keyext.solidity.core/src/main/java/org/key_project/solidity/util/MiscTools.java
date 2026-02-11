/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.util;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.expressions.operators.AssignmentExpression;
import org.key_project.solidity.program.ast.visitor.SolidityASTVisitor;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

public final class MiscTools {
    /// All variables read in the specified program element, excluding newly declared variables.
    ///
    /// @param pe a program element.
    /// @param services services.
    /// @return all variables read in the specified program element, excluding newly declared
    /// variables.
    public static ImmutableSet<ProgramVariable> getLocalIns(SolidityProgramElement pe,
            Services services) {
        final var rpvc = new ReadPVCollector(pe, services);
        rpvc.start();
        return rpvc.result();
    }

    /// All variables changed in the specified program element, excluding newly declared variables.
    ///
    /// @param pe a program element.
    /// @param services services.
    /// @return all variables changed in the specified program element, excluding newly declared
    /// variables.
    public static ImmutableSet<ProgramVariable> getLocalOuts(SolidityProgramElement pe,
            Services services) {
        final WrittenAndDeclaredPVCollector wpvc = new WrittenAndDeclaredPVCollector(pe, services);
        wpvc.start();
        return wpvc.getWrittenPVs();
    }

    // -------------------------------------------------------------------------
    // inner classes
    // -------------------------------------------------------------------------

    private static final class ReadPVCollector extends SolidityASTVisitor {
        /// The list of resulting (i.e., read) program variables.
        private ImmutableSet<ProgramVariable> result = DefaultImmutableSet.nil();

        /// The declared program variables.
        private ImmutableSet<ProgramVariable> declaredPVs =
            DefaultImmutableSet.nil();

        public ReadPVCollector(SolidityProgramElement root, Services services) {
            super(root, services);
            throw new RuntimeException(
                "Check if all left hand sides are collected correctly and then remove this throw statement");
        }

        @Override
        protected void doDefaultAction(SolidityProgramElement node) {
            if (node instanceof ProgramVariable pv) {
                if (!declaredPVs.contains(pv)) {
                    result = result.add(pv);
                }
            }
        }

        public ImmutableSet<ProgramVariable> result() {
            return result;
        }
    }

    private static class WrittenAndDeclaredPVCollector extends SolidityASTVisitor {
        /// The written program variables.
        private ImmutableSet<ProgramVariable> writtenPVs =
            DefaultImmutableSet.nil();

        /// The declared program variables.
        private ImmutableSet<ProgramVariable> declaredPVs =
            DefaultImmutableSet.nil();

        public WrittenAndDeclaredPVCollector(SolidityProgramElement root, Services services) {
            super(root, services);
        }

        @Override
        protected void doDefaultAction(SolidityProgramElement node) {
            if (node instanceof AssignmentExpression ae) {
                var lhs = ae.getChild(0);
                if (lhs instanceof ProgramVariable pv) {
                    if (!declaredPVs.contains(pv)) {
                        writtenPVs = writtenPVs.add(pv);
                    }
                }
            }
        }

        public ImmutableSet<ProgramVariable> getWrittenPVs() {
            return writtenPVs;
        }

        public ImmutableSet<ProgramVariable> getDeclaredPVs() {
            return declaredPVs;
        }
    }
}
