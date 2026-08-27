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
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.StorageReferenceTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.expressions.literals.Literal;
import org.key_project.solidity.program.ast.references.FieldReference;

final class PathSVSort extends ProgramSVSort {
    private static final Map<String, ProgramSVSort> PARAMETERIZED_SORTS = new HashMap<>();

    enum DataArea {
        ANY, STORAGE, MEMORY
    }

    enum Simplicity {
        ANY, SIMPLE, COMPLEX
    }

    private enum Origin {
        ANY, LOCAL, GLOBAL
    }

    private enum TypeCategory {
        ANY, ARRAY, MAPPING
    }

    private enum TypeKind {
        ANY, PRIMITIVE, REFERENCE
    }

    private enum ElementKind {
        ANY, PRIMITIVE, REFERENCE
    }

    private record PathInfo(DataArea dataArea, boolean simple, Origin origin,
            TypeCategory typeCategory) {
    }

    private final DataArea dataArea;
    private final Simplicity simplicity;
    private final Origin origin;
    private final TypeCategory typeCategory;
    private final TypeKind typeKind;
    private final ElementKind elementKind;

    PathSVSort(String name, DataArea dataArea, Simplicity simplicity) {
        this(name, dataArea, simplicity, Origin.ANY, TypeCategory.ANY, TypeKind.ANY,
            ElementKind.ANY);
    }

    private PathSVSort(String name, DataArea dataArea, Simplicity simplicity, Origin origin,
            TypeCategory typeCategory, TypeKind typeKind, ElementKind elementKind) {
        super(new Name(name));
        this.dataArea = dataArea;
        this.simplicity = simplicity;
        this.origin = origin;
        this.typeCategory = typeCategory;
        this.typeKind = typeKind;
        this.elementKind = elementKind;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        PathInfo info = classify(pe, services);
        if (info == null) {
            return false;
        }
        if (dataArea != DataArea.ANY && info.dataArea() != dataArea) {
            return false;
        }
        if (origin != Origin.ANY && info.origin() != origin) {
            return false;
        }
        if (typeCategory != TypeCategory.ANY && info.typeCategory() != typeCategory) {
            return false;
        }
        if (typeKind != TypeKind.ANY && typeKindOf(pe) != typeKind) {
            return false;
        }
        if (elementKind != ElementKind.ANY && elementKindOf(pe) != elementKind) {
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
                case "storage" -> filters.dataArea.set(DataArea.STORAGE, flag);
                case "memory" -> filters.dataArea.set(DataArea.MEMORY, flag);
                case "simple" -> filters.simplicity.set(Simplicity.SIMPLE, flag);
                case "complex", "nonsimple", "non-simple" -> filters.simplicity
                        .set(Simplicity.COMPLEX, flag);
                case "local" -> filters.origin.set(Origin.LOCAL, flag);
                case "global" -> filters.origin.set(Origin.GLOBAL, flag);
                case "array" -> filters.typeCategory.set(TypeCategory.ARRAY, flag);
                case "mapping" -> filters.typeCategory.set(TypeCategory.MAPPING, flag);
                case "primitive" -> filters.typeKind.set(TypeKind.PRIMITIVE, flag);
                case "reference" -> filters.typeKind.set(TypeKind.REFERENCE, flag);
                case "primitiveelement" -> filters.elementKind.set(ElementKind.PRIMITIVE, flag);
                case "referenceelement" -> filters.elementKind.set(ElementKind.REFERENCE, flag);
                default -> throw new IllegalArgumentException(
                    "Unknown Path sort flag '" + rawFlag + "'");
            }
        }
        if (filters.dataArea.value == DataArea.MEMORY && filters.origin.value == Origin.GLOBAL) {
            throw new IllegalArgumentException(
                "Memory paths are always local; use 'memory' or 'memory,local'");
        }
        ProgramSVSort result = new PathSVSort("Path[" + parameter + "]", filters.dataArea.value,
            filters.simplicity.value, filters.origin.value, filters.typeCategory.value,
            filters.typeKind.value, filters.elementKind.value);
        PARAMETERIZED_SORTS.put(parameter, result);
        return result;
    }

    private static PathInfo classify(SolidityProgramElement pe, Services services) {
        if (pe instanceof FieldReference) {
            return new PathInfo(DataArea.STORAGE, true, Origin.GLOBAL, typeCategoryOf(pe));
        }
        if (pe instanceof ProgramVariable pv) {
            DataLocation dataLocation = pv.getDataLocation();
            if (dataLocation == DataLocation.Storage) {
                return new PathInfo(DataArea.STORAGE, true, Origin.LOCAL, typeCategoryOf(pe));
            }
            if (dataLocation == DataLocation.Memory) {
                return new PathInfo(DataArea.MEMORY, true, Origin.LOCAL, typeCategoryOf(pe));
            }
            return new PathInfo(DataArea.ANY, true, Origin.LOCAL, typeCategoryOf(pe));
        }
        if (pe instanceof MemberExp member) {
            PathInfo base = classify(member.getLeftExp(), services);
            if (base == null) {
                return null;
            }
            return new PathInfo(base.dataArea(), false, base.origin(), typeCategoryOf(pe));
        }
        if (pe instanceof IndexExpression index) {
            if (!isSimpleIndex(index.getIndexExp())) {
                return null;
            }
            PathInfo base = classify(index.getLeftExp(), services);
            if (base == null) {
                return null;
            }
            return new PathInfo(base.dataArea(), false, base.origin(), typeCategoryOf(pe));
        }
        // A no-arg `arr.push()` returns the freshly appended slot: a complex storage
        // location rooted at the array receiver, with the array's element type. Treating
        // it as a complex path lets the ordinary complex-receiver unfold rules capture it.
        if (pe instanceof FunctionCallExpression call && isNoArgPush(call)) {
            PathInfo base = classify(((MemberExp) call.getFunctionExp()).getLeftExp(), services);
            if (base == null) {
                return null;
            }
            return new PathInfo(base.dataArea(), false, base.origin(), typeCategoryOf(pe));
        }
        return null;
    }

    static boolean isNoArgPush(FunctionCallExpression call) {
        return call.getArguments().isEmpty()
                && call.getFunctionExp() instanceof MemberExp m
                && m.getRightExp() instanceof FunctionDeclaration fd
                && "push".equals(fd.name().toString());
    }

    private static boolean isSimpleIndex(SolidityProgramElement index) {
        return index instanceof ProgramVariable || index instanceof Literal;
    }

    private static TypeKind typeKindOf(SolidityProgramElement pe) {
        Type type = typeOf(pe);
        if (type instanceof PrimitiveType) {
            return TypeKind.PRIMITIVE;
        }
        if (type instanceof StructDeclaration || type instanceof ArrayType
                || type instanceof DynamicArrayType || type instanceof MappingType) {
            return TypeKind.REFERENCE;
        }
        return TypeKind.ANY;
    }

    private static ElementKind elementKindOf(SolidityProgramElement pe) {
        Type type = typeOf(pe);
        Type elementType = null;
        if (type instanceof MappingType mappingType) {
            elementType = unwrap(mappingType.valueType());
        } else if (type instanceof ArrayType arrayType) {
            elementType = unwrap(arrayType.getElementType());
        } else if (type instanceof DynamicArrayType dynamicArrayType) {
            elementType = unwrap(dynamicArrayType.getElementType());
        }
        if (elementType instanceof PrimitiveType) {
            return ElementKind.PRIMITIVE;
        }
        if (elementType != null && StorageReferenceTypes.isReferenceType(elementType)) {
            return ElementKind.REFERENCE;
        }
        return ElementKind.ANY;
    }

    private static TypeCategory typeCategoryOf(SolidityProgramElement pe) {
        Type type = typeOf(pe);
        if (type instanceof DynamicArrayType || type instanceof ArrayType) {
            return TypeCategory.ARRAY;
        }
        if (type instanceof MappingType) {
            return TypeCategory.MAPPING;
        }
        return TypeCategory.ANY;
    }

    private static Type typeOf(SolidityProgramElement pe) {
        if (!(pe instanceof Expression expression)) {
            return null;
        }
        if (pe instanceof IndexExpression index) {
            Type baseType = typeOf(index.getLeftExp());
            if (baseType instanceof DynamicArrayType arrayType) {
                return unwrap(arrayType.getElementType());
            }
            if (baseType instanceof ArrayType arrayType) {
                return unwrap(arrayType.getElementType());
            }
            if (baseType instanceof MappingType mappingType) {
                return unwrap(mappingType.valueType());
            }
        }
        return unwrap(expression.getType());
    }

    private static Type unwrap(Type type) {
        if (type instanceof KeYSolidityType keyType && keyType.getSolidityType() != null) {
            return keyType.getSolidityType();
        }
        return type;
    }

    private static final class PathFilters {
        private final Filter<DataArea> dataArea = new Filter<>(DataArea.ANY);
        private final Filter<Simplicity> simplicity = new Filter<>(Simplicity.ANY);
        private final Filter<Origin> origin = new Filter<>(Origin.ANY);
        private final Filter<TypeCategory> typeCategory = new Filter<>(TypeCategory.ANY);
        private final Filter<TypeKind> typeKind = new Filter<>(TypeKind.ANY);
        private final Filter<ElementKind> elementKind = new Filter<>(ElementKind.ANY);
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
