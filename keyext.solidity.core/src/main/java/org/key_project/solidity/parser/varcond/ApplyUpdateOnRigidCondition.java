/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.Shift;
import org.key_project.solidity.logic.TermBuilder;
import org.key_project.solidity.rule.matching.inst.SVInstantiations;
import org.key_project.solidity.rule.sv.UpdateSV;

import static org.key_project.solidity.logic.equality.RenamingTermProperty.RENAMING_TERM_PROPERTY;

/// This variable condition can be used to check whether an update can be performed on a formula or
/// term.
/// That is the case if the top-level operator is rigid and of arity greater than 0.
///
/// @author Benjamin Weiss
/// @author Tobias Reinhold
public final class ApplyUpdateOnRigidCondition implements VariableCondition {

    /// The schema variable matched against an update
    private final UpdateSV u;

    /// The schema variable matched against a formula or term
    private final SchemaVariable phi;

    /// The schema variable containing the result of applying the update <code>u</code> on the
    /// formula or term <code>phi</code>
    private final SchemaVariable result;

    /// Creates an instance of the variable condition.
    ///
    /// @param u the schema variable matched against an update
    /// @param phi the schema variable matched against a formula or term
    /// @param result the schema variable containing the result of applying <code>u</code> on
    /// <code>phi</code>
    public ApplyUpdateOnRigidCondition(UpdateSV u, SchemaVariable phi,
            SchemaVariable result) {
        this.u = u;
        this.phi = phi;
        this.result = result;
    }

    /// Applies the update <code>u</code> on the rigid formula or term <code>phi</code>.
    ///
    /// Below a binder of <code>phi</code>, the free variables of <code>u</code> are shifted past
    /// the variables bound there, so that the binder does not capture them.
    ///
    /// @param u the update applied on <code>phi</code>
    /// @param phi the formula or term the update <code>u</code> is applied on
    /// @param services the [Services] to help create terms
    /// @return the term of the update <code>u</code> applied on all subterms of <code>phi</code>
    private static Term applyUpdateOnRigid(Term u, Term phi, Services services) {
        final TermBuilder tb = services.getTermBuilder();
        final Term[] updatedSubs = new Term[phi.arity()];
        for (int i = 0; i < updatedSubs.length; i++) {
            final Term shiftedU = phi.op().bindVarsAt(i)
                    ? new Shift(phi.varsBoundHere(i).size(), tb).apply(u)
                    : u;
            updatedSubs[i] = tb.apply(shiftedU, phi.sub(i));
        }
        return services.getTermFactory().createTerm(phi.op(), updatedSubs, phi.boundVars());
    }

    @Override
    public MatchResultInfo check(SchemaVariable var, SyntaxElement instCandidate,
            MatchResultInfo mc,
            LogicServices services) {
        var svInst = (SVInstantiations) mc.getInstantiations();
        final Term uInst = svInst.getInstantiation(u);
        final Term phiInst = svInst.getInstantiation(phi);
        final Term resultInst = svInst.getInstantiation(result);
        if (uInst == null || phiInst == null) {
            return mc;
        }

        if (!phiInst.op().isRigid() || phiInst.op().arity() == 0) {
            return null;
        }
        Term properResultInst = applyUpdateOnRigid(uInst, phiInst, (Services) services);
        if (resultInst == null) {
            svInst = svInst.add(result, properResultInst, services);
            return mc.setInstantiations(svInst);
        } else if (RENAMING_TERM_PROPERTY.equalsModThisProperty(resultInst, properResultInst)) {
            return mc;
        } else {
            return null;
        }
    }


    @Override
    public String toString() {
        return "\\applyUpdateOnRigid(" + u.name() + ", " + phi.name() + ", " + result.name() + ")";
    }
}
