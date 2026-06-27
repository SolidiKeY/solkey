/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.SourceData;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.StateMutability;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.references.ModifierReference;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.solidity.rule.matching.inst.MatchConditions;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class FunctionDeclaration implements Declaration, Named, SolidityProgramElement {
    // TODO: Create another class for return type
    private final ImmutableArray<ProgramVariable> returnParameters;
    private final ImmutableArray<ProgramVariable> inputParameters;
    private final Block body;
    private final String kind;
    private final Visibility visibility;
    private final StateMutability stateMutability;
    private final ImmutableArray<ModifierReference> modifiers;
    private final Name name;
    private final Type type;

    public String getDocumentation() {
        return documentation;
    }

    private final String documentation;

    public FunctionDeclaration(Name name, List<ProgramVariable> returnParameters, Type type,
            List<ProgramVariable> inputParameters, Block body, String kind,
            Visibility visibility, StateMutability stateMutability,
            List<ModifierReference> modifiers, String documentation) {
        this.name = name;
        this.returnParameters = new ImmutableArray<>(returnParameters);
        this.type = type;
        this.inputParameters = new ImmutableArray<>(inputParameters);
        this.body = body;
        this.kind = kind;
        this.visibility = visibility;
        this.stateMutability = stateMutability;
        this.modifiers = new ImmutableArray<>(modifiers);
        this.documentation = documentation;
    }

    public Block getBody() {
        return body;
    }

    public ImmutableArray<ProgramVariable> getReturnParameters() {
        return returnParameters;
    }

    public ImmutableArray<ProgramVariable> getInputParameters() {
        return inputParameters;
    }

    // Interface SolidityProgramElement

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n < returnParameters.size())
            return returnParameters.get(n);
        n -= returnParameters.size();
        if (n < inputParameters.size())
            return inputParameters.get(n);
        n -= inputParameters.size();
        if (n < modifiers.size())
            return modifiers.get(n);
        n -= modifiers.size();
        if (body != null && n == 0)
            return body;
        throw new IndexOutOfBoundsException(
            "Index should be 0 <= " + n + " < " + getChildCount());
    }

    @Override
    public int getChildCount() {
        return returnParameters.size() + inputParameters.size() + modifiers.size()
                + (body == null ? 0 : 1);
    }

    @Override
    public @Nullable MatchConditions match(SourceData sourceData, @Nullable MatchConditions mc) {
        if (!(sourceData.getSource() instanceof FunctionDeclaration sourceFunction)) {
            return null;
        }
        if (!name.equals(sourceFunction.name)) {
            return null;
        }
        sourceData.next();
        return mc;
    }

    @Override
    public void visit(Visitor v) {
    }

    @Override
    public String toString() {
        StringBuffer strBuffer = new StringBuffer();
        String params = inputParameters.stream().map(ProgramVariable::typeAndName)
                .collect(Collectors.joining(", "));
        strBuffer.append("function ");
        strBuffer.append(name)
                .append(" (")
                .append(params)
                .append(") ")
                .append(visibility)
                .append(" ")
                .append(stateMutability)
                .append(" ")
                .append(modifiers.stream().map(ModifierReference::toString)
                        .collect(Collectors.joining(" ")))
                .append(getBody().toString());
        return strBuffer.toString();
    }

    public ImmutableArray<ModifierReference> getModifiers() {
        return modifiers;
    }

    public String getKind() {
        return kind;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public StateMutability getStateMutability() {
        return stateMutability;
    }

    public Type getType() {
        return type;
    }

    @Override
    public Name name() {
        return name;
    }
}
