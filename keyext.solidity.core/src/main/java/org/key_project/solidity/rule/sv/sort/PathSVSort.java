/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.references.FieldReference;

final class PathSVSort extends ProgramSVSort {

    enum Location {
        STORAGE, MEMORY
    }

    enum Simplicity {
        ANY, SIMPLE, COMPLEX
    }

    private record PathInfo(Location location, boolean simple) {
    }

    private final Location location;
    private final Simplicity simplicity;

    PathSVSort(String name, Location location, Simplicity simplicity) {
        super(new Name(name));
        this.location = location;
        this.simplicity = simplicity;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        PathInfo info = classify(pe, services);
        if (info == null || info.location() != location) {
            return false;
        }
        return switch (simplicity) {
            case ANY -> true;
            case SIMPLE -> info.simple();
            case COMPLEX -> !info.simple();
        };
    }

    private static PathInfo classify(SolidityProgramElement pe, Services services) {
        if (pe instanceof FieldReference) {
            return new PathInfo(Location.STORAGE, true);
        }
        if (pe instanceof ProgramVariable pv) {
            DataLocation dataLocation = pv.getDataLocation();
            if (dataLocation == DataLocation.Storage) {
                return new PathInfo(Location.STORAGE, true);
            }
            if (dataLocation == DataLocation.Memory) {
                return new PathInfo(Location.MEMORY, true);
            }
            return null;
        }
        if (pe instanceof MemberExp member) {
            PathInfo base = classify(member.getLeftExp(), services);
            if (base == null) {
                return null;
            }
            return new PathInfo(base.location(), base.simple());
        }
        if (pe instanceof IndexExpression index) {
            PathInfo base = classify(index.getLeftExp(), services);
            if (base == null) {
                return null;
            }
            boolean simple = base.simple() && isSimpleExpression(index.getIndexExp(), services);
            return new PathInfo(base.location(), simple);
        }
        return null;
    }

    private static boolean isSimpleExpression(Expression expression, Services services) {
        return ProgramSVSort.SIMPLE_EXPRESSION.canStandFor(expression, services);
    }
}
