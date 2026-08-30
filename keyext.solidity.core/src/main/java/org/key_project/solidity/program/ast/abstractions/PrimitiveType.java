/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;

import org.jspecify.annotations.Nullable;

public class PrimitiveType implements Type, SyntaxElement {

    private static final HashMap<String, PrimitiveType> primitives = new HashMap<>();

    /// Classifies a primitive type by the logic sort family it belongs to.
    public enum Kind {
        INTEGER, BOOLEAN, STRING, ADDRESS, BYTES, FIXED, VOID
    }

    private static PrimitiveType newPrimitiveType(String name,
            Kind kind) {
        synchronized (primitives) {
            assert !primitives.containsKey(name);
            PrimitiveType pt = new PrimitiveType(new Name(name), kind);
            primitives.put(name, pt);
            return pt;
        }
    }

    public static PrimitiveType getPrimitiveType(String name)
            throws NoSuchElementException {
        PrimitiveType primitive;
        synchronized (primitives) {
            primitive = primitives.get(name);
            if (primitive == null)
                throw new NoSuchElementException("No primitive type with name " + name);
        }
        return primitive;
    }

    /// @return all registered primitive types (snapshot)
    public static java.util.List<PrimitiveType> all() {
        synchronized (primitives) {
            return java.util.List.copyOf(primitives.values());
        }
    }

    private final Name name;
    private final Kind kind;

    private PrimitiveType(Name name, Kind kind) {
        this.name = name;
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }


    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        PrimitiveType type = (PrimitiveType) o;
        return Objects.equals(name, type.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public Name name() {
        return name;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    public static final PrimitiveType INT = newPrimitiveType("int", Kind.INTEGER);
    public static final PrimitiveType INT8 = newPrimitiveType("int8", Kind.INTEGER);
    public static final PrimitiveType INT16 = newPrimitiveType("int16", Kind.INTEGER);
    public static final PrimitiveType INT24 = newPrimitiveType("int24", Kind.INTEGER);
    public static final PrimitiveType INT32 = newPrimitiveType("int32", Kind.INTEGER);
    public static final PrimitiveType INT40 = newPrimitiveType("int40", Kind.INTEGER);
    public static final PrimitiveType INT48 = newPrimitiveType("int48", Kind.INTEGER);
    public static final PrimitiveType INT56 = newPrimitiveType("int56", Kind.INTEGER);
    public static final PrimitiveType INT64 = newPrimitiveType("int64", Kind.INTEGER);
    public static final PrimitiveType INT72 = newPrimitiveType("int72", Kind.INTEGER);
    public static final PrimitiveType INT80 = newPrimitiveType("int80", Kind.INTEGER);
    public static final PrimitiveType INT88 = newPrimitiveType("int88", Kind.INTEGER);
    public static final PrimitiveType INT96 = newPrimitiveType("int96", Kind.INTEGER);
    public static final PrimitiveType INT104 = newPrimitiveType("int104", Kind.INTEGER);
    public static final PrimitiveType INT112 = newPrimitiveType("int112", Kind.INTEGER);
    public static final PrimitiveType INT120 = newPrimitiveType("int120", Kind.INTEGER);
    public static final PrimitiveType INT128 = newPrimitiveType("int128", Kind.INTEGER);
    public static final PrimitiveType INT136 = newPrimitiveType("int136", Kind.INTEGER);
    public static final PrimitiveType INT144 = newPrimitiveType("int144", Kind.INTEGER);
    public static final PrimitiveType INT152 = newPrimitiveType("int152", Kind.INTEGER);
    public static final PrimitiveType INT160 = newPrimitiveType("int160", Kind.INTEGER);
    public static final PrimitiveType INT168 = newPrimitiveType("int168", Kind.INTEGER);
    public static final PrimitiveType INT176 = newPrimitiveType("int176", Kind.INTEGER);
    public static final PrimitiveType INT184 = newPrimitiveType("int184", Kind.INTEGER);
    public static final PrimitiveType INT192 = newPrimitiveType("int192", Kind.INTEGER);
    public static final PrimitiveType INT200 = newPrimitiveType("int200", Kind.INTEGER);
    public static final PrimitiveType INT208 = newPrimitiveType("int208", Kind.INTEGER);
    public static final PrimitiveType INT216 = newPrimitiveType("int216", Kind.INTEGER);
    public static final PrimitiveType INT224 = newPrimitiveType("int224", Kind.INTEGER);
    public static final PrimitiveType INT232 = newPrimitiveType("int232", Kind.INTEGER);
    public static final PrimitiveType INT240 = newPrimitiveType("int240", Kind.INTEGER);
    public static final PrimitiveType INT248 = newPrimitiveType("int248", Kind.INTEGER);
    public static final PrimitiveType INT256 = newPrimitiveType("int256", Kind.INTEGER);

    public static final PrimitiveType UINT = newPrimitiveType("uint", Kind.INTEGER);
    public static final PrimitiveType UINT8 = newPrimitiveType("uint8", Kind.INTEGER);
    public static final PrimitiveType UINT16 = newPrimitiveType("uint16", Kind.INTEGER);
    public static final PrimitiveType UINT24 = newPrimitiveType("uint24", Kind.INTEGER);
    public static final PrimitiveType UINT32 = newPrimitiveType("uint32", Kind.INTEGER);
    public static final PrimitiveType UINT40 = newPrimitiveType("uint40", Kind.INTEGER);
    public static final PrimitiveType UINT48 = newPrimitiveType("uint48", Kind.INTEGER);
    public static final PrimitiveType UINT56 = newPrimitiveType("uint56", Kind.INTEGER);
    public static final PrimitiveType UINT64 = newPrimitiveType("uint64", Kind.INTEGER);
    public static final PrimitiveType UINT72 = newPrimitiveType("uint72", Kind.INTEGER);
    public static final PrimitiveType UINT80 = newPrimitiveType("uint80", Kind.INTEGER);
    public static final PrimitiveType UINT88 = newPrimitiveType("uint88", Kind.INTEGER);
    public static final PrimitiveType UINT96 = newPrimitiveType("uint96", Kind.INTEGER);
    public static final PrimitiveType UINT104 = newPrimitiveType("uint104", Kind.INTEGER);
    public static final PrimitiveType UINT112 = newPrimitiveType("uint112", Kind.INTEGER);
    public static final PrimitiveType UINT120 = newPrimitiveType("uint120", Kind.INTEGER);
    public static final PrimitiveType UINT128 = newPrimitiveType("uint128", Kind.INTEGER);
    public static final PrimitiveType UINT136 = newPrimitiveType("uint136", Kind.INTEGER);
    public static final PrimitiveType UINT144 = newPrimitiveType("uint144", Kind.INTEGER);
    public static final PrimitiveType UINT152 = newPrimitiveType("uint152", Kind.INTEGER);
    public static final PrimitiveType UINT160 = newPrimitiveType("uint160", Kind.INTEGER);
    public static final PrimitiveType UINT168 = newPrimitiveType("uint168", Kind.INTEGER);
    public static final PrimitiveType UINT176 = newPrimitiveType("uint176", Kind.INTEGER);
    public static final PrimitiveType UINT184 = newPrimitiveType("uint184", Kind.INTEGER);
    public static final PrimitiveType UINT192 = newPrimitiveType("uint192", Kind.INTEGER);
    public static final PrimitiveType UINT200 = newPrimitiveType("uint200", Kind.INTEGER);
    public static final PrimitiveType UINT208 = newPrimitiveType("uint208", Kind.INTEGER);
    public static final PrimitiveType UINT216 = newPrimitiveType("uint216", Kind.INTEGER);
    public static final PrimitiveType UINT224 = newPrimitiveType("uint224", Kind.INTEGER);
    public static final PrimitiveType UINT232 = newPrimitiveType("uint232", Kind.INTEGER);
    public static final PrimitiveType UINT240 = newPrimitiveType("uint240", Kind.INTEGER);
    public static final PrimitiveType UINT248 = newPrimitiveType("uint248", Kind.INTEGER);
    public static final PrimitiveType UINT256 = newPrimitiveType("uint256", Kind.INTEGER);

    public static final PrimitiveType BYTES1 = newPrimitiveType("bytes1", Kind.BYTES);
    public static final PrimitiveType BYTES2 = newPrimitiveType("bytes2", Kind.BYTES);
    public static final PrimitiveType BYTES3 = newPrimitiveType("bytes3", Kind.BYTES);
    public static final PrimitiveType BYTES4 = newPrimitiveType("bytes4", Kind.BYTES);
    public static final PrimitiveType BYTES5 = newPrimitiveType("bytes5", Kind.BYTES);
    public static final PrimitiveType BYTES6 = newPrimitiveType("bytes6", Kind.BYTES);
    public static final PrimitiveType BYTES7 = newPrimitiveType("bytes7", Kind.BYTES);
    public static final PrimitiveType BYTES8 = newPrimitiveType("bytes8", Kind.BYTES);
    public static final PrimitiveType BYTES9 = newPrimitiveType("bytes9", Kind.BYTES);
    public static final PrimitiveType BYTES10 = newPrimitiveType("bytes10", Kind.BYTES);
    public static final PrimitiveType BYTES11 = newPrimitiveType("bytes11", Kind.BYTES);
    public static final PrimitiveType BYTES12 = newPrimitiveType("bytes12", Kind.BYTES);
    public static final PrimitiveType BYTES13 = newPrimitiveType("bytes13", Kind.BYTES);
    public static final PrimitiveType BYTES14 = newPrimitiveType("bytes14", Kind.BYTES);
    public static final PrimitiveType BYTES15 = newPrimitiveType("bytes15", Kind.BYTES);
    public static final PrimitiveType BYTES16 = newPrimitiveType("bytes16", Kind.BYTES);
    public static final PrimitiveType BYTES17 = newPrimitiveType("bytes17", Kind.BYTES);
    public static final PrimitiveType BYTES18 = newPrimitiveType("bytes18", Kind.BYTES);
    public static final PrimitiveType BYTES19 = newPrimitiveType("bytes19", Kind.BYTES);
    public static final PrimitiveType BYTES20 = newPrimitiveType("bytes20", Kind.BYTES);
    public static final PrimitiveType BYTES21 = newPrimitiveType("bytes21", Kind.BYTES);
    public static final PrimitiveType BYTES22 = newPrimitiveType("bytes22", Kind.BYTES);
    public static final PrimitiveType BYTES23 = newPrimitiveType("bytes23", Kind.BYTES);
    public static final PrimitiveType BYTES24 = newPrimitiveType("bytes24", Kind.BYTES);
    public static final PrimitiveType BYTES25 = newPrimitiveType("bytes25", Kind.BYTES);
    public static final PrimitiveType BYTES26 = newPrimitiveType("bytes26", Kind.BYTES);
    public static final PrimitiveType BYTES27 = newPrimitiveType("bytes27", Kind.BYTES);
    public static final PrimitiveType BYTES28 = newPrimitiveType("bytes28", Kind.BYTES);
    public static final PrimitiveType BYTES29 = newPrimitiveType("bytes29", Kind.BYTES);
    public static final PrimitiveType BYTES30 = newPrimitiveType("bytes30", Kind.BYTES);
    public static final PrimitiveType BYTES31 = newPrimitiveType("bytes31", Kind.BYTES);
    public static final PrimitiveType BYTES32 = newPrimitiveType("bytes32", Kind.BYTES);

    public static final PrimitiveType BOOL = newPrimitiveType("bool", Kind.BOOLEAN);
    public static final PrimitiveType STRING = newPrimitiveType("string", Kind.STRING);
    public static final PrimitiveType BYTES = newPrimitiveType("bytes", Kind.BYTES);
    public static final PrimitiveType FIXED = newPrimitiveType("fixed", Kind.FIXED);
    public static final PrimitiveType UFIXED = newPrimitiveType("ufixed", Kind.FIXED);
    public static final PrimitiveType ADDRESS = newPrimitiveType("address", Kind.ADDRESS);
    public static final PrimitiveType VOID = newPrimitiveType("void", Kind.VOID);

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException(
            "Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
