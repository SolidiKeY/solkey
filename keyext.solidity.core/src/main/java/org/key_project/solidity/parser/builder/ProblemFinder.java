package org.key_project.solidity.parser.builder;

import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.NamespaceSet;

public class ProblemFinder extends ExpressionBuilder {
    public ProblemFinder(Services services, NamespaceSet nss) {
        super(services, nss);
    }
}
