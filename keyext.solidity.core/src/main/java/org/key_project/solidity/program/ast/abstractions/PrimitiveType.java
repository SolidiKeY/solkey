/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.abstractions;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.key_project.logic.Name;

import org.jspecify.annotations.NonNull;

public class PrimitiveType implements Type {

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

    public static final @NonNull PrimitiveType UINT256 = newPrimitiveType("uint256");
    public static final @NonNull PrimitiveType BOOL = newPrimitiveType("bool");
    public static final @NonNull PrimitiveType ADDRESS = newPrimitiveType("address");
    // TODO add all primitive types

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
    public @NonNull Name getName() {
        return name;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}
