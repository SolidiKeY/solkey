/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.SortImpl;

import org.jspecify.annotations.NonNull;

public class PrimitiveType implements Type, SyntaxElement {

    private static final HashMap<String, PrimitiveType> primitives = new HashMap<>();

    private static @NonNull PrimitiveType newPrimitiveType(String name) {
        synchronized (primitives) {
            assert !primitives.containsKey(name.toString());
            PrimitiveType pt = new PrimitiveType(new Name(name));
            primitives.put(name.toString(), pt);
            return pt;
        }
    }

    public static PrimitiveType getPrimitiveType(String name) throws NoSuchElementException {
        PrimitiveType primitive;
        synchronized (primitives) {
            primitive = primitives.get(name);
            if (primitive == null) {
                throw new NoSuchElementException("No primitive type with name " + name);
            }
        }
        return primitive;
    }

    private final @NonNull Name name;

    private PrimitiveType(@NonNull Name name) {
        this.name = name;
    }


    @Override
    public boolean equals(Object o) {
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
    public @NonNull Name name() {
        return name;
    }

    @Override
    public @NonNull Sort getSort(Services services) {
        Namespace<@NonNull Sort> sorts = services.getNamespaces().sorts();
        Sort sort = sorts.lookup(name);
        if (sort == null) {
            sort = new SortImpl(name, false);
            sorts.add(sort);
        }
        return sort;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    public static final @NonNull PrimitiveType INT = newPrimitiveType("int");
    public static final @NonNull PrimitiveType INT8 = newPrimitiveType("int8");
    public static final @NonNull PrimitiveType INT16 = newPrimitiveType("int16");
    public static final @NonNull PrimitiveType INT24 = newPrimitiveType("int24");
    public static final @NonNull PrimitiveType INT32 = newPrimitiveType("int32");
    public static final @NonNull PrimitiveType INT40 = newPrimitiveType("int40");
    public static final @NonNull PrimitiveType INT48 = newPrimitiveType("int48");
    public static final @NonNull PrimitiveType INT56 = newPrimitiveType("int56");
    public static final @NonNull PrimitiveType INT64 = newPrimitiveType("int64");
    public static final @NonNull PrimitiveType INT72 = newPrimitiveType("int72");
    public static final @NonNull PrimitiveType INT80 = newPrimitiveType("int80");
    public static final @NonNull PrimitiveType INT88 = newPrimitiveType("int88");
    public static final @NonNull PrimitiveType INT96 = newPrimitiveType("int96");
    public static final @NonNull PrimitiveType INT104 = newPrimitiveType("int104");
    public static final @NonNull PrimitiveType INT112 = newPrimitiveType("int112");
    public static final @NonNull PrimitiveType INT120 = newPrimitiveType("int120");
    public static final @NonNull PrimitiveType INT128 = newPrimitiveType("int128");
    public static final @NonNull PrimitiveType INT136 = newPrimitiveType("int136");
    public static final @NonNull PrimitiveType INT144 = newPrimitiveType("int144");
    public static final @NonNull PrimitiveType INT152 = newPrimitiveType("int152");
    public static final @NonNull PrimitiveType INT160 = newPrimitiveType("int160");
    public static final @NonNull PrimitiveType INT168 = newPrimitiveType("int168");
    public static final @NonNull PrimitiveType INT176 = newPrimitiveType("int176");
    public static final @NonNull PrimitiveType INT184 = newPrimitiveType("int184");
    public static final @NonNull PrimitiveType INT192 = newPrimitiveType("int192");
    public static final @NonNull PrimitiveType INT200 = newPrimitiveType("int200");
    public static final @NonNull PrimitiveType INT208 = newPrimitiveType("int208");
    public static final @NonNull PrimitiveType INT216 = newPrimitiveType("int216");
    public static final @NonNull PrimitiveType INT224 = newPrimitiveType("int224");
    public static final @NonNull PrimitiveType INT232 = newPrimitiveType("int232");
    public static final @NonNull PrimitiveType INT240 = newPrimitiveType("int240");
    public static final @NonNull PrimitiveType INT248 = newPrimitiveType("int248");
    public static final @NonNull PrimitiveType INT256 = newPrimitiveType("int256");

    public static final @NonNull PrimitiveType UINT = newPrimitiveType("uint");
    public static final @NonNull PrimitiveType UINT8 = newPrimitiveType("uint8");
    public static final @NonNull PrimitiveType UINT16 = newPrimitiveType("uint16");
    public static final @NonNull PrimitiveType UINT24 = newPrimitiveType("uint24");
    public static final @NonNull PrimitiveType UINT32 = newPrimitiveType("uint32");
    public static final @NonNull PrimitiveType UINT40 = newPrimitiveType("uint40");
    public static final @NonNull PrimitiveType UINT48 = newPrimitiveType("uint48");
    public static final @NonNull PrimitiveType UINT56 = newPrimitiveType("uint56");
    public static final @NonNull PrimitiveType UINT64 = newPrimitiveType("uint64");
    public static final @NonNull PrimitiveType UINT72 = newPrimitiveType("uint72");
    public static final @NonNull PrimitiveType UINT80 = newPrimitiveType("uint80");
    public static final @NonNull PrimitiveType UINT88 = newPrimitiveType("uint88");
    public static final @NonNull PrimitiveType UINT96 = newPrimitiveType("uint96");
    public static final @NonNull PrimitiveType UINT104 = newPrimitiveType("uint104");
    public static final @NonNull PrimitiveType UINT112 = newPrimitiveType("uint112");
    public static final @NonNull PrimitiveType UINT120 = newPrimitiveType("uint120");
    public static final @NonNull PrimitiveType UINT128 = newPrimitiveType("uint128");
    public static final @NonNull PrimitiveType UINT136 = newPrimitiveType("uint136");
    public static final @NonNull PrimitiveType UINT144 = newPrimitiveType("uint144");
    public static final @NonNull PrimitiveType UINT152 = newPrimitiveType("uint152");
    public static final @NonNull PrimitiveType UINT160 = newPrimitiveType("uint160");
    public static final @NonNull PrimitiveType UINT168 = newPrimitiveType("uint168");
    public static final @NonNull PrimitiveType UINT176 = newPrimitiveType("uint176");
    public static final @NonNull PrimitiveType UINT184 = newPrimitiveType("uint184");
    public static final @NonNull PrimitiveType UINT192 = newPrimitiveType("uint192");
    public static final @NonNull PrimitiveType UINT200 = newPrimitiveType("uint200");
    public static final @NonNull PrimitiveType UINT208 = newPrimitiveType("uint208");
    public static final @NonNull PrimitiveType UINT216 = newPrimitiveType("uint216");
    public static final @NonNull PrimitiveType UINT224 = newPrimitiveType("uint224");
    public static final @NonNull PrimitiveType UINT232 = newPrimitiveType("uint232");
    public static final @NonNull PrimitiveType UINT240 = newPrimitiveType("uint240");
    public static final @NonNull PrimitiveType UINT248 = newPrimitiveType("uint248");
    public static final @NonNull PrimitiveType UINT256 = newPrimitiveType("uint256");

    public static final @NonNull PrimitiveType BYTES1 = newPrimitiveType("bytes1");
    public static final @NonNull PrimitiveType BYTES2 = newPrimitiveType("bytes2");
    public static final @NonNull PrimitiveType BYTES3 = newPrimitiveType("bytes3");
    public static final @NonNull PrimitiveType BYTES4 = newPrimitiveType("bytes4");
    public static final @NonNull PrimitiveType BYTES5 = newPrimitiveType("bytes5");
    public static final @NonNull PrimitiveType BYTES6 = newPrimitiveType("bytes6");
    public static final @NonNull PrimitiveType BYTES7 = newPrimitiveType("bytes7");
    public static final @NonNull PrimitiveType BYTES8 = newPrimitiveType("bytes8");
    public static final @NonNull PrimitiveType BYTES9 = newPrimitiveType("bytes9");
    public static final @NonNull PrimitiveType BYTES10 = newPrimitiveType("bytes10");
    public static final @NonNull PrimitiveType BYTES11 = newPrimitiveType("bytes11");
    public static final @NonNull PrimitiveType BYTES12 = newPrimitiveType("bytes12");
    public static final @NonNull PrimitiveType BYTES13 = newPrimitiveType("bytes13");
    public static final @NonNull PrimitiveType BYTES14 = newPrimitiveType("bytes14");
    public static final @NonNull PrimitiveType BYTES15 = newPrimitiveType("bytes15");
    public static final @NonNull PrimitiveType BYTES16 = newPrimitiveType("bytes16");
    public static final @NonNull PrimitiveType BYTES17 = newPrimitiveType("bytes17");
    public static final @NonNull PrimitiveType BYTES18 = newPrimitiveType("bytes18");
    public static final @NonNull PrimitiveType BYTES19 = newPrimitiveType("bytes19");
    public static final @NonNull PrimitiveType BYTES20 = newPrimitiveType("bytes20");
    public static final @NonNull PrimitiveType BYTES21 = newPrimitiveType("bytes21");
    public static final @NonNull PrimitiveType BYTES22 = newPrimitiveType("bytes22");
    public static final @NonNull PrimitiveType BYTES23 = newPrimitiveType("bytes23");
    public static final @NonNull PrimitiveType BYTES24 = newPrimitiveType("bytes24");
    public static final @NonNull PrimitiveType BYTES25 = newPrimitiveType("bytes25");
    public static final @NonNull PrimitiveType BYTES26 = newPrimitiveType("bytes26");
    public static final @NonNull PrimitiveType BYTES27 = newPrimitiveType("bytes27");
    public static final @NonNull PrimitiveType BYTES28 = newPrimitiveType("bytes28");
    public static final @NonNull PrimitiveType BYTES29 = newPrimitiveType("bytes29");
    public static final @NonNull PrimitiveType BYTES30 = newPrimitiveType("bytes30");
    public static final @NonNull PrimitiveType BYTES31 = newPrimitiveType("bytes31");
    public static final @NonNull PrimitiveType BYTES32 = newPrimitiveType("bytes32");

    public static final @NonNull PrimitiveType ADDRESS = newPrimitiveType("address");
    public static final @NonNull PrimitiveType BOOL = newPrimitiveType("bool");
    public static final @NonNull PrimitiveType STRING = newPrimitiveType("string");
    public static final @NonNull PrimitiveType BYTES = newPrimitiveType("bytes");
    public static final @NonNull PrimitiveType FIXED = newPrimitiveType("fixed");
    public static final @NonNull PrimitiveType UFIXED = newPrimitiveType("ufixed");

    public static final @NonNull PrimitiveType STRUCT = newPrimitiveType("struct");
    public static final @NonNull PrimitiveType TUPLE = newPrimitiveType("tuple");
    public static final @NonNull PrimitiveType FUNCTION = newPrimitiveType("function");
    public static final @NonNull PrimitiveType CONTRACT = newPrimitiveType("contract");


    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int getChildCount() {
        return 0;
    }
}
