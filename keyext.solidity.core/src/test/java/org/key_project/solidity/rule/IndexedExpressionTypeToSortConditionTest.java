/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.DynamicArraySort;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.parser.varcond.IndexedExpressionTypeToSortCondition;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.rule.matching.inst.MatchConditions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.BOOL;

public class IndexedExpressionTypeToSortConditionTest extends TypeToSortConditionTestBase {
    private IndexedExpressionTypeToSortCondition condition;
    private IndexedExpressionTypeToSortCondition memoryCondition;

    @BeforeEach
    void setUp() {
        condition = new IndexedExpressionTypeToSortCondition(receiverSV, alpha);
        memoryCondition = new IndexedExpressionTypeToSortCondition(receiverSV, alpha, true);
    }

    @Test
    void bindsMappingValueSort() {
        ProgramVariable flags = new ProgramVariable(new Name("flags"),
            new KeYSolidityType(new MappingType(PrimitiveType.UINT256, BOOL),
                new SortImpl(new Name("mapping(uint256 => bool)"), false)),
            DataLocation.Storage);

        MatchConditions result = check(flags);

        assertSort(result, boolSort);
    }

    @Test
    void bindsArrayElementSort() {
        ProgramVariable flags = new ProgramVariable(new Name("flags"),
            new KeYSolidityType(new DynamicArrayType(BOOL),
                new DynamicArraySort(boolSort)),
            DataLocation.Storage);

        MatchConditions result = check(flags);

        assertSort(result, boolSort);
    }

    @Test
    void bindsMemoryArrayReferenceElementSortToIdentity() {
        StructDeclaration struct = new StructDeclaration(new Name("Payload"), List.of(), -1);
        services.getSolidityInfo().put(new KeYSolidityType(struct, structSort));
        ProgramVariable payloads = new ProgramVariable(new Name("payloads"),
            new KeYSolidityType(new DynamicArrayType(struct), new DynamicArraySort(structSort)),
            DataLocation.Memory);

        MatchConditions result = checkMemory(payloads);

        assertSort(result, identitySort);
    }

    @Test
    void keepsPrimitiveMemoryArrayElementSort() {
        ProgramVariable flags = new ProgramVariable(new Name("flags"),
            new KeYSolidityType(new DynamicArrayType(BOOL),
                new DynamicArraySort(boolSort)),
            DataLocation.Memory);

        MatchConditions result = checkMemory(flags);

        assertSort(result, boolSort);
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

    private MatchConditions checkMemory(ProgramVariable receiver) {
        return (MatchConditions) memoryCondition.check(receiverSV, receiver,
            MatchConditions.EMPTY_MATCHCONDITIONS, services);
    }
}
