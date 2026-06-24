/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.metaconstruct;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;

/// Resolves `#defaultOf(F)` where `F` is a Field constant by looking up its
/// declared Solidity type via [Services#getSolidityInfo] and returning the
/// matching default value term. Non-Field arguments (e.g. unevaluated
/// `last(...)`, `at(idx)`) fall back to the empty struct `mtSt`.
public class MetaDefaultOf extends AbstractTermTransformer {
    private static final Name EMPTY_STRUCT = new Name("mtSt");

    public MetaDefaultOf() {
        super(new Name("#defaultOf"), 1);
    }

    public Term transform(Term term, SVInstantiations svInst, Services services) {
        Name fieldName = term.sub(0).op().name();
        Type type = services.getSolidityInfo().getFieldType(fieldName);
        TermBuilder tb = services.getTermBuilder();
        if (type instanceof PrimitiveType pt) {
            String n = pt.name().toString();
            if (n.contains("int") || n.equals("address")) {
                return tb.zTerm("0");
            }
            if (n.equals("bool")) {
                return tb.ff();
            }
        }
        Function mtSt = services.getNamespaces().functions().lookup(EMPTY_STRUCT);
        return tb.func(mtSt);
    }
}
