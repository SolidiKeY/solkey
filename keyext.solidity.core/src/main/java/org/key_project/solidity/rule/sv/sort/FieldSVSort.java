/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.sv.sort;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.StorageReferenceTypes;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;

/// Program schema variable sort matching the *field-name* child of a `MemberExp`,
/// i.e. the right-hand identifier in `receiver.a`. Used in taclets of shape
/// `\find(... { s#sp.s#a = s#se; } ...)` so the receiver and field can be
/// captured as separate schema variables and re-emitted in `\replacewith`.
///
/// `Field[primitive]` / `Field[reference]` additionally restrict the field's declared type
/// (mirroring [PathSVSort]'s `primitive`/`reference` flags): primitive-typed fields hold plain
/// values, while struct/array/mapping fields are reference-typed locations.
public class FieldSVSort extends ProgramSVSort {

    private enum TypeKind {
        ANY, PRIMITIVE, REFERENCE
    }

    private static final Map<String, ProgramSVSort> PARAMETERIZED_SORTS = new HashMap<>();

    private final TypeKind typeKind;

    public FieldSVSort() {
        this(new Name("Field"), TypeKind.ANY);
    }

    private FieldSVSort(Name name, TypeKind typeKind) {
        super(name);
        this.typeKind = typeKind;
    }

    @Override
    public boolean canStandFor(SolidityProgramElement pe, Services services) {
        if (!(pe instanceof FieldDeclaration fd)) {
            return false;
        }
        if (typeKind == TypeKind.ANY) {
            return true;
        }
        Type fieldType = fd.getTypeReference().resolvedType();
        if (fieldType instanceof KeYSolidityType keyType && keyType.getSolidityType() != null) {
            fieldType = keyType.getSolidityType();
        }
        if (fieldType == null) {
            return false;
        }
        return switch (typeKind) {
            case PRIMITIVE -> fieldType instanceof PrimitiveType;
            case REFERENCE -> StorageReferenceTypes.isReferenceType(fieldType);
            case ANY -> true;
        };
    }

    @Override
    public ProgramSVSort createInstance(String parameter) {
        ProgramSVSort cached = PARAMETERIZED_SORTS.get(parameter);
        if (cached != null) {
            return cached;
        }
        TypeKind kind = switch (parameter.toLowerCase(Locale.ROOT)) {
            case "primitive" -> TypeKind.PRIMITIVE;
            case "reference" -> TypeKind.REFERENCE;
            default -> throw new IllegalArgumentException(
                "Unknown Field sort flag '" + parameter
                    + "' (expected 'primitive' or 'reference')");
        };
        ProgramSVSort result =
            new FieldSVSort(new Name("Field[" + parameter + "]"), kind);
        PARAMETERIZED_SORTS.put(parameter, result);
        return result;
    }
}
