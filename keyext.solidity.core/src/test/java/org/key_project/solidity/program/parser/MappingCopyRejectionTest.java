/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.program.parser;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.logic.sort.SortImpl;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.PrimitiveType;
import org.key_project.solidity.program.ast.declarations.FieldDeclaration;
import org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation;
import org.key_project.solidity.program.ast.declarations.StructDeclaration;
import org.key_project.solidity.program.ast.references.TypeReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MappingCopyRejectionTest {

    private static final StructDeclaration LEDGER = new StructDeclaration(new Name("Ledger"),
        List.of(new FieldDeclaration(new Name("balances"),
            new TypeReference(new MappingType(PrimitiveType.UINT, PrimitiveType.UINT)))),
        -1);

    private static final StructDeclaration TOKEN = new StructDeclaration(new Name("Token"),
        List.of(new FieldDeclaration(new Name("value"), new TypeReference(PrimitiveType.UINT))),
        -1);

    private static ProgramVariable variable(String name, StructDeclaration type,
            DataLocation location) {
        return new ProgramVariable(new Name(name),
            new KeYSolidityType(type, new SortImpl(new Name("Struct"))), location);
    }

    @Test
    void mappingCarryingAssignmentTargetIsRejected() {
        ProgramVariable target = variable("l1", LEDGER, DataLocation.Memory);
        ProgramVariable source = variable("l2", LEDGER, DataLocation.Memory);
        assertThrows(SolidityParseException.class,
            () -> ParserUtils.parseAssignment(target, source, "="));
    }

    @Test
    void storagePointerRebindIsAllowed() {
        ProgramVariable target = variable("l1", LEDGER, DataLocation.Storage);
        ProgramVariable source = variable("l2", LEDGER, DataLocation.Storage);
        assertDoesNotThrow(() -> ParserUtils.parseAssignment(target, source, "="));
    }

    @Test
    void mapFreeAssignmentIsAllowed() {
        ProgramVariable target = variable("t1", TOKEN, DataLocation.Memory);
        ProgramVariable source = variable("t2", TOKEN, DataLocation.Memory);
        assertDoesNotThrow(() -> ParserUtils.parseAssignment(target, source, "="));
    }

    @Test
    void compoundAssignmentIsNotAffected() {
        ProgramVariable target = variable("l1", LEDGER, DataLocation.Memory);
        ProgramVariable source = variable("l2", LEDGER, DataLocation.Memory);
        assertDoesNotThrow(() -> ParserUtils.parseAssignmentMaybe(target, source, "+="));
    }
}
