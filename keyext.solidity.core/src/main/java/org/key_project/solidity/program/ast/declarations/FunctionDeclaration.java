/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.ast.declarations;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.StateMutability;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility;
import org.key_project.solidity.program.ast.references.ModifierReference;
import org.key_project.solidity.program.ast.statement.Block;
import org.key_project.solidity.program.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;

public class FunctionDeclaration extends Declaration {
    private final ImmutableArray<ParameterDeclaration> returnParameters;
    private final ImmutableArray<ParameterDeclaration> inputParameters;
    private final Block body;

    public String getKind() {
        return kind;
    }

    private final String kind;

    public Visibility getVisibility() {
        return visibility;
    }

    private final Visibility visibility;

    public StateMutability getStateMutability() {
        return stateMutability;
    }

    private final StateMutability stateMutability;
    private final ImmutableArray<ModifierReference> modifiers;

    public Name getName() {
        return name;
    }

    private final Name name;

    public FunctionDeclaration(Name name, List<ParameterDeclaration> returnParameters,
            List<ParameterDeclaration> inputParameters, Block body, String kind,
            Visibility visibility, StateMutability stateMutability,
            List<ModifierReference> modifiers) {
        super(new ImmutableArray<>());
        this.name = name;
        this.returnParameters = new ImmutableArray<>(returnParameters);
        this.inputParameters = new ImmutableArray<>(inputParameters);
        this.body = body;
        this.kind = kind;
        this.visibility = visibility;
        this.stateMutability = stateMutability;
        this.modifiers = new ImmutableArray<>(modifiers);
    }

    public FunctionDeclaration(ExtList children, Name name, String kind, Visibility visibility, StateMutability stM) {
        super(children.removeFirstOccurrence(ImmutableArray.class));
        this.name = name;
        this.returnParameters = getFromClass(children);
        this.inputParameters = getFromClass(children);
        this.body = Objects.requireNonNull(children.removeFirstOccurrence(Block.class));
        this.kind = kind;
        this.visibility = visibility;
        this.stateMutability = stM;
        this.modifiers = getFromClass(children);
    }

    public <T> ImmutableArray<T> getFromClass(ExtList ext) {
        T el = (T) ext.removeFirstOccurrence(List.class);
        if(el == null)
            return new ImmutableArray<>();
        return new ImmutableArray<>(el);
    }

    public Block getBody() {
        return body;
    }

    public ImmutableArray<ParameterDeclaration> getReturnParameters() {
        return returnParameters;
    }

    public ImmutableArray<ParameterDeclaration> getInputParameters() {
        return inputParameters;
    }

    // Interface SolidityProgramElement

    @Override
    public SyntaxElement getChild(int n) {
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
        strBuffer.append("function ");
        strBuffer.append(name)
                .append(" (")
                .append(inputParameters.stream().map(ParameterDeclaration::toString)
                        .collect(Collectors.joining(", ")))
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

    public void visit(Visitor v) {
        v.performActionOnFunctionDeclaration(this);
    }
}
