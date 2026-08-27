/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

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
        for (String rawFlag : parameter.split(",")) {
            String flag = rawFlag.toLowerCase(Locale.ROOT);
            switch (flag) {
                case "storage" -> filters.location.set(Location.STORAGE, flag);
                case "memory" -> filters.location.set(Location.MEMORY, flag);
                case "simple" -> filters.simplicity.set(Simplicity.SIMPLE, flag);
                case "complex", "nonsimple", "non-simple" -> filters.simplicity
                        .set(Simplicity.COMPLEX, flag);
                case "local" -> filters.origin.set(Origin.LOCAL, flag);
                case "global" -> filters.origin.set(Origin.GLOBAL, flag);
                case "array" -> filters.kind.set(Kind.ARRAY, flag);
                case "mapping" -> filters.kind.set(Kind.MAPPING, flag);
                default -> throw new IllegalArgumentException(
                    "Unknown Path sort flag '" + rawFlag + "'");
            }
        }
        if (filters.location.value == Location.MEMORY && filters.origin.value == Origin.GLOBAL) {
            throw new IllegalArgumentException(
                "Memory paths are always local; use 'memory' or 'memory,local'");
        }
        ProgramSVSort result = new PathSVSort("Path[" + parameter + "]", filters.location.value,
            filters.simplicity.value, filters.origin.value, filters.kind.value);
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
        private final Filter<Location> location = new Filter<>(Location.ANY);
        private final Filter<Simplicity> simplicity = new Filter<>(Simplicity.ANY);
        private final Filter<Origin> origin = new Filter<>(Origin.ANY);
        private final Filter<Kind> kind = new Filter<>(Kind.ANY);
    }

    /** One filter axis: its value plus the flag that set it, so conflicts can be reported. */
    private static final class Filter<T> {
        private T value;
        private String flag;

        private Filter(T unrestricted) {
            this.value = unrestricted;
        }

        private void set(T newValue, String newFlag) {
            if (flag != null) {
                throw new IllegalArgumentException(
                    "Conflicting Path sort flags '" + flag + "' and '" + newFlag + "'");
            }
            this.value = newValue;
            this.flag = newFlag;
        }
    }
}
