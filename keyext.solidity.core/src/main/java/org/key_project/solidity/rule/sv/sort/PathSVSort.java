/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.references.FieldReference;

final class PathSVSort extends ProgramSVSort {
    private static final Map<String, ProgramSVSort> PARAMETERIZED_SORTS = new HashMap<>();

    private enum Axis {
        LOCATION, SIMPLICITY, ORIGIN
    }

    enum Location {
        ANY, STORAGE, MEMORY
    }

    enum Simplicity {
        ANY, SIMPLE, COMPLEX
    }

    private enum Origin {
        ANY, LOCAL, GLOBAL
    }

    private record PathInfo(Location location, boolean simple, Origin origin) {
    }

    private final Location location;
    private final Simplicity simplicity;
    private final Origin origin;

    PathSVSort(String name, Location location, Simplicity simplicity) {
        this(name, location, simplicity, Origin.ANY);
    }

    private PathSVSort(String name, Location location, Simplicity simplicity, Origin origin) {
        super(new Name(name));
        this.location = location;
        this.simplicity = simplicity;
        this.origin = origin;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        PathInfo info = classify(pe, services);
        if (info == null) {
            return false;
        }
        if (location != Location.ANY && info.location() != location) {
            return false;
        }
        if (origin != Origin.ANY && info.origin() != origin) {
            return false;
        }
        return switch (simplicity) {
            case ANY -> true;
            case SIMPLE -> info.simple();
            case COMPLEX -> !info.simple();
        };
    }

    @Override
    public ProgramSVSort createInstance(String parameter) {
        ProgramSVSort cached = PARAMETERIZED_SORTS.get(parameter);
        if (cached != null) {
            return cached;
        }
        var filters = new PathFilters();
        Map<Axis, String> seen = new EnumMap<>(Axis.class);
        for (String rawFlag : parameter.split("\\.")) {
            String flag = rawFlag.toLowerCase(Locale.ROOT);
            switch (flag) {
                case "storage" -> filters.set(Axis.LOCATION, flag, seen,
                    () -> filters.location = Location.STORAGE);
                case "memory" -> filters.set(Axis.LOCATION, flag, seen,
                    () -> filters.location = Location.MEMORY);
                case "simple" -> filters.set(Axis.SIMPLICITY, flag, seen,
                    () -> filters.simplicity = Simplicity.SIMPLE);
                case "complex", "nonsimple", "non-simple" -> filters.set(Axis.SIMPLICITY, flag,
                    seen, () -> filters.simplicity = Simplicity.COMPLEX);
                case "local" -> filters.set(Axis.ORIGIN, flag, seen,
                    () -> filters.origin = Origin.LOCAL);
                case "global" -> filters.set(Axis.ORIGIN, flag, seen,
                    () -> filters.origin = Origin.GLOBAL);
                default -> throw new IllegalArgumentException(
                    "Unknown Path sort flag '" + rawFlag + "'");
            }
        }
        if (filters.location == Location.MEMORY && filters.origin == Origin.GLOBAL) {
            throw new IllegalArgumentException(
                "Memory paths are always local; use 'memory' or 'memory.local'");
        }
        ProgramSVSort result = new PathSVSort("Path[name=" + parameter + "]", filters.location,
            filters.simplicity, filters.origin);
        PARAMETERIZED_SORTS.put(parameter, result);
        return result;
    }

    private static PathInfo classify(SolidityProgramElement pe, Services services) {
        if (pe instanceof FieldReference) {
            return new PathInfo(Location.STORAGE, true, Origin.GLOBAL);
        }
        if (pe instanceof ProgramVariable pv) {
            DataLocation dataLocation = pv.getDataLocation();
            if (dataLocation == DataLocation.Storage) {
                return new PathInfo(Location.STORAGE, true, Origin.LOCAL);
            }
            if (dataLocation == DataLocation.Memory) {
                return new PathInfo(Location.MEMORY, true, Origin.LOCAL);
            }
            return new PathInfo(Location.ANY, true, Origin.LOCAL);
        }
        if (pe instanceof MemberExp member) {
            PathInfo base = classify(member.getLeftExp(), services);
            if (base == null) {
                return null;
            }
            return new PathInfo(base.location(), false, base.origin());
        }
        if (pe instanceof IndexExpression index) {
            PathInfo base = classify(index.getLeftExp(), services);
            if (base == null) {
                return null;
            }
            return new PathInfo(base.location(), false, base.origin());
        }
        return null;
    }

    private static final class PathFilters {
        private Location location = Location.ANY;
        private Simplicity simplicity = Simplicity.ANY;
        private Origin origin = Origin.ANY;

        private void set(Axis axis, String flag, Map<Axis, String> seen, Runnable setter) {
            String previous = seen.putIfAbsent(axis, flag);
            if (previous != null) {
                throw new IllegalArgumentException(
                    "Conflicting Path sort flags '" + previous + "' and '" + flag + "'");
            }
            setter.run();
        }
    }
}
