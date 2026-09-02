/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.proof.init;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.key_project.solidity.program.parser.SolidityOutline;
import org.key_project.solidity.program.parser.SolidityOutline.Field;
import org.key_project.solidity.program.parser.SolidityOutline.StorageType;
import org.key_project.solidity.theory.StructLDT;

/// - a `uintN` cell is non-negative — `0 <= find<[int]>(storage, path)`;
/// - an array's length cell is non-negative — `0 <= find<[int]>(storage, path · size)`.
public final class WellFormedTacletGenerator {

    /// The expansion is stated over the contract-storage program variable itself rather than a
    /// schema variable: a schema variable occurring both outside and inside the mapping
    /// quantifiers would need a `\notFreeIn` varcond, and `wellFormed(storage)` is the only
    /// instance an obligation assumes.
    private static final String STORAGE = StructLDT.STORAGE_NAME.toString();

    private WellFormedTacletGenerator() {}

    /// The `\rules { … }` block declaring `wellFormedExpand` for `contract`, or the empty string
    /// when the layout constrains nothing (no `uint` cell and no array anywhere).
    public static String rulesBlock(SolidityOutline.Contract contract) {
        Layout layout = new Layout(contract);
        List<String> conjuncts = new ArrayList<>();
        for (Field field : contract.stateVariables()) {
            conjuncts.addAll(layout.wellFormed(layout.root(field), field.type()));
        }
        if (conjuncts.isEmpty()) {
            return "";
        }
        return """
                \\rules {
                    wellFormedExpand {
                        \\find(wellFormed(%s))
                        \\replacewith(%s)
                        \\heuristics(simplify)
                    };
                }

                """.formatted(STORAGE, String.join("\n            & ", conjuncts));
    }

    /// The recursion over one contract's declared layout. Mapping keys are numbered as they are
    /// bound so the conjunction never captures, and the struct types currently on the recursion
    /// path are tracked so a recursive layout (`struct S { mapping(uint => S) m; }`) stops
    /// instead of unfolding forever.
    private static final class Layout {
        private final SolidityOutline.Contract contract;
        private final Set<String> onPath = new LinkedHashSet<>();
        private int boundVariables = 0;

        Layout(SolidityOutline.Contract contract) {
            this.contract = contract;
        }

        /// The constraints the layout puts on the cell reached by `path`, as separate conjuncts.
        List<String> wellFormed(String path, StorageType type) {
            List<String> conjuncts = new ArrayList<>();
            switch (type) {
                case StorageType.Primitive primitive -> {
                    if (primitive.isUnsigned()) {
                        conjuncts.add("0 <= " + read(path));
                    }
                }
                case StorageType.Struct struct -> {
                    if (onPath.add(struct.name())) {
                        contract.struct(struct.name()).ifPresent(definition -> {
                            for (Field member : definition.members()) {
                                conjuncts.addAll(wellFormed(
                                    member(path, struct.name(), member.name()), member.type()));
                            }
                        });
                        onPath.remove(struct.name());
                    }
                }
                case StorageType.Array ignored -> conjuncts.add("0 <= " + read(segment(path,
                    "size")));
                case StorageType.Mapping mapping -> {
                    if (mapping.key() instanceof StorageType.Primitive key && isInteger(key)) {
                        String argument = "k" + boundVariables++;
                        List<String> values =
                            wellFormed(segment(path, "at(" + argument + ")"), mapping.value());
                        if (!values.isEmpty()) {
                            conjuncts.add("\\forall int " + argument + "; ("
                                + String.join("\n                & ", values) + ")");
                        }
                    }
                }
            }
            return conjuncts;
        }

        String root(Field field) {
            return "cons1(" + contract.name() + StructLDT.FIELD_SEPARATOR + field.name() + ")";
        }

        private String read(String path) {
            return "find<[int]>(" + STORAGE + ", " + path + ")";
        }

        private String segment(String path, String field) {
            return "consr(" + path + ", " + field + ")";
        }

        private String member(String path, String struct, String field) {
            return segment(path, contract.name() + StructLDT.FIELD_SEPARATOR + struct
                    + StructLDT.FIELD_SEPARATOR + field);
        }

        /// The key types `at(_)` accepts: `at` indexes by the mathematical integers, and an
        /// address is one too.
        private boolean isInteger(StorageType.Primitive key) {
            return key.name().matches("u?int\\d*") || "address".equals(key.name());
        }
    }
}
