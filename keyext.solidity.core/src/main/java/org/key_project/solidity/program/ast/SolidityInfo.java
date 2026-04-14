/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.TupleType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.expressions.Expression;

import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.*;

/**
 * This class is responsible to answer queries about the solidity program model
 * Such queries are for instance: all declared and known contracts, all functions for
 * a contract, finding a contract by name or a function declaration by its signature,
 * providing the type and KeYSolidityType by name etc.
 */
public class SolidityInfo {
    private final Map<Name, Type> typeMap = new HashMap<>();

    public Type getType(Name typeName) {
        if (typeMap.containsKey(typeName))
            return typeMap.get(typeName);
        return getPrimitiveType(typeName.toString());
    }

    public Type getDynamicTypeMap(Name primaryTypeName) {
        Name typeName = new Name(primaryTypeName + "[]");
        if (typeMap.containsKey(typeName ))
            return typeMap.get(typeName);
        Type primaryType = getType(primaryTypeName);
        Type type = new DynamicArrayType(primaryType);
        typeMap.put(typeName, type);
        return type;
    }

    public Type getStaticTypeMap(Name primaryTypeName, Expression expression) {
        return getStaticTypeMap(primaryTypeName, Integer.parseInt(expression.toString()));
    }

    public Type getStaticTypeMap(Name primaryTypeName, int size) {
        Name typeName = new Name(primaryTypeName + "[" + size + "]");
        if (typeMap.containsKey(typeName))
            return typeMap.get(typeName);
        Type primaryType = getType(primaryTypeName);
        Type type = new ArrayType(primaryType, size);
        typeMap.put(typeName, type);
        return type;
    }

    public MappingType getMappingTypeMap(Type keyType, Type valueType) {
        MappingType mapping = new MappingType(keyType, valueType);
        if (typeMap.containsKey(mapping.name()))
            return (MappingType) typeMap.get(mapping.name());
        typeMap.put(mapping.name(), mapping);
        return mapping;
    }

    public TupleType getTupleTypeMap(List<Type> types) {
        Name typeName = new Name("(" + types.stream().map(Object::toString)
                .collect(Collectors.joining(", ")) + ")");
        if (typeMap.containsKey(typeName))
            return (TupleType) typeMap.get(typeName);
        TupleType type = new TupleType(types);
        typeMap.put(typeName, type);
        return type;
    }

    public KeYSolidityType getKeYSolidityType(String type) {
        return (KeYSolidityType) getType(new Name(type));
    }

    public void addType(Sort sort, Type type) {
        Name sortName = sort.name();
        if(!typeMap.containsKey(sortName))
            typeMap.put(sortName, type);
    }

    public Set<Function> getAllSolidityFunctions() {
        // not Set<Function> but Set<ProgramFunction> should be returned
        return null;
    }

    public static Type getPrimitiveType(String typeS) {
        return switch (typeS) {
            case "int" -> INT;
            case "int8" -> INT8;
            case "int16" -> INT16;
            case "int24" -> INT24;
            case "int32" -> INT32;
            case "int40" -> INT40;
            case "int48" -> INT48;
            case "int56" -> INT56;
            case "int64" -> INT64;
            case "int72" -> INT72;
            case "int80" -> INT80;
            case "int88" -> INT88;
            case "int96" -> INT96;
            case "int104" -> INT104;
            case "int112" -> INT112;
            case "int120" -> INT120;
            case "int128" -> INT128;
            case "int136" -> INT136;
            case "int144" -> INT144;
            case "int152" -> INT152;
            case "int160" -> INT160;
            case "int168" -> INT168;
            case "int176" -> INT176;
            case "int184" -> INT184;
            case "int192" -> INT192;
            case "int200" -> INT200;
            case "int208" -> INT208;
            case "int216" -> INT216;
            case "int224" -> INT224;
            case "int232" -> INT232;
            case "int240" -> INT240;
            case "int248" -> INT248;
            case "int256" -> INT256;

            case "uint" -> UINT;
            case "uint8" -> UINT8;
            case "uint16" -> UINT16;
            case "uint24" -> UINT24;
            case "uint32" -> UINT32;
            case "uint40" -> UINT40;
            case "uint48" -> UINT48;
            case "uint56" -> UINT56;
            case "uint64" -> UINT64;
            case "uint72" -> UINT72;
            case "uint80" -> UINT80;
            case "uint88" -> UINT88;
            case "uint96" -> UINT96;
            case "uint104" -> UINT104;
            case "uint112" -> UINT112;
            case "uint120" -> UINT120;
            case "uint128" -> UINT128;
            case "uint136" -> UINT136;
            case "uint144" -> UINT144;
            case "uint152" -> UINT152;
            case "uint160" -> UINT160;
            case "uint168" -> UINT168;
            case "uint176" -> UINT176;
            case "uint184" -> UINT184;
            case "uint192" -> UINT192;
            case "uint200" -> UINT200;
            case "uint208" -> UINT208;
            case "uint216" -> UINT216;
            case "uint224" -> UINT224;
            case "uint232" -> UINT232;
            case "uint240" -> UINT240;
            case "uint248" -> UINT248;
            case "uint256" -> UINT256;

            case "bytes" -> BYTES;
            case "bytes1" -> BYTES1;
            case "bytes2" -> BYTES2;
            case "bytes3" -> BYTES3;
            case "bytes4" -> BYTES4;
            case "bytes5" -> BYTES5;
            case "bytes6" -> BYTES6;
            case "bytes7" -> BYTES7;
            case "bytes8" -> BYTES8;
            case "bytes9" -> BYTES9;
            case "bytes10" -> BYTES10;
            case "bytes11" -> BYTES11;
            case "bytes12" -> BYTES12;
            case "bytes13" -> BYTES13;
            case "bytes14" -> BYTES14;
            case "bytes15" -> BYTES15;
            case "bytes16" -> BYTES16;
            case "bytes17" -> BYTES17;
            case "bytes18" -> BYTES18;
            case "bytes19" -> BYTES19;
            case "bytes20" -> BYTES20;
            case "bytes21" -> BYTES21;
            case "bytes22" -> BYTES22;
            case "bytes23" -> BYTES23;
            case "bytes24" -> BYTES24;
            case "bytes25" -> BYTES25;
            case "bytes26" -> BYTES26;
            case "bytes27" -> BYTES27;
            case "bytes28" -> BYTES28;
            case "bytes29" -> BYTES29;
            case "bytes30" -> BYTES30;
            case "bytes31" -> BYTES31;
            case "bytes32" -> BYTES32;

            case "rational" -> INT256;
            case "struct" -> STRUCT;
            case "bool" -> BOOL;
            case "address" -> ADDRESS;
            case "string" -> STRING;
            case "fixed" -> FIXED;
            case "ufixed" -> UFIXED;
            case "tuple" -> TUPLE;
            case "function" -> FUNCTION;
            case "contract" -> CONTRACT;

            default -> null;
        };
    }

}
