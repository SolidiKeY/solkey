/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.key_project.solidity.keyfile.Key;
import org.key_project.solidity.keyfile.KeyFormula;
import org.key_project.solidity.keyfile.KeyProblem;
import org.key_project.solidity.keyfile.KeyTerm;
import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.theory.StructLDT;

public final class StorageLayout {

    private final List<String> roots = new ArrayList<>();

    public StorageLayout(SolidityOutline.Contract contract) {
        for (SolidityOutline.Variable variable : contract.stateVariables()) {
            if (variable.constantValue() == null
                    && !isTrivial(contract, variable.type(), new HashSet<>())) {
                roots.add(StructLDT.fieldConstantName(contract.name(), variable.name()));
            }
        }
    }

    public boolean isEmpty() {
        return roots.isEmpty();
    }

    /// The constructor's starting storage: every non-trivial root holds the empty node of its
    /// declared type, stored into `mtSt`.
    public KeyTerm emptyStorage() {
        KeyTerm storage = Key.constant("mtSt");
        for (String root : roots) {
            storage = Key.apply("storeSt", storage, Key.constant(root), fieldType(root));
        }
        return storage;
    }

    /// The rule unfolding `layoutInv(s)` into one `conforms` atom per non-trivial root.
    public List<KeyProblem.Taclet> rules() {
        List<KeyFormula> atoms = new ArrayList<>();
        for (String root : roots) {
            atoms.add(Key.predicate("conforms", fieldType(root),
                Key.typed("selectSt", "Struct", S, Key.constant(root))));
        }
        return List.of(new KeyProblem.Taclet("insertLayoutInv", "Struct", List.of("s"), false,
            Key.predicate("layoutInv", S), Key.and(atoms), "simplify"));
    }

    private static final KeyTerm S = Key.constant("s");

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
        if (type.endsWith("]")) {
            return false;
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
