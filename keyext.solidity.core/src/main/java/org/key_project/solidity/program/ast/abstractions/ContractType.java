package org.key_project.solidity.program.ast.abstractions;

import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.SyntaxElementCursor;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.program.ast.declarations.ContractDeclaration;
import org.key_project.solidity.program.ast.declarations.Declaration;
import org.key_project.util.collection.ImmutableArray;

public class ContractType extends Declaration implements Type {
    private final ContractDeclaration contract;

    public ContractType(ContractDeclaration contract) {
        super(new ImmutableArray<>());
        this.contract = contract;
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        return contract.getSort(services);
    }

    @Override
    public Name name() {
        return contract.name();
    }

    @Override
    public SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Contract type has no children");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public String toString() {
        return contract.name().toString();
    }

    @Override
    public SyntaxElementCursor getCursor() {
        return super.getCursor();
    }
}
