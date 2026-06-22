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
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.EnumDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.references.FieldReference;

final class PathSVSort extends ProgramSVSort {
    private static final Map<String, ProgramSVSort> PARAMETERIZED_SORTS = new HashMap<>();

    private enum Axis {
        LOCATION, SIMPLICITY, SHAPE, CONTAINER, VALUE_KIND, ORIGIN
    }

    enum Location {
        ANY, STORAGE, MEMORY
    }

    enum Simplicity {
        ANY, SIMPLE, COMPLEX
    }

    private enum Shape {
        ANY, ROOT, FIELD, INDEX
    }

    private enum Container {
        ANY, ARRAY, MAPPING
    }

    private enum ValueKind {
        ANY, PRIMITIVE, REFERENCE
    }

    private enum Origin {
        ANY, LOCAL, GLOBAL
    }

    private record PathInfo(Location location, boolean simple, Shape shape, Container container,
            ValueKind valueKind, Origin origin) {
    }

    private final Location location;
    private final Simplicity simplicity;
    private final Shape shape;
    private final Container container;
    private final ValueKind valueKind;
    private final Origin origin;

    PathSVSort(String name, Location location, Simplicity simplicity) {
        this(name, location, simplicity, Shape.ANY, Container.ANY, ValueKind.ANY, Origin.ANY);
    }

    private PathSVSort(String name, Location location, Simplicity simplicity, Shape shape,
            Container container, ValueKind valueKind, Origin origin) {
        super(new Name(name));
        this.location = location;
        this.simplicity = simplicity;
        this.shape = shape;
        this.container = container;
        this.valueKind = valueKind;
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
        if (shape != Shape.ANY && info.shape() != shape) {
            return false;
        }
        if (container != Container.ANY && info.container() != container) {
            return false;
        }
        if (valueKind != ValueKind.ANY && info.valueKind() != valueKind) {
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
                case "root" -> filters.set(Axis.SHAPE, flag, seen,
                    () -> filters.shape = Shape.ROOT);
                case "field" -> filters.set(Axis.SHAPE, flag, seen,
                    () -> filters.shape = Shape.FIELD);
                case "index" -> filters.set(Axis.SHAPE, flag, seen,
                    () -> filters.shape = Shape.INDEX);
                case "array" -> filters.set(Axis.CONTAINER, flag, seen,
                    () -> filters.container = Container.ARRAY);
                case "mapping" -> filters.set(Axis.CONTAINER, flag, seen,
                    () -> filters.container = Container.MAPPING);
                case "primitive" -> filters.set(Axis.VALUE_KIND, flag, seen,
                    () -> filters.valueKind = ValueKind.PRIMITIVE);
                case "reference" -> filters.set(Axis.VALUE_KIND, flag, seen,
                    () -> filters.valueKind = ValueKind.REFERENCE);
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
            filters.simplicity, filters.shape, filters.container, filters.valueKind,
            filters.origin);
        PARAMETERIZED_SORTS.put(parameter, result);
        return result;
    }

    private static PathInfo classify(SolidityProgramElement pe, Services services) {
        if (pe instanceof FieldReference) {
            return new PathInfo(Location.STORAGE, true, Shape.ROOT, Container.ANY,
                valueKind(((Expression) pe).getType()), Origin.GLOBAL);
        }
        if (pe instanceof ProgramVariable pv) {
            DataLocation dataLocation = pv.getDataLocation();
            if (dataLocation == DataLocation.Storage) {
                return new PathInfo(Location.STORAGE, true, Shape.ROOT, Container.ANY,
                    valueKind(pv.getType()), Origin.LOCAL);
            }
            if (dataLocation == DataLocation.Memory) {
                return new PathInfo(Location.MEMORY, true, Shape.ROOT, Container.ANY,
                    valueKind(pv.getType()), Origin.LOCAL);
            }
            return new PathInfo(Location.ANY, true, Shape.ROOT, Container.ANY,
                valueKind(pv.getType()), Origin.LOCAL);
        }
        if (pe instanceof MemberExp member) {
            PathInfo base = classify(member.getLeftExp(), services);
            if (base == null) {
                return null;
            }
            return new PathInfo(base.location(), false, Shape.FIELD, Container.ANY,
                valueKind(member.getType()), base.origin());
        }
        if (pe instanceof IndexExpression index) {
            PathInfo base = classify(index.getLeftExp(), services);
            if (base == null) {
                return null;
            }
            Type indexedType = index.getLeftExp().getType();
            return new PathInfo(base.location(), false, Shape.INDEX, container(indexedType),
                valueKind(indexedValueType(indexedType)), base.origin());
        }
        return null;
    }

    private static Container container(Type type) {
        Type unwrapped = unwrap(type);
        if (unwrapped instanceof ArrayType || unwrapped instanceof DynamicArrayType) {
            return Container.ARRAY;
        }
        if (unwrapped instanceof MappingType) {
            return Container.MAPPING;
        }
        return Container.ANY;
    }

    private static Type indexedValueType(Type type) {
        Type unwrapped = unwrap(type);
        if (unwrapped instanceof ArrayType arrayType) {
            return arrayType.getElementType();
        }
        if (unwrapped instanceof DynamicArrayType arrayType) {
            return arrayType.getElementType();
        }
        if (unwrapped instanceof MappingType mappingType) {
            return mappingType.valueType();
        }
        return null;
    }

    private static ValueKind valueKind(Type type) {
        Type unwrapped = unwrap(type);
        if (unwrapped instanceof PrimitiveType || unwrapped instanceof EnumDeclaration) {
            return ValueKind.PRIMITIVE;
        }
        if (unwrapped instanceof StructDeclaration || unwrapped instanceof ContractDeclaration
                || unwrapped instanceof ArrayType || unwrapped instanceof DynamicArrayType
                || unwrapped instanceof MappingType) {
            return ValueKind.REFERENCE;
        }
        return ValueKind.ANY;
    }

    private static Type unwrap(Type type) {
        if (type instanceof KeYSolidityType keySolidityType) {
            return keySolidityType.getSolidityType();
        }
        return type;
    }

    private static final class PathFilters {
        private Location location = Location.ANY;
        private Simplicity simplicity = Simplicity.ANY;
        private Shape shape = Shape.ANY;
        private Container container = Container.ANY;
        private ValueKind valueKind = ValueKind.ANY;
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
