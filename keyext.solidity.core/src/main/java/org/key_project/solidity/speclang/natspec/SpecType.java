/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang.natspec;

/// The shape of a Solidity type as far as a specification needs it, derived from solc's
/// `typeDescriptions.typeString`.
public sealed interface SpecType {

    SpecType INT = new Int();
    SpecType BOOL = new Bool();

    record Int() implements SpecType {
    }

    record Bool() implements SpecType {
    }

    record Mapping(SpecType key, SpecType value) implements SpecType {
    }

    record Array(SpecType element) implements SpecType {
    }

    record Struct(String name) implements SpecType {
    }

    /// The `.key` sort a value of this type is read with, or `null` for a reference type.
    default String sort() {
        return switch (this) {
            case Int ignored -> "int";
            case Bool ignored -> "bool";
            default -> null;
        };
    }

    static SpecType of(String typeString) {
        String type = typeString.trim()
                .replaceAll(" (storage ref|storage pointer|memory|calldata)$", "");
        if (type.startsWith("mapping(") && type.endsWith(")")) {
            String inner = type.substring("mapping(".length(), type.length() - 1);
            int arrow = inner.indexOf(" => ");
            if (arrow < 0) {
                throw new SpecException("cannot read mapping type '" + typeString + "'");
            }
            return new Mapping(of(inner.substring(0, arrow)), of(inner.substring(arrow + 4)));
        }
        if (type.endsWith("]")) {
            return new Array(of(type.substring(0, type.lastIndexOf('['))));
        }
        if (type.startsWith("struct ")) {
            String name = type.substring("struct ".length());
            return new Struct(name.substring(name.lastIndexOf('.') + 1));
        }
        if (type.equals("bool")) {
            return BOOL;
        }
        if (type.matches("u?int\\d*") || type.matches("address( payable)?")
                || type.startsWith("enum ") || type.startsWith("contract ")) {
            return INT;
        }
        throw new SpecException("unsupported type '" + typeString + "' in a specification");
    }
}
