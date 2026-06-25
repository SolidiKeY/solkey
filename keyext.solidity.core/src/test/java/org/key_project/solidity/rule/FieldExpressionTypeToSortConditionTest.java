/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.rule;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.GenericSort;
import org.key_project.solidity.parser.ParserForTesting;
import org.key_project.solidity.parser.varcond.FieldExpressionTypeToSortCondition;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.program.ast.references.TypeReference;
import org.key_project.solidity.rule.matching.inst.MatchConditions;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.rule.sv.SchemaVariableFactory;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class FieldExpressionTypeToSortConditionTest {
    private Services services;
    private Sort intSort;
    private Sort boolSort;
    private Sort structSort;
    private ProgramSV fieldSV;
    private ProgramSV receiverSV;
    private GenericSort alpha;
    private FieldExpressionTypeToSortCondition condition;

    @BeforeEach
    void setUp() {
        services = ParserForTesting.load().getServices();
        intSort = services.getTheoryInfo().getIntLDT().targetSort();
        boolSort = services.getTheoryInfo().getBoolLDT().targetSort();
        structSort = services.getTheoryInfo().getStructLDT().targetSort();
        fieldSV = SchemaVariableFactory.createProgramSV(new Name("a"), ProgramSVSort.FIELD,
            false);
        receiverSV = SchemaVariableFactory.createProgramSV(new Name("sp"),
            ProgramSVSort.SIMPLE_STORAGE_PATH, false);
        alpha = new GenericSort(new Name("alpha"));
        condition = new FieldExpressionTypeToSortCondition(fieldSV, alpha);
    }

    @Test
    void bindsPrimitiveIntFieldSort() {
        MatchConditions result = check(field("age", "uint"));

        assertSort(result, intSort);
    }

    @Test
    void bindsPrimitiveBoolFieldSort() {
        MatchConditions result = check(field("enabled", "bool"));

        assertSort(result, boolSort);
    }

    @Test
    void bindsStructFieldSort() {
        StructDeclaration struct = new StructDeclaration(new Name("Payload"), List.of(), -1);
        services.getSolidityInfo().put(new KeYSolidityType(struct, structSort));

        MatchConditions result =
            check(new FieldDeclaration(new Name("payload"), new TypeReference(struct)));

        assertSort(result, structSort);
    }

    @Test
    void rejectsNonFieldInstantiation() {
        ProgramVariable total = new ProgramVariable(new Name("total"),
            new KeYSolidityType(PrimitiveType.UINT256, intSort), DataLocation.Storage);

        assertNull(condition.check(fieldSV, total, MatchConditions.EMPTY_MATCHCONDITIONS,
            services));
    }

    @Test
    void ignoresReceiverSchemaVariable() {
        ProgramVariable receiver = new ProgramVariable(new Name("sp"),
            new KeYSolidityType(PrimitiveType.UINT256, intSort), DataLocation.Storage);

        assertSame(MatchConditions.EMPTY_MATCHCONDITIONS,
            condition.check(receiverSV, receiver, MatchConditions.EMPTY_MATCHCONDITIONS,
                services));
    }

    private FieldDeclaration field(String name, String typeName) {
        return new FieldDeclaration(new Name(name), new TypeReference(new Name(typeName)));
    }

    private MatchConditions check(FieldDeclaration field) {
        return (MatchConditions) condition.check(fieldSV, field,
            MatchConditions.EMPTY_MATCHCONDITIONS, services);
    }

    private void assertSort(MatchConditions result, Sort expected) {
        assertNotNull(result);
        assertEquals(expected, result.getInstantiations().getGenericSortInstantiations()
                .getInstantiation(alpha));
    }
}
