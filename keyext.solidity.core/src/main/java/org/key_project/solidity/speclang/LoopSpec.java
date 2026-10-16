/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.speclang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.parser.SolidityOutline;

import org.jspecify.annotations.Nullable;

public record LoopSpec(List<String> invariants, @Nullable String decreases,
        Map<String, Binding> bindings, SolidityOutline.Contract contract,
        SolidityOutline.Function function, @Nullable ProgramVariable breakFlag) {

    public LoopSpec(List<String> invariants, @Nullable String decreases,
            Map<String, Binding> bindings, SolidityOutline.Contract contract,
            SolidityOutline.Function function) {
        this(invariants, decreases, bindings, contract, function, null);
    }

    public LoopSpec withBreakFlag(@Nullable ProgramVariable variable) {
        return new LoopSpec(invariants, decreases, bindings, contract, function, variable);
    }

    public record Binding(ProgramVariable variable, String type) {
    }

    public LoopSpec rename(Map<ProgramVariable, ProgramVariable> replacements) {
        Map<String, Binding> renamed = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<String, Binding> entry : bindings.entrySet()) {
            Binding binding = entry.getValue();
            ProgramVariable replacement = replacements.get(binding.variable());
            if (replacement != null) {
                binding = new Binding(replacement, binding.type());
                changed = true;
            }
            renamed.put(entry.getKey(), binding);
        }
        ProgramVariable renamedBreak = breakFlag == null ? null
                : replacements.getOrDefault(breakFlag, breakFlag);
        return changed || renamedBreak != breakFlag
                ? new LoopSpec(invariants, decreases, renamed, contract, function, renamedBreak)
                : this;
    }
}
