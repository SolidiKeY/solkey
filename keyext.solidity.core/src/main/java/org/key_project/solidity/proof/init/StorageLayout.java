/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.key_project.solidity.keyfile.Key;
import org.key_project.solidity.keyfile.KeyFormula;
import org.key_project.solidity.keyfile.KeyProblem;
import org.key_project.solidity.keyfile.KeyTerm;
import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.theory.StructLDT;

public final class StorageLayout {

    private final List<String> roots = new ArrayList<>();
    private final List<KeyProblem.Taclet> memberOrder = new ArrayList<>();

    public StorageLayout(SolidityOutline.Contract contract) {
        for (SolidityOutline.Variable variable : contract.stateVariables()) {
            if (variable.constantValue() == null
                    && !isTrivial(contract, variable.type(), new HashSet<>())) {
                roots.add(StructLDT.fieldConstantName(contract.name(), variable.name()));
            }
        }
        Map<String, String> owners = structOwners(contract);
        contract.structs().forEach((struct, members) -> {
            String owner = owners.get(struct);
            if (owner == null || !(owner.isEmpty() || owner.equals(contract.name()))) {
                return;
            }
            List<KeyFormula> earlier = new ArrayList<>();
            for (SolidityOutline.Variable member : members) {
                String field = owner.isEmpty()
                        ? StructLDT.fieldConstantName(struct, member.name())
                        : StructLDT.fieldConstantName(owner, struct, member.name());
                KeyFormula isEarlier = earlier.isEmpty() ? Key.not(Key.eq(M, M))
                        : earlier.stream().reduce(Key::or).orElseThrow();
                memberOrder.add(new KeyProblem.Taclet(
                    "memberBefore_" + struct + "_" + member.name(), "MemberField",
                    List.of("m"), false, Key.predicate("before", M, Key.constant(field)),
                    isEarlier, "concrete"));
                earlier.add(Key.eq(M, Key.constant(field)));
            }
        });
    }

    private static final Pattern STRUCT_TYPE = Pattern.compile("struct (?:(\\w+)\\.)?(\\w+)");

    private static Map<String, String> structOwners(SolidityOutline.Contract contract) {
        Map<String, String> owners = new HashMap<>();
        List<String> types = new ArrayList<>();
        contract.stateVariables().forEach(v -> types.add(v.type()));
        contract.structs().values().forEach(ms -> ms.forEach(m -> types.add(m.type())));
        for (String type : types) {
            Matcher matcher = STRUCT_TYPE.matcher(type);
            while (matcher.find()) {
                owners.putIfAbsent(matcher.group(2),
                    matcher.group(1) == null ? "" : matcher.group(1));
            }
        }
        return owners;
    }

    public boolean isEmpty() {
        return roots.isEmpty();
    }

    /// The constructor's starting storage: the empty storage, as on the EVM.
    public KeyTerm emptyStorage() {
        return Key.constant("mtSt");
    }

    /// The rule unfolding `lenInv(s)` into one `conforms` atom per root that reaches a dynamic
    /// array.
    public List<KeyProblem.Taclet> rules() {
        List<KeyFormula> atoms = new ArrayList<>();
        for (String root : roots) {
            atoms.add(Key.predicate("conforms", fieldType(root),
                Key.typed("selectSt", "Struct", S, Key.constant(root))));
        }
        List<KeyProblem.Taclet> rules = new ArrayList<>();
        rules.add(new KeyProblem.Taclet("insertLenInv", "Struct", List.of("s"), false,
            Key.predicate("lenInv", S), Key.and(atoms), "simplify"));
        rules.addAll(memberOrder);
        return rules;
    }

    private static final KeyTerm S = Key.constant("s");
    private static final KeyTerm M = Key.constant("m");

    private static KeyTerm fieldType(String root) {
        return Key.apply("fieldType", Key.constant(root));
    }

    private static boolean isTrivial(SolidityOutline.Contract contract, String typeString,
            Set<String> visiting) {
        String type = typeString.strip();
        for (String location : List.of(" storage ref", " storage pointer", " memory",
            " calldata")) {
            if (type.endsWith(location)) {
                type = type.substring(0, type.length() - location.length()).strip();
            }
        }
        if (type.endsWith("[]")) {
            return false;
        }
        if (type.endsWith("]")) {
            return isTrivial(contract, type.substring(0, type.lastIndexOf('[')), visiting);
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
                    return isTrivial(contract, inner.substring(i + 2), visiting);
                }
            }
            return true;
        }
        if (type.startsWith("struct ")) {
            String name = type.substring("struct ".length());
            name = name.substring(name.lastIndexOf('.') + 1);
            if (!contract.structs().containsKey(name) || !visiting.add(name)) {
                return true;
            }
            for (SolidityOutline.Variable member : contract.structs().get(name)) {
                if (!isTrivial(contract, member.type(), visiting)) {
                    return false;
                }
            }
        }
        return true;
    }
}
