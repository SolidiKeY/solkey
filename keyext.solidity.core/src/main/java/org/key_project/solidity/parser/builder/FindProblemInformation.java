/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser.builder;

import org.key_project.solidity.parser.KeYSolidityDLParser;
import org.key_project.solidity.parser.ProblemInformation;
import org.key_project.util.java.StringUtil;

import org.jspecify.annotations.NonNull;

public class FindProblemInformation extends AbstractBuilder<Object> {
    private final @NonNull ProblemInformation information = new ProblemInformation();

    @Override
    public Object visitFile(KeYSolidityDLParser.FileContext ctx) {
        if (ctx.profile() != null) {
            information.setProfile(accept(ctx.profile()));
        }
        if (ctx.preferences() != null) {
            information.setPreferences(accept(ctx.preferences()));
        }
        each(ctx.decls(), ctx.problem());
        return null;
    }

    @Override
    public Object visitDecls(KeYSolidityDLParser.DeclsContext ctx) {
        information.setSoliditySource(acceptFirst(ctx.programSource()));
        return null;
    }

    @Override
    public Object visitProblem(KeYSolidityDLParser.ProblemContext ctx) {
        if (ctx.CHOOSECONTRACT() != null) {
            if (ctx.chooseContract != null) {
                information.setChooseContract(accept(ctx.chooseContract));
            } else {
                information.setChooseContract("");
            }
        }
        if (ctx.PROOFOBLIGATION() != null) {
            if (ctx.proofObligation != null) {
                information.setProofObligation(accept(ctx.proofObligation));
            } else {
                information.setProofObligation("");
            }
        }
        information.setHasProblemTerm(ctx.PROBLEM() != null);
        return null;
    }

    @Override
    public String visitString_value(KeYSolidityDLParser.String_valueContext ctx) {
        return null; // ParsingFacade.getValueDocumentation(ctx);
    }


    @Override
    public String visitProgramSource(KeYSolidityDLParser.ProgramSourceContext ctx) {
        return ctx.oneProgramSource() != null ? (String) accept(ctx.oneProgramSource()) : null;
    }

    @Override
    public String visitOneProgramSource(KeYSolidityDLParser.OneProgramSourceContext ctx) {
        return StringUtil.trim(ctx.getText(), '"');
    }

    @Override
    public Object visitProfile(KeYSolidityDLParser.ProfileContext ctx) {
        return accept(ctx.name);
    }

    @Override
    public String visitPreferences(KeYSolidityDLParser.PreferencesContext ctx) {
        return ctx.s != null ? (String) accept(ctx.s) : null;
    }

    /// The found problem information.
    public @NonNull ProblemInformation getProblemInformation() {
        return information;
    }
}
