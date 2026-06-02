/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.SolidityInfo;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.KeYSolidityType;
import org.key_project.solidity.program.ast.abstractions.MappingType;
import org.key_project.solidity.program.ast.abstractions.TupleType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.IndexExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.expressions.NewExpression;
import org.key_project.solidity.program.ast.expressions.TupleExpression;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.ContractReference;
import org.key_project.solidity.program.ast.references.FunctionReference;
import org.key_project.solidity.program.ast.references.ModifierReference;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.program.parser.SolcParser;
import org.key_project.util.collection.ImmutableArray;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;
import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.*;
import static org.key_project.solidity.program.ast.declarations.FunctionEnums.DataLocation.*;
import static org.key_project.solidity.program.ast.declarations.FunctionEnums.StateMutability.*;
import static org.key_project.solidity.program.ast.declarations.FunctionEnums.Visibility.*;
import static org.key_project.solidity.program.ast.expressions.literals.BoolLiteral.*;
import static org.key_project.solidity.program.parser.SolcParserNoServices.getDeclStr;


public class SolJsonParserTest {

    @Test
    void parse() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(1, contractDeclaration.getFieldDeclarations().size());
        assertEquals(Storage,
            contractDeclaration.getFieldDeclarations().get(0).getProgramVariable()
                    .getDataLocation());
        assertSame(UINT256,
            contractDeclaration.getFieldDeclarations().get(0).getProgramVariable().getType());
        StateVariableDeclaration balanceDecl = contractDeclaration.getFieldDeclarations().get(0);
        assertEquals(1, balanceDecl.getChildCount());
        assertInstanceOf(ProgramVariable.class, balanceDecl.getChild(0));
        assertThrows(IndexOutOfBoundsException.class, () -> balanceDecl.getChild(1));
        assertTrue(balanceDecl.toString().contains("balance"));
        assertEquals("SimpleContract", contractDeclaration.name().toString());
    }

    @Test
    void parseContractWithIntAndBool() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance;
                   bool closed;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(2, contractDeclaration.getFieldDeclarations().size());
        assertSame(UINT256,
            contractDeclaration.getFieldDeclarations().get(0).getProgramVariable().getType());
        assertSame(BOOL,
            contractDeclaration.getFieldDeclarations().get(1).getProgramVariable().getType());
    }

    @Test
    void initializedSolidityInfoRegistersOnlySupportedPrimitiveTypes() {
        Services services = new Services();
        SolidityInfo info = services.getSolidityInfo();
        Sort intSort = services.getTheoryInfo().getIntLDT().targetSort();
        Sort boolSort = services.getTheoryInfo().getBoolLDT().targetSort();

        assertCompleteType(info.getKeYSolidityType(UINT256), UINT256, intSort);
        assertCompleteType(info.getKeYSolidityType(INT8), INT8, intSort);
        assertCompleteType(info.getKeYSolidityType(BOOL), BOOL, boolSort);
        assertNull(info.getKeYSolidityType(ADDRESS));
        assertNull(info.getKeYSolidityType(BYTES32));
    }


    private static void assertCompleteType(KeYSolidityType actual, Type expectedType,
            Sort expectedSort) {
        assertNotNull(actual);
        assertSame(expectedType, actual.getSolidityType());
        assertSame(expectedSort, actual.getSort());
    }

    @Test
    void parseContractWithIntAndBoolSet() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance = 1000;
                   bool closed = true;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(2, contractDeclaration.getFieldDeclarations().size());
        StateVariableDeclaration firstField = contractDeclaration.getFieldDeclarations().get(0);
        assertInstanceOf(Uint256Literal.class, firstField.getInitializer());
        assertEquals(1000,
            ((Uint256Literal) firstField.getInitializer()).getValue().longValue());
        StateVariableDeclaration secondField = contractDeclaration.getFieldDeclarations().get(1);
        assertInstanceOf(BoolLiteral.class, secondField.getInitializer());
        assertSame(TRUE, secondField.getInitializer());
        assertSame(UINT256, firstField.getProgramVariable().getType());
        assertSame(BOOL, secondField.getProgramVariable().getType());
        assertEquals(2, firstField.getChildCount());
        assertInstanceOf(ProgramVariable.class, firstField.getChild(0));
        assertInstanceOf(Uint256Literal.class, firstField.getChild(1));
        assertThrows(IndexOutOfBoundsException.class, () -> firstField.getChild(2));
    }

    @Test
    void parseContractWithAddition() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance = 1000;
                   uint256 deposit = 5 + 100;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(2, contractDeclaration.getFieldDeclarations().size());
    }

    @Test
    void parseContractWithReferenceAddition() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance = 1000;
                   uint256 deposit = balance + 100;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(2, contractDeclaration.getFieldDeclarations().size());
        Expression initializer = contractDeclaration.getFieldDeclarations().get(1).getInitializer();
        assertInstanceOf(AddOperator.class, initializer);
        AddOperator addOp = (AddOperator) initializer;
        assertInstanceOf(ProgramVariable.class, addOp.getLeft());
        assertInstanceOf(Uint256Literal.class, addOp.getRight());
        assertSame(UINT256, initializer.getType());
        assertEquals(2, addOp.getChildCount());
        assertSame(addOp.getLeft(), addOp.getChild(0));
        assertSame(addOp.getRight(), addOp.getChild(1));
        assertThrows(IndexOutOfBoundsException.class, () -> addOp.getChild(2));
    }

    @Test
    void parseContractWithBoth() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance = 1000;
                   SimpleContract other;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(2, contractDeclaration.getFieldDeclarations().size());
    }

    @Test
    void parseFunction() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   function func() public pure {
                   }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(1, contractDeclaration.getFunctions().size());
        FunctionDeclaration functionDeclaration = contractDeclaration.getFunctions().getFirst();
        Block block = functionDeclaration.getBody();
        assertTrue(block.isEmpty());
        assertEquals(0, block.getStatements().size());
        assertTrue(
            functionDeclaration.toString().contains("function func () public pure"));
        assertEquals(1, functionDeclaration.getChildCount());
        assertInstanceOf(Block.class, functionDeclaration.getChild(0));
        assertThrows(IndexOutOfBoundsException.class, () -> functionDeclaration.getChild(1));
        assertEquals(0, block.getChildCount());
        assertThrows(IndexOutOfBoundsException.class, () -> block.getChild(0));
        assertSame(Public, functionDeclaration.getVisibility());
        assertSame(pure, functionDeclaration.getStateMutability());
        assertEquals("func", functionDeclaration.name().toString());
        assertEquals(0, functionDeclaration.getReturnParameters().size());
        assertSame(VOID, functionDeclaration.getType());
    }

    @Test
    void parseComplexFunction() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function func(uint256 v) public pure returns(uint256) {
                       return v;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(1, contractDeclaration.getFunctions().size());
        FunctionDeclaration function = contractDeclaration.getFunctions().getFirst();
        assertEquals(1, function.getInputParameters().size());
        assertEquals(1, function.getReturnParameters().size());
        Block block = function.getBody();
        assertFalse(block.isEmpty());
        assertEquals(1, block.getStatements().size());
        assertEquals(Default, function.getInputParameters().get(0).getDataLocation());
        assertSame(UINT256, function.getInputParameters().get(0).getType());
        assertSame(UINT256, function.getReturnParameters().get(0).getType());
        assertEquals(3, function.getChildCount());
        assertInstanceOf(ProgramVariable.class, function.getChild(0)); // return param
        assertInstanceOf(ProgramVariable.class, function.getChild(1)); // input param
        assertInstanceOf(Block.class, function.getChild(2)); // body
        assertThrows(IndexOutOfBoundsException.class, () -> function.getChild(3));
        assertEquals(1, block.getChildCount());
        assertInstanceOf(ReturnStatement.class, block.getChild(0));
        assertThrows(IndexOutOfBoundsException.class, () -> block.getChild(1));
        ReturnStatement retStmt = (ReturnStatement) block.getStatements().get(0);
        assertEquals(1, retStmt.getChildCount());
        assertInstanceOf(ProgramVariable.class, retStmt.getChild(0));
        assertThrows(IndexOutOfBoundsException.class, () -> retStmt.getChild(1));
        assertSame(Public, function.getVisibility());
        assertSame(pure, function.getStateMutability());
    }

    @Test
    void parseSimpleAssignment() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   function func(uint256 v) public pure  {
                      v = 4;
                   }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        String s = contractDeclaration.toString();
        assertEquals(1, contractDeclaration.getFunctions().size());
        FunctionDeclaration function = contractDeclaration.getFunctions().getFirst();
        Block block = function.getBody();
        assertFalse(block.isEmpty());
        assertEquals(1, block.getStatements().size());
        Statement exprStmnt = block.getStatements().get(0);
        assertInstanceOf(ExpressionStatement.class, exprStmnt);
        assertInstanceOf(AssignmentExpression.class,
            ((ExpressionStatement) exprStmnt).getExpression());
        ExpressionStatement exprStatement = (ExpressionStatement) exprStmnt;
        assertEquals(1, exprStatement.getChildCount());
        assertInstanceOf(AssignmentExpression.class, exprStatement.getChild(0));
        assertThrows(IndexOutOfBoundsException.class, () -> exprStatement.getChild(1));
    }

    @Test
    void variableDeclaration() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   function func() public pure {
                      int256 v;
                   }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        assertTrue(contractDec.toString().contains("int256 v"));
        DeclarationStatement ds = (DeclarationStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        ProgramVariable v =
            ((StatementVariableDeclaration) ds.getDeclarations().get(0)).getProgramVariable();
        assertSame(Default, v.getDataLocation());
        assertSame(INT256, v.getType());
        assertEquals(1, ds.getChildCount());
        assertInstanceOf(StatementVariableDeclaration.class, ds.getChild(0));
        assertThrows(IndexOutOfBoundsException.class, () -> ds.getChild(1));
    }

    @Test
    void variableDeclarationAssigned() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   function func() public pure {
                      bool b;
                      b = true;
                   }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        assertTrue(contractDec.toString().contains("b = true"));
        DeclarationStatement ds = (DeclarationStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        ProgramVariable b =
            ((StatementVariableDeclaration) ds.getDeclarations().get(0)).getProgramVariable();
        assertSame(BOOL, b.getType());
    }

    @Test
    void variableDeclarationAssignedSameStatement() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   function func() public pure {
                      bool b = true;
                      bool c = true || false;
                   }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String s = contractDec.toString();
        assertTrue(s.contains("bool b = true"));
        assertTrue(s.contains("bool c = true || false"));
        DeclarationStatement ds1 = (DeclarationStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        assertEquals(2, ds1.getChildCount());
        assertInstanceOf(StatementVariableDeclaration.class, ds1.getChild(0));
        assertSame(TRUE, ds1.getChild(1));
        assertThrows(IndexOutOfBoundsException.class, () -> ds1.getChild(2));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void parseContractWithOperations() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 deposit = 5 ^ 5 + 100 % 4 - 1 * 3 / 3;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(1, contractDeclaration.getFieldDeclarations().size());
        Expression expOpSynt = contractDeclaration.getFieldDeclarations().get(0).getInitializer();
        assertInstanceOf(ExponentialOperator.class, expOpSynt);
        ExponentialOperator expOp = (ExponentialOperator) expOpSynt;
        assertEquals(INT256, expOp.getType());
    }

    @Test
    void parseContractWithManyOperations() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   bool v = true && true || false;
                   int w = ~0;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(2, contractDeclaration.getFieldDeclarations().size());
        assertInstanceOf(OrOperator.class,
            contractDeclaration.getFieldDeclarations().get(0).getInitializer());
        assertSame(BOOL,
            contractDeclaration.getFieldDeclarations().get(0).getProgramVariable().getType());
        assertSame(INT,
            contractDeclaration.getFieldDeclarations().get(1).getProgramVariable().getType());
    }

    @Test
    void parseIf() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   bool v = true ? false : true;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(1, contractDeclaration.getFieldDeclarations().size());
        assertInstanceOf(TernaryOperator.class,
            contractDeclaration.getFieldDeclarations().get(0).getInitializer());
        assertSame(BOOL,
            contractDeclaration.getFieldDeclarations().get(0).getProgramVariable().getType());
        assertSame(BOOL,
            contractDeclaration.getFieldDeclarations().get(0).getInitializer().getType());
        TernaryOperator ternary =
            (TernaryOperator) contractDeclaration.getFieldDeclarations().get(0).getInitializer();
        assertEquals(3, ternary.getChildCount());
        assertSame(TRUE, ternary.getChild(0)); // condition = true
        assertSame(TRUE, ternary.getChild(1)); // falseExpression = true (value after :)
        assertSame(FALSE, ternary.getChild(2)); // trueExpression = false (value after ?)
        assertThrows(IndexOutOfBoundsException.class, () -> ternary.getChild(3));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void parseContractWithBoolIntOperations() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   bool v = 1 != 0 && 1 == 1 && 0 < 0 && 0 <= 0 && 0 > 0 && 0 > 0;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(1, contractDeclaration.getFieldDeclarations().size());
        assertInstanceOf(AndOperator.class,
            contractDeclaration.getFieldDeclarations().get(0).getInitializer());
    }

    @Test
    void parseUnaryOperations() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 i;
                   uint256 j;
                   uint256 v = i++ + j--;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        ImmutableArray<StateVariableDeclaration> fieldDeclarations =
            contractDeclaration.getFieldDeclarations();
        assertEquals(3, fieldDeclarations.size());
        BinaryOperator addOp =
            (BinaryOperator) contractDeclaration.getFieldDeclarations().get(2).getInitializer();
        SyntaxElement exp = addOp.getLeft();
        assertInstanceOf(PlusPlusOperator.class, exp);
        assertSame(UINT256, fieldDeclarations.get(0).getProgramVariable().getType());
        assertSame(UINT256, fieldDeclarations.get(1).getProgramVariable().getType());
        assertSame(UINT256, fieldDeclarations.get(2).getProgramVariable().getType());
        PlusPlusOperator ppOp = (PlusPlusOperator) exp;
        assertEquals(1, ppOp.getChildCount());
        assertInstanceOf(ProgramVariable.class, ppOp.getChild(0));
        assertThrows(IndexOutOfBoundsException.class, () -> ppOp.getChild(1));
    }

    @Test
    void parseComplexAssignment() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function func(uint256 u, uint256 v, uint256 w) public pure  {
                        v += w = u -= 1;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        assertEquals(1, contractDeclaration.getFunctions().size());
        assertInstanceOf(PlusEqualOperator.class,
            ((ExpressionStatement) contractDeclaration.getFunctions()
                    .getFirst().getBody().getStatements().get(0)).getExpression());
        FunctionDeclaration func = contractDeclaration.getFunctions().getFirst();
        ImmutableArray<ProgramVariable> inputParameters = func.getInputParameters();
        assertSame(UINT256, inputParameters.get(0).getType());
        assertSame(UINT256, inputParameters.get(1).getType());
        assertSame(UINT256, inputParameters.get(2).getType());
    }

    @Test
    @Disabled("Parsing type is not working")
    void parseDictInFunction() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f(bool a, bool b) public pure  {
                        f({b : true, a : false});
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
    }

    @Test
    void parseMemoryParameter() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    struct Person {
                       int age;
                    }
                    function func(Person memory p) public pure  {
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        String structName = contractDeclaration.getStructs().get(0).name().toString();
        assertEquals("SimpleContract.Person", structName);
        assertEquals(Memory,
            contractDeclaration.getFunctions().get(0).getInputParameters().get(0)
                    .getDataLocation());
    }

    @Test
    void parseStruct() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    struct Person {
                       int age;
                    }
                    Person alice;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        List<StructDeclaration> structs = contractDeclaration.getStructs();
        assertEquals(1, structs.size());
        StructDeclaration struct = structs.getFirst();
        assertEquals(1, struct.getFields().size());
        assertEquals(Storage,
            contractDeclaration.getFieldDeclarations().get(0).getProgramVariable()
                    .getDataLocation());
        assertEquals(1, struct.getChildCount());
        assertInstanceOf(FieldDeclaration.class, struct.getChild(0));
        FieldDeclaration field = struct.getFields().get(0);
        assertEquals("int", field.getTypeReference().getTypeName().toString());
        assertNull(field.getInitializer());
        assertEquals(1, field.getChildCount());
    }

    @Test
    void parseUsingStruct() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    struct Person {
                       uint256 age;
                    }
                    Person alice;
                    function f() public returns (uint256) {
                        return alice.age;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        List<StructDeclaration> structs = contractDeclaration.getStructs();
        assertEquals(1, structs.size());
        assertEquals(1, structs.getFirst().getFields().size());
        FunctionDeclaration function = contractDeclaration.getFunctions().getFirst();
        var retStmSynt = function.getBody().getStatements().get(0);
        assertInstanceOf(ReturnStatement.class, retStmSynt);
        ReturnStatement retStm = (ReturnStatement) retStmSynt;
        Expression retExp = retStm.getReturnExp();
        assertInstanceOf(MemberExp.class, retExp);
        MemberExp memberExp = (MemberExp) retExp;
        assertEquals(2, memberExp.getChildCount());
        assertInstanceOf(ProgramVariable.class, memberExp.getLeftExp()); // alice
        assertInstanceOf(FieldDeclaration.class, memberExp.getRightExp());
        assertThrows(IndexOutOfBoundsException.class, () -> memberExp.getChild(2));
    }

    @Test
    void parseMemory() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    struct Person {
                       uint256 age;
                    }
                    function f() public pure {
                        Person memory alice;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        var declStmS =
            contractDeclaration.getFunctions().getFirst().getBody().getStatements().get(0);
        assertInstanceOf(DeclarationStatement.class, declStmS);
        DeclarationStatement declStms = (DeclarationStatement) declStmS;
        StatementVariableDeclaration decl =
            (StatementVariableDeclaration) declStms.getDeclarations().get(0);
        assertEquals("alice", decl.getProgramVariable().name().toString());
        String contractStr = contractDeclaration.toString();
        assertTrue(contractStr.contains("Person memory alice"));
        assertEquals(Memory, decl.getProgramVariable().getDataLocation());
    }

    @Test
    void parseStoragePointer() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    struct Person {
                       uint256 age;
                    }
                    Person alice;
                    function f() public {
                        Person storage bob = alice;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        DeclarationStatement ds = (DeclarationStatement) contractDeclaration.getFunctions().get(0)
                .getBody().getStatements().get(0);
        ProgramVariable bob =
            ((StatementVariableDeclaration) ds.getDeclarations().get(0)).getProgramVariable();
        assertEquals("bob", bob.name().toString());
        assertEquals(Storage, bob.getDataLocation());
    }

    @Test
    void parseElementaryType() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    int[] v;
                    function f() public returns (int) {
                        return v[1+1];
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        assertTrue(contractDec.toString().contains("v[1 + 1]"));
        Type vType = contractDec.getFieldDeclarations().get(0).getProgramVariable().getType();
        assertInstanceOf(DynamicArrayType.class, vType);
        DynamicArrayType arrayType = (DynamicArrayType) vType;
        assertSame(INT, arrayType.getElementType());
        ReturnStatement retStmt = (ReturnStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        IndexExpression idxExp = (IndexExpression) retStmt.getReturnExp();
        assertEquals(2, idxExp.getChildCount());
        assertInstanceOf(ProgramVariable.class, idxExp.getChild(0)); // v
        assertInstanceOf(AddOperator.class, idxExp.getChild(1)); // 1+1
        assertThrows(IndexOutOfBoundsException.class, () -> idxExp.getChild(2));
    }

    @Test
    void parseInlineArray() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        bool[3] memory foo;
                        foo = [false, true, false];
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("bool[3] memory foo;"));
        assertTrue(contractS.contains("foo = [false, true, false];"));
        DeclarationStatement declStm = (DeclarationStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        StatementVariableDeclaration decl =
            (StatementVariableDeclaration) declStm.getDeclarations().get(0);
        ProgramVariable foo = decl.getProgramVariable();
        Type fooType = foo.getType();
        assertInstanceOf(ArrayType.class, fooType);
        ArrayType arrayType = (ArrayType) fooType;
        assertEquals(3, arrayType.length());
        assertSame(BOOL, arrayType.getElementType());
    }

    @Test
    void parseTempArray() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        bool[] memory foo = new bool[](3);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("bool[] memory foo"));
        DeclarationStatement declStm = (DeclarationStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        StatementVariableDeclaration decl =
            (StatementVariableDeclaration) declStm.getDeclarations().get(0);
        ProgramVariable foo = decl.getProgramVariable();
        Type fooType = foo.getType();
        assertInstanceOf(DynamicArrayType.class, fooType);
        DynamicArrayType arrayType = (DynamicArrayType) fooType;
        assertSame(BOOL, arrayType.getElementType());
    }

    @Test
    void parseSlice() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f(bool[] calldata v) public {
                        v[0:1];
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        assertTrue(contractDec.toString().contains("v[0:1];"));
        ProgramVariable v = contractDec.getFunctions().getFirst().getInputParameters().get(0);
        Type vType = v.getType();
        assertInstanceOf(DynamicArrayType.class, vType);
        DynamicArrayType arrayType = (DynamicArrayType) vType;
        assertSame(BOOL, arrayType.getElementType());
    }

    @Test
    void cast() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        bool(true);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        assertTrue(contractDec.toString().contains("bool(true);"));
    }

    @Test
    void functionCall() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f(bool v) public returns (bool) {
                        return v;
                    }
                    function g(bool v) public {
                        f(v);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        assertTrue(contractDec.toString().contains("f(v);"));
        FunctionDeclaration f = contractDec.getFunctions().get(0);
        assertSame(BOOL, f.getInputParameters().get(0).getType());
        assertSame(BOOL, f.getReturnParameters().get(0).getType());
        FunctionDeclaration g = contractDec.getFunctions().get(1);
        ExpressionStatement callStmt = (ExpressionStatement) g.getBody().getStatements().get(0);
        FunctionCallExpression fCall = (FunctionCallExpression) callStmt.getExpression();
        assertEquals(2, fCall.getChildCount()); // 1 arg + functionExp
        assertInstanceOf(ProgramVariable.class, fCall.getChild(0)); // v
        assertInstanceOf(FunctionReference.class, fCall.getChild(1)); // functionExp
        assertThrows(IndexOutOfBoundsException.class, () -> fCall.getChild(2));
        ImmutableArray<Expression> args = fCall.getArguments();
        assertEquals(1, args.size());
        assertSame(fCall.getArgument(0), args.get(0));
    }

    @Test
    void parseIfStatement() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    int i;
                    function f() public {
                        if(true) i = 0;
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("if"));
        assertTrue(contractS.contains("i = 0;"));
        assertSame(INT, contractDec.getFieldDeclarations().get(0).getProgramVariable().getType());
        ConditionStatement ifStmt = (ConditionStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        assertEquals(2, ifStmt.getChildCount());
        assertSame(TRUE, ifStmt.getChild(0));
        assertInstanceOf(ExpressionStatement.class, ifStmt.getChild(1));
        assertThrows(IndexOutOfBoundsException.class, () -> ifStmt.getChild(2));
    }

    @Test
    void parseIfElseStm() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    int i;
                    function f() public {
                        if(i == 2)
                            i = 0;
                        else
                            i = 1;
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("i == 2"));
        assertTrue(contractS.contains("if"));
        assertTrue(contractS.contains("i = 0;"));
        assertTrue(contractS.contains("else"));
        assertTrue(contractS.contains("i = 1;"));
        ConditionStatement ifElseStmt = (ConditionStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        assertEquals(3, ifElseStmt.getChildCount());
        assertInstanceOf(EqualOperator.class, ifElseStmt.getChild(0));
        assertInstanceOf(ExpressionStatement.class, ifElseStmt.getChild(1));
        assertInstanceOf(ExpressionStatement.class, ifElseStmt.getChild(2));
        assertThrows(IndexOutOfBoundsException.class, () -> ifElseStmt.getChild(3));
    }

    @Test
    void parseWhileStm() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    int i;
                    function f() public {
                        while(i == 0) i = 1;
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("while"));
        assertTrue(contractS.contains("i == 0"));
        assertTrue(contractS.contains("i = 1;"));
    }

    @Test
    void parseWhileStmYul() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        while(true) {
                            continue;
                            break;
                        }
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("while"));
        assertTrue(contractS.contains("continue;"));
        assertTrue(contractS.contains("break;"));
        WhileStatement whileStmt = (WhileStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        Block whileBody = (Block) whileStmt.getBody();
        ContinueStatement contStmt = (ContinueStatement) whileBody.getStatements().get(0);
        assertEquals(0, contStmt.getChildCount());
        assertThrows(IndexOutOfBoundsException.class, () -> contStmt.getChild(0));
        BreakStatement breakStmt = (BreakStatement) whileBody.getStatements().get(1);
        assertEquals(0, breakStmt.getChildCount());
        assertThrows(IndexOutOfBoundsException.class, () -> breakStmt.getChild(0));
    }

    @Test
    void parseFor() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    int i;
                    function f() public {
                        for(i = 0; i<10; i++){}
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("for(i = 0; i < 10; i ++)"));
        ForStatement forStmt = (ForStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        assertEquals(4, forStmt.getChildCount());
        assertThrows(IndexOutOfBoundsException.class, () -> forStmt.getChild(4));
        assertInstanceOf(AssignmentExpression.class, forStmt.getInit().getInit());
        assertEquals(1, forStmt.getInit().getChildCount());
        assertInstanceOf(PlusPlusOperator.class, forStmt.getUpdate().getUpdate());
        assertEquals(1, forStmt.getUpdate().getChildCount());
    }

    @Test
    void parseForEmpty() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        for(; ; ){}
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("for(; ; )"));
        ForStatement forStmt = (ForStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        assertEquals(1, forStmt.getChildCount());
        assertInstanceOf(Block.class, forStmt.getChild(0));
        assertThrows(IndexOutOfBoundsException.class, () -> forStmt.getChild(1));
    }

    @Test
    void parseDoWhile() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        do {} while (true);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("do"));
        assertTrue(contractS.contains("while (true)"));
    }

    @Test
    void externalContract() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() external pure {
                    }
                    function g(address target) public {
                        SimpleContract(target).f();
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("SimpleContract(target).f()"));
        FunctionDeclaration f = contractDec.getFunctions().get(0);
        assertSame(external, f.getVisibility());
        assertSame(pure, f.getStateMutability());
        FunctionDeclaration g = contractDec.getFunctions().get(1);
        assertSame(ADDRESS, g.getInputParameters().get(0).getType());
        assertSame(Public, g.getVisibility());
        assertSame(nonpayable, g.getStateMutability());
    }

    @Test
    void parseTryCatch() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f(address target) public {
                        try SimpleContract(target).g() { }
                        catch { }
                    }
                    function g() external pure {
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("SimpleContract(target).g()"));
        TryStatement tryStmt = (TryStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        MemberExp memberExp = (MemberExp) tryStmt.getExpression();
        FunctionCallExpression innerCall = (FunctionCallExpression) memberExp.getLeftExp();
        ContractReference contr = (ContractReference) innerCall.getFunctionExp();
        assertEquals(0, contr.getChildCount());
        assertEquals(3, tryStmt.getChildCount());
        assertSame(memberExp, tryStmt.getChild(0));
        assertInstanceOf(Block.class, tryStmt.getChild(1));
        assertThrows(IndexOutOfBoundsException.class, () -> tryStmt.getChild(3));
    }

    @Test
    void parseTryWithReturn() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f(address target) public {
                        try SimpleContract(target).g() returns (int a)
                        { int b = a;
                        }
                        catch { }
                    }
                    function g() external pure returns (int) {
                        return 0;
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        TryStatement tryStatement =
            (TryStatement) contractDec.getFunctions().get(0).getBody().getStatements().get(0);
        assertTrue(tryStatement.toString().contains("SimpleContract(target).g() returns (int a)"));
        ProgramVariable returnA = tryStatement.getReturnDeclaration().get(0);
        ProgramVariable rightA = (ProgramVariable) ((DeclarationStatement) tryStatement.getBody()
                .getStatements().get(0)).getInitialValue();
        assertSame(returnA, rightA);
        assertSame(INT, returnA.getType());
        assertEquals(4, tryStatement.getChildCount());
        assertSame(returnA, tryStatement.getChild(1));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void parseTryCatchMoreCases() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f(address target) public {
                        try SimpleContract(target).g() {
                            int i;
                        }
                        catch Error(string memory reason) {
                            int j;
                        }
                        catch {
                            int k;
                        }
                    }
                    function g() external pure {
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        TryStatement tryStmt = (TryStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        ImmutableArray<CatchClause> clauses = tryStmt.getCatchClauses();
        assertEquals(2, clauses.size());
        assertSame(tryStmt.getCatchClause(0), clauses.get(0));
        CatchClause errorClause = tryStmt.getCatchClause(0);
        assertEquals("Error", ((Object) errorClause.getKind()).toString());
        DeclarationStatement jDecl =
            (DeclarationStatement) errorClause.getBody().getStatements().get(0);
        assertEquals("int j;", jDecl.toString());
        StatementVariableDeclaration catchDecl = errorClause.getCatchDeclaration();
        assertEquals("string memory reason", catchDecl.toString());
        assertSame(STRING, catchDecl.getProgramVariable().getType());
        assertEquals("catch Error(string memory reason) {\nint j;\n}\n", errorClause.toString());
        CatchClause allClause = tryStmt.getCatchClause(1);
        assertEquals("ALL", ((Object) allClause.getKind()).toString());
        DeclarationStatement kDecl =
            (DeclarationStatement) allClause.getBody().getStatements().get(0);
        assertEquals("int k;", kDecl.toString());
    }

    @Disabled("Revert and require should be implemented as a regular function")
    @Test
    void parseRevertRequire() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f(address target) public {
                        revert("");
                        require(true);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
    }

    @Test
    void tupleReturn() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public returns (bool, bool) {
                        return (false, true);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("false, true"));
        TupleType type = (TupleType) contractDec.getFunctions().getFirst().getType();
        assertEquals(BOOL, type.getTypes().get(0));
    }

    @Test
    void multipleReturns() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public returns (bool, bool) {
                        return (false, true);
                    }
                    function g() public {
                        (bool a, bool b) = f();
                        (int c, int d) = (1, 2);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        DeclarationStatement decl = (DeclarationStatement) contractDec.getFunctions().get(1)
                .getBody().getStatements().get(0);
        assertEquals(2, decl.getDeclarations().size());
        assertEquals("(bool a, bool b) = f();", decl.toString());

        TupleType type = (TupleType) decl.getInitialValue().getType();
        assertEquals(2, type.getTypes().size());
        assertEquals(BOOL, type.getTypes().get(0));
        assertEquals(BOOL, type.getTypes().get(1));
        ReturnStatement retStmt0 = (ReturnStatement) contractDec.getFunctions().get(0)
                .getBody().getStatements().get(0);
        TupleExpression tupleExpr = (TupleExpression) retStmt0.getReturnExp();
        assertEquals(2, tupleExpr.getChildCount());
        assertSame(FALSE, tupleExpr.getChild(0));
        assertSame(TRUE, tupleExpr.getChild(1));
        assertThrows(IndexOutOfBoundsException.class, () -> tupleExpr.getChild(2));
    }

    @Test
    void constructor() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    constructor () {
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        FunctionDeclaration constructor = contractDec.getFunctions().stream()
                .filter(f -> f.getKind().equals("constructor"))
                .findFirst().orElse(null);
        assertNotNull(constructor);
        assertEquals(0, constructor.getInputParameters().size());
        assertTrue(constructor.getBody().isEmpty());
        assertEquals(0, constructor.getBody().getStatements().size());
    }

    @Test
    void mapping() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    mapping(bool => int256) public b;
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        Type bType = contractDec.getFieldDeclarations().get(0).getProgramVariable().getType();
        assertInstanceOf(MappingType.class, bType);
        MappingType mappingType = (MappingType) bType;
        assertSame(BOOL, mappingType.keyType());
        assertSame(INT256, mappingType.valueType());
    }

    @Test
    void nestedMapping() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    mapping(bool => mapping(bool => int256)) public b;
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        Type bType = contractDec.getFieldDeclarations().get(0).getProgramVariable().getType();
        assertInstanceOf(MappingType.class, bType);
        MappingType outerMapping = (MappingType) bType;
        assertSame(BOOL, outerMapping.keyType());
        assertInstanceOf(MappingType.class, outerMapping.valueType());
        MappingType innerMapping = (MappingType) outerMapping.valueType();
        assertSame(BOOL, innerMapping.keyType());
        assertSame(INT256, innerMapping.valueType());
    }

    @Test
    void modifier() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    modifier mod1(){
                        _;
                    }
                    modifier mod2(){
                        _;
                    }
                    function f() public mod1 mod2 {
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        ImmutableArray<ModifierDeclaration> modDecls = contractDec.getModifiers();
        assertEquals(2, modDecls.size());
        FunctionDeclaration f = contractDec.getFunctions().getFirst();
        ImmutableArray<ModifierReference> modRefs = f.getModifiers();
        assertEquals(2, modRefs.size());
        assertEquals("mod1", modRefs.get(0).name);
        assertEquals("mod2", modRefs.get(1).name);
        assertEquals("mod1", modRefs.get(0).toString());
        assertEquals(0, modRefs.get(0).getChildCount());
        assertThrows(IndexOutOfBoundsException.class, () -> modRefs.get(0).getChild(0));
        for (ModifierDeclaration mod : modDecls) {
            assertEquals(1, mod.getChildCount());
            assertInstanceOf(Block.class, mod.getChild(0));
            assertThrows(IndexOutOfBoundsException.class, () -> mod.getChild(1));
        }
        assertTrue(contractDec.getModifiers().get(0).toString().contains("mod1"));
        assertEquals(3, contractDec.getChildCount());
        assertInstanceOf(ModifierDeclaration.class, contractDec.getChild(0));
        assertInstanceOf(ModifierDeclaration.class, contractDec.getChild(1));
        assertInstanceOf(FunctionDeclaration.class, contractDec.getChild(2));
        assertThrows(IndexOutOfBoundsException.class, () -> contractDec.getChild(3));
    }

    @Test
    void modifierWithParameters() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    modifier mod(uint256 x, address y){
                        _;
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        ImmutableArray<ModifierDeclaration> modDecls = contractDec.getModifiers();
        assertEquals(1, modDecls.size());
        ModifierDeclaration mod = modDecls.get(0);
        assertEquals(3, mod.getChildCount());
        assertInstanceOf(ProgramVariable.class, mod.getChild(0));
        assertInstanceOf(ProgramVariable.class, mod.getChild(1));
        assertInstanceOf(Block.class, mod.getChild(2));
        assertThrows(IndexOutOfBoundsException.class, () -> mod.getChild(3));
    }

    @Test
    void parameterListFunction() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    struct Person {
                       int age;
                    }
                    function f(Person memory bob) public {
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        FunctionDeclaration f = contractDec.getFunctions().getFirst();
        assertEquals(1, f.getInputParameters().size());
        ProgramVariable bob = f.getInputParameters().get(0);
        assertEquals(Memory, bob.getDataLocation());
        assertEquals("bob", bob.name().toString());
        assertInstanceOf(StructDeclaration.class, bob.getType());
        assertEquals("SimpleContract.Person", bob.getType().name().toString());
    }

    @Test
    void selfReferenceFunction() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        f();
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("f();"));
        FunctionReference fRef =
            (FunctionReference) ((FunctionCallExpression) ((ExpressionStatement) contractDec
                    .getFunctions().getFirst().getBody().getStatements()
                    .get(0)).getExpression()).functionExp;
        Type type = fRef.getType();
        assertSame(VOID, type);

        FunctionDeclaration selfF = contractDec.getFunctions().getFirst();
        FunctionDeclaration refDecl = fRef.referencedDeclaration;
        assertSame(selfF, refDecl);
        assertSame(Public, selfF.getVisibility());
        assertSame(nonpayable, selfF.getStateMutability());
    }

    @Test
    void enumParse() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    enum State {
                        Begin,
                        End
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("enum State"));
        assertTrue(contractS.contains("Begin, End"));
        assertEquals(1, contractDec.getEnumDeclarations().size());
        EnumDeclaration stateEnum = contractDec.getEnumDeclarations().get(0);
        assertEquals("State", stateEnum.getName().toString());
        assertEquals(2, stateEnum.getMembers().size());
        assertEquals(1, contractDec.getChildCount());
        assertInstanceOf(EnumDeclaration.class, contractDec.getChild(0));
        assertThrows(IndexOutOfBoundsException.class, () -> contractDec.getChild(1));
        assertEquals("State", stateEnum.name().toString());
        assertEquals(2, stateEnum.getChildCount());
        assertInstanceOf(MemberEnumDeclaration.class, stateEnum.getChild(0));
        assertTrue(stateEnum.toString().contains("enum State"));
        assertEquals("Begin", stateEnum.findMember(new Name("Begin")).getName().toString());
        MemberEnumDeclaration member = stateEnum.getMembers().get(0);
        assertEquals("Begin", member.getName().toString());
        assertEquals(0, member.getChildCount());
        assertThrows(IndexOutOfBoundsException.class, () -> member.getChild(0));
        assertEquals("Begin", member.toString());
    }

    @Test
    void usingEnum() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    enum State {
                        Begin,
                        End
                    }
                    function f() public {
                        State s = State.Begin;
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("State.Begin"));
        EnumDeclaration stateEnum = contractDec.getEnumDeclarations().get(0);
        DeclarationStatement declStm = (DeclarationStatement) contractDec.getFunctions().getFirst()
                .getBody().getStatements().get(0);
        StatementVariableDeclaration decl =
            (StatementVariableDeclaration) declStm.getDeclarations().get(0);
        ProgramVariable s = decl.getProgramVariable();
        Type sType = s.getType();
        assertInstanceOf(EnumDeclaration.class, sType);
        assertSame(stateEnum, sType);
    }

    @Test
    void parseSpecification() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    /// @return BoolTrue
                   function func() public pure returns (bool) {
                        return true;
                   }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        FunctionDeclaration functionDeclaration = contractDeclaration.getFunctions().getFirst();
        assertEquals("@return BoolTrue", functionDeclaration.getDocumentation());
    }

    @Test
    void twoContracts() throws IOException {
        // language=solidity
        String contracts = """
                contract A {}
                contract SimpleContract {
                    A a = new A();
                }""";
        SolcParser solcParser = new SolcParser();
        List<SyntaxElement> elements = solcParser.getDeclsJsonParser(contracts);
        assertEquals(2, elements.size());
        ContractDeclaration ctrl = (ContractDeclaration) elements.get(1);
        assertEquals("function () returns (contract A)",
            ((NewExpression) ((FunctionCallExpression) ctrl.getFieldDeclarations().get(0)
                    .getInitializer()).getFunctionExp()).getFunction());
        NewExpression newExp = (NewExpression) ((FunctionCallExpression) ctrl.getFieldDeclarations()
                .get(0).getInitializer()).getFunctionExp();
        assertEquals(0, newExp.getChildCount());
        assertThrows(IndexOutOfBoundsException.class, () -> newExp.getChild(0));
    }

    @Test
    void twoContractsConstructor() throws IOException {
        // language=solidity
        String contracts = """
                contract A {
                    constructor(int a) {
                    }
                }
                contract SimpleContract {
                    A a = new A(0);
                }""";
        SolcParser solcParser = new SolcParser();
        List<SyntaxElement> elements = solcParser.getDeclsJsonParser(contracts);
        assertEquals(2, elements.size());
        ContractDeclaration ctrl = (ContractDeclaration) elements.get(1);
        String ctrlStr = ctrl.toString();
        assertEquals("function (int256) returns (contract A)",
            ((NewExpression) ((FunctionCallExpression) ctrl.getFieldDeclarations().get(0)
                    .getInitializer()).getFunctionExp()).getFunction());
        assertTrue(ctrlStr.contains("A a"));
    }

    @Test
    void selfReferenceContract() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    SimpleContract sc;
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        Type contractType =
            contractDec.getFieldDeclarations().get(0).getProgramVariable().getType();
        assertInstanceOf(ContractDeclaration.class, contractType);
        assertEquals("SimpleContract", contractType.name().toString());
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("SimpleContract sc"));
    }

    @Test
    void sameTupleReturn() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public returns (int, bool) {
                        return (0, false);
                    }
                    function g() public returns (int, bool) {
                        return (0, true);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        TupleType fType = (TupleType) contractDec.getFunctions().get(0).getType();
        TupleType gType = (TupleType) contractDec.getFunctions().get(1).getType();
        assertSame(fType, gType);
        assertEquals(INT, fType.getTypes().get(0));
        assertEquals(BOOL, fType.getTypes().get(1));
    }

    @Test
    void twoMappings() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    mapping(bool => int) public m1;
                    mapping(bool => int) public m2;
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        Type m1Type = contractDec.getFieldDeclarations().get(0).getProgramVariable().getType();
        Type m2Type = contractDec.getFieldDeclarations().get(1).getProgramVariable().getType();
        assertSame(m1Type, m2Type);
    }

    @Test
    void parseNestedStructMemberAssignment() throws IOException {
        // language=solidity
        String contract = """
                contract SimpleContract {
                    struct Account {
                        uint256 balance;
                    }
                    struct Person {
                        uint256 age;
                        Account account;
                    }
                    Person alice;
                    function f() public {
                        alice.account.balance = 10;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        FunctionDeclaration functionF = contractDeclaration.getFunctions().get(0);

        // ExpressionStatement -> AssignmentExpression
        var stmt = functionF.getBody().getStatements().get(0);
        ExpressionStatement exprStatement = (ExpressionStatement) stmt;
        AssignmentExpression assignExpr = (AssignmentExpression) exprStatement.getExpression();

        assertInstanceOf(Uint256Literal.class, assignExpr.getRight());

        // Outer MemberExp: alice.account.balance
        MemberExp outerMember = (MemberExp) assignExpr.getLeft();
        FieldDeclaration balanceField = (FieldDeclaration) outerMember.getRightExp();
        assertEquals("balance", balanceField.name().toString());
        assertEquals("uint256", balanceField.getTypeReference().getTypeName().toString());

        // Inner MemberExp: alice.account
        MemberExp innerMember = (MemberExp) outerMember.getLeftExp();
        assertEquals("alice", ((ProgramVariable) innerMember.getLeftExp()).name().toString());
        FieldDeclaration accountField = (FieldDeclaration) innerMember.getRightExp();
        assertEquals("account", accountField.name().toString());
        assertEquals("SimpleContract.Account",
            accountField.getTypeReference().referencedType.name().toString());

        // Right-hand side literal value 10
        Uint256Literal rhs = (Uint256Literal) assignExpr.getRight();
        assertEquals(10, rhs.getValue().intValueExact());
    }
}
