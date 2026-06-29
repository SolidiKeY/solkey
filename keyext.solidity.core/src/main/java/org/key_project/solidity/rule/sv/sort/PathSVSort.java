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
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.references.FieldReference;

final class PathSVSort extends ProgramSVSort {
    private static final Map<String, ProgramSVSort> PARAMETERIZED_SORTS = new HashMap<>();

    private enum Axis {
        LOCATION, SIMPLICITY, ORIGIN, KIND
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

    private enum Kind {
        ANY, ARRAY, MAPPING
    }

    private record PathInfo(Location location, boolean simple, Origin origin, Kind kind) {
    }

    private final Location location;
    private final Simplicity simplicity;
    private final Origin origin;
    private final Kind kind;

    PathSVSort(String name, Location location, Simplicity simplicity) {
        this(name, location, simplicity, Origin.ANY, Kind.ANY);
    }

    private PathSVSort(String name, Location location, Simplicity simplicity, Origin origin,
            Kind kind) {
        super(new Name(name));
        this.location = location;
        this.simplicity = simplicity;
        this.origin = origin;
        this.kind = kind;
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
        if (kind != Kind.ANY && info.kind() != kind) {
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
                case "array" -> filters.set(Axis.KIND, flag, seen,
                    () -> filters.kind = Kind.ARRAY);
                case "mapping" -> filters.set(Axis.KIND, flag, seen,
                    () -> filters.kind = Kind.MAPPING);
                default -> throw new IllegalArgumentException(
                    "Unknown Path sort flag '" + rawFlag + "'");
            }
        }
        if (filters.location == Location.MEMORY && filters.origin == Origin.GLOBAL) {
            throw new IllegalArgumentException(
                "Memory paths are always local; use 'memory' or 'memory.local'");
        }
        ProgramSVSort result = new PathSVSort("Path[name=" + parameter + "]", filters.location,
            filters.simplicity, filters.origin, filters.kind);
        PARAMETERIZED_SORTS.put(parameter, result);
        return result;
    }

    private static PathInfo classify(SolidityProgramElement pe, Services services) {
        if (pe instanceof FieldReference) {
            return new PathInfo(Location.STORAGE, true, Origin.GLOBAL, kindOf(pe));
        }
        if (pe instanceof ProgramVariable pv) {
            DataLocation dataLocation = pv.getDataLocation();
            if (dataLocation == DataLocation.Storage) {
                return new PathInfo(Location.STORAGE, true, Origin.LOCAL, kindOf(pe));
            }
            if (dataLocation == DataLocation.Memory) {
                return new PathInfo(Location.MEMORY, true, Origin.LOCAL, kindOf(pe));
            }
            return new PathInfo(Location.ANY, true, Origin.LOCAL, kindOf(pe));
        }
        if (pe instanceof MemberExp member) {
            PathInfo base = classify(member.getLeftExp(), services);
            if (base == null) {
                return null;
            }
            return new PathInfo(base.location(), false, base.origin(), kindOf(pe));
        }
        if (pe instanceof IndexExpression index) {
            PathInfo base = classify(index.getLeftExp(), services);
            if (base == null) {
                return null;
            }
            return new PathInfo(base.location(), false, base.origin(), kindOf(pe));
        }
        // A no-arg `arr.push()` returns the freshly appended slot: a complex storage
        // location rooted at the array receiver, with the array's element type. Treating
        // it as a complex path lets the ordinary complex-receiver unfold rules capture it.
        if (pe instanceof FunctionCallExpression call && isNoArgPush(call)) {
            PathInfo base = classify(((MemberExp) call.getFunctionExp()).getLeftExp(), services);
            if (base == null) {
                return null;
            }
            return new PathInfo(base.location(), false, base.origin(), kindOf(pe));
        }
        return null;
    }

    private static boolean isNoArgPush(FunctionCallExpression call) {
        return call.getArguments().isEmpty()
                && call.getFunctionExp() instanceof MemberExp m
                && m.getRightExp() instanceof FunctionDeclaration fd
                && "push".equals(fd.name().toString());
    }

    private static Kind kindOf(SolidityProgramElement pe) {
        if (!(pe instanceof Expression expression)) {
            return Kind.ANY;
        }
        Type type = unwrap(expression.getType());
        if (type instanceof DynamicArrayType || type instanceof ArrayType) {
            return Kind.ARRAY;
        }
        if (type instanceof MappingType) {
            return Kind.MAPPING;
        }
        return Kind.ANY;
    }

    private static Type unwrap(Type type) {
        if (type instanceof KeYSolidityType keyType && keyType.getSolidityType() != null) {
            return keyType.getSolidityType();
        }
        return type;
    }

    private static final class PathFilters {
        private Location location = Location.ANY;
        private Simplicity simplicity = Simplicity.ANY;
        private Origin origin = Origin.ANY;
        private Kind kind = Kind.ANY;

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
