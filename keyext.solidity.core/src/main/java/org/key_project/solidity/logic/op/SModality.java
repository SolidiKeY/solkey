/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.op;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.key_project.logic.Name;
import org.key_project.logic.TermCreationException;
import org.key_project.solidity.logic.SolidityBlock;
import org.key_project.solidity.logic.SolidityDLTheory;
import org.key_project.solidity.program.ast.SolidityProgramElement;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// This class is used to represent a dynamic logic modality like diamond and box (but also
/// extensions of DL like preserves and throughout are possible in the future).
public class SModality extends org.key_project.logic.op.Modality {
    /// keeps track of created modalities
    private static final Map<SolidityProgramElement, WeakHashMap<SolidityModalityKind, WeakReference<SModality>>> modalities =
        new WeakHashMap<>();

    /// Retrieves the modality of the given useKind and program.
    ///
    /// @param kind the useKind of the modality such as diamond or box
    /// @param sb the program of this modality
    /// @return the modality of the given useKind and program.
    public static synchronized SModality getModality(SolidityModalityKind kind, SolidityBlock sb) {
        var kind2mod = modalities.get(sb.program());
        final SModality mod;
        WeakReference<SModality> modRef;
        if (kind2mod == null) {
            kind2mod = new WeakHashMap<>();
            mod = new SModality(sb, kind);
            modRef = new WeakReference<>(mod);
            kind2mod.put(kind, modRef);
            modalities.put(sb.program(), kind2mod);
        } else {
            modRef = kind2mod.get(kind);
            if (modRef == null || modRef.get() == null) {
                mod = new SModality(sb, kind);
                modRef = new WeakReference<>(mod);
                kind2mod.put(kind, modRef);
                modalities.put(sb.program(), kind2mod);
            } else {
                mod = modRef.get();
                assert mod != null;
            }
        }
        return mod;
    }

    private final SolidityBlock block;

    /// Creates a modal operator with the given name
    /// **Creation must only be done by ???!**
    private SModality(SolidityBlock prg, SolidityModalityKind kind) {
        super(kind.name(), SolidityDLTheory.FORMULA, kind);
        this.block = prg;
    }

    @Override
    public @NonNull SolidityBlock programBlock() {
        return block;
    }

    @Override
    public void validTopLevelException(org.key_project.logic.Term term)
            throws TermCreationException {
        if (1 != term.arity()) {
            throw new TermCreationException(this, term);
        }

        if (1 != term.subs().size()) {
            throw new TermCreationException(this, term);
        }

        if (!term.boundVars().isEmpty()) {
            throw new TermCreationException(this, term);
        }

        if (term.sub(0) == null) {
            throw new TermCreationException(this, term);
        }
    }

    public static class SolidityModalityKind extends Kind {
        private static final Map<String, SolidityModalityKind> kinds = new HashMap<>();
        /// The diamond operator of dynamic logic. A formula <alpha;>Phi can be read as after
        /// processing the program alpha there exists a state such that Phi holds.
        public static final SolidityModalityKind DIA =
            new SolidityModalityKind(new Name("diamond"));
        /// The box operator of dynamic logic. A formula \[alpha;]Phi can be read as 'In all states
        /// reachable processing the program alpha the formula Phi holds'.
        public static final SolidityModalityKind BOX = new SolidityModalityKind(new Name("box"));

        public SolidityModalityKind(Name name) {
            super(name);
            kinds.put(name.toString(), this);
        }

        public static SModality.@Nullable SolidityModalityKind getKind(String name) {
            return kinds.get(name);
        }

        /// Whether this modality is termination sensitive, i.e., it is a "diamond-useKind"
        /// modality.
        public boolean terminationSensitive() {
            return (this == DIA);
        }
    }
}
