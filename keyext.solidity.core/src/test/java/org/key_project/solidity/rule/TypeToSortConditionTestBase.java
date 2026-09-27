/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.parser.ParserForTesting;
import org.key_project.solidity.rule.matching.inst.MatchConditions;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.rule.sv.SchemaVariableFactory;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class TypeToSortConditionTestBase {
    protected Services services;
    protected Sort intSort;
    protected Sort boolSort;
    protected Sort structSort;
    protected Sort identitySort;
    protected ProgramSV receiverSV;
    protected GenericSort alpha;

    @BeforeEach
    void setUpSortsAndSchemaVariables() {
        services = ParserForTesting.load().getServices();
        intSort = services.getTheoryInfo().getIntLDT().targetSort();
        boolSort = services.getTheoryInfo().getBoolLDT().targetSort();
        structSort = services.getTheoryInfo().getStructLDT().targetSort();
        identitySort = services.getTheoryInfo().getMemoryLDT().getIdentitySort();
        receiverSV = SchemaVariableFactory.createProgramSV(new Name("sp"),
            ProgramSVSort.SIMPLE_STORAGE_PATH, false);
        alpha = new GenericSort(new Name("alpha"));
    }

    protected void assertSort(@Nullable MatchConditions result, Sort expected) {
        assertNotNull(result);
        assertEquals(expected, result.getInstantiations().getGenericSortInstantiations()
                .getInstantiation(alpha));
    }
}
