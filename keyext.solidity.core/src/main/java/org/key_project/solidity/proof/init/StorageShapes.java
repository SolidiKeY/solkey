/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.key_project.solidity.keyfile.Key;
import org.key_project.solidity.keyfile.KeyFormula;
import org.key_project.solidity.keyfile.KeyProblem;
import org.key_project.solidity.keyfile.KeyTerm;
import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.theory.StructLDT;

public final class StorageShapes {

    public static final String SHAPE_SUFFIX = "shape";

    private final SolidityOutline.Contract contract;
    private final Map<String, Shape> structShapes = new LinkedHashMap<>();
    private final Map<String, KeyTerm> roots = new LinkedHashMap<>();

    public StorageShapes(SolidityOutline.Contract contract) {
        this.contract = contract;
        for (String struct : contract.structs().keySet()) {
            if (!isTrivialStruct(struct, new HashSet<>())) {
                structShapes.put(struct, new Shape.StructOf(struct));
            }
        }
        for (SolidityOutline.Variable variable : contract.stateVariables()) {
            if (variable.constantValue() != null) {
                continue;
            }
            Shape shape = shapeOf(variable.type());
            if (!shape.isTrivial()) {
                roots.put(field(variable.name()), shape.render(contract.name()));
            }
        }
    }

    public boolean isEmpty() {
        return roots.isEmpty();
    }

    /// The constructor's starting storage: the shaped empty node of every non-trivial root
    /// stored into `mtSt`.
    public KeyTerm emptyStorage() {
        KeyTerm storage = Key.constant("mtSt");
        for (Map.Entry<String, KeyTerm> root : roots.entrySet()) {
            storage = Key.apply("storeSt", storage, Key.constant(root.getKey()),
                Key.apply("emptyOf", root.getValue()));
        }
        return storage;
    }

    /// The `\functions` declarations of the struct shape constants.
    public List<KeyProblem.Function> declarations() {
        return structShapes.keySet().stream()
                .map(struct -> new KeyProblem.Function(true, "Shape", shapeConstant(struct)))
                .toList();
    }

    /// The rules unfolding `wellformed(s)` into one `wf` atom per non-trivial root and each
    /// struct shape into the atoms of its members.
    public List<KeyProblem.Taclet> rules() {
        List<KeyProblem.Taclet> rules = new ArrayList<>();
        rules.add(rule("insertWellformed", Key.predicate("wellformed", S), conjunction(roots)));
        for (Map.Entry<String, Shape> struct : structShapes.entrySet()) {
            Map<String, KeyTerm> members = new LinkedHashMap<>();
            for (SolidityOutline.Variable member : contract.structs().get(struct.getKey())) {
                Shape shape = shapeOf(member.type());
                if (!shape.isTrivial()) {
                    members.put(
                        StructLDT.fieldConstantName(contract.name(), struct.getKey(),
                            member.name()),
                        shape.render(contract.name()));
                }
            }
            rules.add(rule("wfStruct_" + struct.getKey(),
                Key.predicate("wf", Key.constant(shapeConstant(struct.getKey())), S),
                conjunction(members)));
        }
        return rules;
    }

    private static final KeyTerm S = Key.constant("s");

    private static KeyProblem.Taclet rule(String name, KeyFormula find, KeyFormula body) {
        return new KeyProblem.Taclet(name, "Struct", List.of("s"), false, find, body,
            "simplify");
    }

    private static KeyFormula conjunction(Map<String, KeyTerm> atoms) {
        List<KeyFormula> parts = new ArrayList<>();
        for (Map.Entry<String, KeyTerm> atom : atoms.entrySet()) {
            parts.add(Key.predicate("wf", atom.getValue(),
                Key.typed("selectSt", "Struct", S, Key.constant(atom.getKey()))));
        }
        return Key.and(parts);
    }

    private String field(String variable) {
        return StructLDT.fieldConstantName(contract.name(), variable);
    }

    private String shapeConstant(String struct) {
        return shapeConstantName(contract.name(), struct);
    }

    public static String shapeConstantName(String contract, String struct) {
        return StructLDT.fieldConstantName(contract, struct, SHAPE_SUFFIX);
    }

    private boolean isTrivialStruct(String struct, Set<String> visiting) {
        if (!visiting.add(struct)) {
            return true;
        }
        for (SolidityOutline.Variable member : contract.structs().get(struct)) {
            if (!parse(member.type(), visiting).isTrivial()) {
                return false;
            }
        }
        return true;
    }

    private Shape shapeOf(String typeString) {
        return parse(typeString, null);
    }

    private Shape parse(String typeString, Set<String> visiting) {
        String type = typeString.strip();
        for (String location : List.of(" storage ref", " storage pointer", " memory",
            " calldata")) {
            if (type.endsWith(location)) {
                type = type.substring(0, type.length() - location.length()).strip();
            }
        }
        if (type.endsWith("]")) {
            int open = type.lastIndexOf('[');
            String length = type.substring(open + 1, type.length() - 1).strip();
            Shape element = parse(type.substring(0, open), visiting);
            return length.isEmpty() ? new Shape.Dyn(element)
                    : new Shape.Fixed(length, element);
        }
        if (type.startsWith("mapping(") && type.endsWith(")")) {
            String inner = type.substring("mapping(".length(), type.length() - 1);
            int depth = 0;
            for (int i = 0; i < inner.length() - 1; i++) {
                char c = inner.charAt(i);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                } else if (depth == 0 && c == '=' && inner.charAt(i + 1) == '>') {
                    return new Shape.MapOf(parse(inner.substring(i + 2), visiting));
                }
            }
            return Shape.LEAF;
        }
        if (type.startsWith("struct ")) {
            String name = type.substring("struct ".length());
            name = name.substring(name.lastIndexOf('.') + 1);
            if (!contract.structs().containsKey(name)) {
                return Shape.LEAF;
            }
            boolean trivial = visiting == null ? !structShapes.containsKey(name)
                    : isTrivialStruct(name, visiting);
            return trivial ? Shape.LEAF : new Shape.StructOf(name);
        }
        return Shape.LEAF;
    }

    sealed interface Shape {
        Shape LEAF = new Leaf();

        boolean isTrivial();

        KeyTerm render(String contract);

        record Leaf() implements Shape {
            @Override
            public boolean isTrivial() {
                return true;
            }

            @Override
            public KeyTerm render(String contract) {
                return Key.constant("leaf");
            }
        }

        record Fixed(String length, Shape element) implements Shape {
            @Override
            public boolean isTrivial() {
                return false;
            }

            @Override
            public KeyTerm render(String contract) {
                return Key.apply("fixedArr", Key.num(length), element.render(contract));
            }
        }

        record Dyn(Shape element) implements Shape {
            @Override
            public boolean isTrivial() {
                return false;
            }

            @Override
            public KeyTerm render(String contract) {
                return Key.apply("dynArr", element.render(contract));
            }
        }

        record MapOf(Shape value) implements Shape {
            @Override
            public boolean isTrivial() {
                return value.isTrivial();
            }

            @Override
            public KeyTerm render(String contract) {
                return Key.apply("mapOf", value.render(contract));
            }
        }

        record StructOf(String name) implements Shape {
            @Override
            public boolean isTrivial() {
                return false;
            }

            @Override
            public KeyTerm render(String contract) {
                return Key.constant(shapeConstantName(contract, name));
            }
        }
    }
}
