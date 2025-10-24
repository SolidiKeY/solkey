/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import org.key_project.logic.Name;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.SolidityDLTheory;
import org.key_project.solidity.logic.op.SFunction;
import org.key_project.util.collection.ImmutableArray;

import org.junit.jupiter.api.Test;

class ParsingFacadeTest {

    private Services services = new Services();

    @Test
    void parseExpression() {
        services.getNamespaces().functions()
                .add(new SFunction(new Name("A"), SolidityDLTheory.FORMULA, new ImmutableArray<>(),
                    true));
        services.getNamespaces().functions()
                .add(new SFunction(new Name("B"), SolidityDLTheory.FORMULA, new ImmutableArray<>(),
                    true));
        KeYIO io = new KeYIO(services);
        io.parseExpression("A & B");
    }
}
