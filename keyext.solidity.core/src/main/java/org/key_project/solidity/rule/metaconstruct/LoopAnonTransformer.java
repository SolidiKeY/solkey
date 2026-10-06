/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import java.util.ArrayList;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;
import org.key_project.solidity.logic.SolidityDLTheory;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.op.SFunction;
import org.key_project.solidity.logic.op.SModality;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

public final class LoopAnonTransformer extends AbstractTermTransformer {

    public LoopAnonTransformer() {
        super(new Name("#loopAnon"), 2, SolidityDLTheory.FORMULA);
    }

    @Override
    public Term transform(Term term, SVInstantiations svInst, Services services) {
        SModality modality = (SModality) term.sub(0).op();
        LoopFrame frame = LoopFrame.of(modality.programBlock().program());
        TermBuilder tb = services.getTermBuilder();
        NamespaceSet namespaces = services.getNamespaces();
        List<Term> updates = new ArrayList<>();
        for (ProgramVariable local : frame.locals()) {
            updates.add(anonymise(local, services));
        }
        if (frame.storage()) {
            updates.add(anonymise(namespaces.programVariables().lookup("storage"), services));
        }
        if (frame.net()) {
            updates.add(anonymise(namespaces.programVariables().lookup("net"), services));
        }
        Term phi = term.sub(1);
        return updates.isEmpty() ? phi
                : tb.apply(tb.parallel(updates.toArray(new Term[0])), phi);
    }

    private static Term anonymise(ProgramVariable variable, Services services) {
        TermBuilder tb = services.getTermBuilder();
        return tb.elementary(variable, tb.func(fresh(variable.name() + "_anon",
            variable.sort(), services.getNamespaces())));
    }

    private static SFunction fresh(String base, Sort sort, NamespaceSet namespaces) {
        Name name = new Name(base);
        for (int i = 1; namespaces.lookupLogicSymbol(name) != null
                || namespaces.programVariables().lookup(name) != null; i++) {
            name = new Name(base + "_" + i);
        }
        SFunction function = new SFunction(name, sort, true, false);
        namespaces.functions().addSafely(function);
        return function;
    }
}
