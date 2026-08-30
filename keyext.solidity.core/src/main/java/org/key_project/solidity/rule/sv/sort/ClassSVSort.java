/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;

/// Program schema variable sort whose match condition is a plain instance-of check against a
/// single program element class.
final class ClassSVSort extends ProgramSVSort {

    private final Class<?> matchedClass;

    ClassSVSort(String name, Class<?> matchedClass) {
        super(new Name(name));
        this.matchedClass = matchedClass;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        return matchedClass.isInstance(pe);
    }
}
