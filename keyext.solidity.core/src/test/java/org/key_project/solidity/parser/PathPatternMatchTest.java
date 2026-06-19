/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.parser;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.SourceData;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.references.TypeReference;
import org.key_project.solidity.rule.matching.inst.MatchConditions;
import org.key_project.solidity.rule.sv.ProgramSV;
import org.key_project.solidity.rule.sv.SchemaVariableFactory;
import org.key_project.solidity.rule.sv.sort.ProgramSVSort;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class PathPatternMatchTest {

    private final Services services = new Services();
    private final KeYSolidityType uintType =
        new KeYSolidityType(PrimitiveType.UINT256, new SortImpl(new Name("uint256"), false));
    private final KeYSolidityType accountType = structType("Account", field("balance"));
    private final KeYSolidityType personType = structType("Person", field("account"));

    @Test
    void memberPatternBindsBasePathAndChecksLiteralFieldName() {
        ProgramSV base = SchemaVariableFactory.createProgramSV(new Name("a"),
            ProgramSVSort.STORAGE_PATH, false, accountType);
        MemberExp pattern = new MemberExp(base, field("balance"), uintType);

        ProgramVariable alice = new ProgramVariable(new Name("alice"), personType,
            DataLocation.Storage);
        MemberExp account = new MemberExp(alice, field("account"), accountType);
        MemberExp balance = new MemberExp(account, field("balance"), uintType);
        MemberExp age = new MemberExp(account, field("age"), uintType);

        MatchConditions result = pattern.match(new SourceData(balance, -1, services),
            MatchConditions.EMPTY_MATCHCONDITIONS);

        assertSame(account, result.getInstantiations().getInstantiation(base));
        assertNull(pattern.match(new SourceData(age, -1, services),
            MatchConditions.EMPTY_MATCHCONDITIONS));
    }

    private KeYSolidityType structType(String name, FieldDeclaration... fields) {
        Sort sort = new SortImpl(new Name(name + "Sort"), false);
        Type type = new StructDeclaration(new Name(name), List.of(fields), -1);
        return new KeYSolidityType(type, sort);
    }

    private static FieldDeclaration field(String name) {
        return new FieldDeclaration(new Name(name), new TypeReference(new Name("uint256")));
    }
}
