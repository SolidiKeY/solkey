/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.DynamicArraySort;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.parser.ParserForTesting;
import org.key_project.solidity.parser.varcond.IndexedExpressionTypeToSortCondition;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.rule.matching.inst.MatchConditions;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.rule.sv.SchemaVariableFactory;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IndexedExpressionTypeToSortConditionTest {
    private Services services;
    private Sort boolSort;
    private Sort intSort;
    private ProgramSV receiverSV;
    private GenericSort alpha;
    private IndexedExpressionTypeToSortCondition condition;

    @BeforeEach
    void setUp() {
        services = ParserForTesting.load().getServices();
        boolSort = services.getTheoryInfo().getBoolLDT().targetSort();
        intSort = services.getTheoryInfo().getIntLDT().targetSort();
        receiverSV = SchemaVariableFactory.createProgramSV(new Name("sp"),
            ProgramSVSort.SIMPLE_STORAGE_PATH, false);
        alpha = new GenericSort(new Name("alpha"));
        condition = new IndexedExpressionTypeToSortCondition(receiverSV, alpha);
    }

    @Test
    void bindsMappingValueSort() {
        ProgramVariable flags = new ProgramVariable(new Name("flags"),
            new KeYSolidityType(new MappingType(PrimitiveType.UINT256, PrimitiveType.BOOL),
                new SortImpl(new Name("mapping(uint256 => bool)"), false)),
            DataLocation.Storage);

        MatchConditions result = check(flags);

        assertNotNull(result);
        assertEquals(boolSort, result.getInstantiations().getGenericSortInstantiations()
                .getInstantiation(alpha));
    }

    @Test
    void bindsArrayElementSort() {
        ProgramVariable flags = new ProgramVariable(new Name("flags"),
            new KeYSolidityType(new DynamicArrayType(PrimitiveType.BOOL),
                new DynamicArraySort(boolSort)),
            DataLocation.Storage);

        MatchConditions result = check(flags);

        assertNotNull(result);
        assertEquals(boolSort, result.getInstantiations().getGenericSortInstantiations()
                .getInstantiation(alpha));
    }

    @Test
    void rejectsNonIndexedReceiverType() {
        ProgramVariable total = new ProgramVariable(new Name("total"),
            new KeYSolidityType(PrimitiveType.UINT256, intSort), DataLocation.Storage);

        assertNull(check(total));
    }

    private MatchConditions check(ProgramVariable receiver) {
        return (MatchConditions) condition.check(receiverSV, receiver,
            MatchConditions.EMPTY_MATCHCONDITIONS, services);
    }
}
