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
import org.key_project.solidity.program.ast.abstractions.TupleType;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.StateMutability;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.references.ModifierReference;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;

public class FunctionDeclaration extends DeclarationClass implements Named {
    // TODO: Create another class for return type
    private final ImmutableArray<ProgramVariable> returnParameters;
    private final ImmutableArray<ProgramVariable> inputParameters;
    private final Block body;
    private final String kind;
    private final Visibility visibility;
    private final StateMutability stateMutability;
    private final ImmutableArray<ModifierReference> modifiers;
    private final Name name;
    private final TupleType type;

    public String getDocumentation() {
        return documentation;
    }

    private final String documentation;

    public FunctionDeclaration(Name name, List<ProgramVariable> returnParameters, TupleType type,
            List<ProgramVariable> inputParameters, Block body, String kind,
            Visibility visibility, StateMutability stateMutability,
            List<ModifierReference> modifiers, String documentation) {
        super(new ImmutableArray<>());
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
        if (n < returnParameters.size()) {
            return returnParameters.get(n);
        } else {
            n -= returnParameters.size();
            if (n < inputParameters.size()) {
                return inputParameters.get(n);
            } else {
                n -= inputParameters.size();
                if (n < modifiers.size()) {
                    return modifiers.get(n);
                } else {
                    n -= modifiers.size();
                    if (n == 0) {
                        return body;
                    }
                    throw new IndexOutOfBoundsException("Index out of bounds");
                }
            }
        }
    }

    @Override
    public int getChildCount() {
        return returnParameters.size() + inputParameters.size() + modifiers.size() + 1;
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

    public TupleType getType() {
        return type;
    }

    @Override
    public Name name() {
        return name;
    }
}
