/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule.matching.instructions;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.OperatorSV;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.matcher.vm.instruction.CheckNodeKindInstruction;
import org.key_project.prover.rules.matcher.vm.instruction.GotoNextInstruction;
import org.key_project.prover.rules.matcher.vm.instruction.GotoNextSiblingInstruction;
import org.key_project.prover.rules.matcher.vm.instruction.MatchIdentityInstruction;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.solidity.logic.op.ParametricFunctionInstance;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.rule.sv.ModalOperatorSV;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.rule.sv.VariableSV;

/// Class encoding the instructions of the matching vm
public class SolidityDLMatchInstructionSet {
    public static GotoNextInstruction gotoNextInstruction() {
        return GotoNextInstruction.INSTANCE;
    }

    public static GotoNextSiblingInstruction gotoNextSiblingInstruction() {
        return GotoNextSiblingInstruction.INSTANCE;
    }

    public static MatchModalOperatorSVInstruction matchModalOperatorSV(
            ModalOperatorSV sv) {
        return new MatchModalOperatorSVInstruction(sv);
    }

    public static MatchSchemaVariableInstruction matchNonVariableSV(OperatorSV sv) {
        return new MatchNonVariableSVInstruction(sv);
    }

    public static MatchSchemaVariableInstruction matchVariableSV(
            VariableSV sv) {
        return new MatchVariableSVInstruction(sv);
    }

    public static MatchSchemaVariableInstruction matchProgramSV(
            ProgramSV sv) {
        return new MatchProgramSVInstruction(sv);
    }

    public static MatchInstruction matchProgram(SolidityProgramElement prg) {
        return new MatchProgramInstruction(prg);
    }

    /// returns the instruction for the specified variable
    ///
    /// @param op the [SchemaVariable] for which to get the instruction
    /// @return the instruction for the specified variable
    public static MatchSchemaVariableInstruction getMatchInstructionForSV(
            SchemaVariable op) {
        return switch (op) {
            case VariableSV variableSV -> matchVariableSV(variableSV);
            case ProgramSV programSV -> matchProgramSV(programSV);
            case OperatorSV operatorSV -> matchNonVariableSV(operatorSV);
            default -> throw new IllegalArgumentException(
                "Do not know how to match " + op + " of type " + op.getClass());
        };
    }

    public static SimilarParametricFunctionInstruction getSimilarParametricFunctionInstruction(
            ParametricFunctionInstance psi) {
        return new SimilarParametricFunctionInstruction(psi);
    }

    public static MatchIdentityInstruction getMatchIdentityInstruction(
            SyntaxElement syntaxElement) {
        return new MatchIdentityInstruction(syntaxElement);
    }

    public static MatchGenericSortInstruction getMatchGenericSortInstruction(GenericSort gs) {
        return new MatchGenericSortInstruction(gs);
    }

    public static CheckNodeKindInstruction getCheckNodeKindInstruction(
            Class<? extends SyntaxElement> kind) {
        return new CheckNodeKindInstruction(kind);
    }

    public static MatchInstruction matchAndBindVariable(
            QuantifiableVariable var) {
        return BindVariablesInstruction.create(var);
    }

    public static MatchInstruction unbindVariables(int size) {
        return new UnbindVariablesInstruction(size);
    }
}
