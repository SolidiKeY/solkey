/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;
import java.util.List;

import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.common.Services;
import org.key_project.solidity.logic.op.ProgramVariable;
import org.key_project.solidity.program.ast.abstractions.ArrayType;
import org.key_project.solidity.program.ast.abstractions.DynamicArrayType;
import org.key_project.solidity.program.ast.abstractions.TupleType;
import org.key_project.solidity.program.ast.abstractions.Type;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.FunctionCallExpression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.expressions.NewExpression;
import org.key_project.solidity.program.ast.expressions.literals.*;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral.*;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.abstractions.MappingType;
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
            contractDeclaration.getFieldDeclarations().get(0).getProgramVariable().getLocation());
        assertSame(UINT256,
            contractDeclaration.getFieldDeclarations().get(0).getProgramVariable().getType());
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
        assertNotNull(initializer);
        assertInstanceOf(AddOperator.class, initializer);
        AddOperator addOp = (AddOperator) initializer;
        assertInstanceOf(ProgramVariable.class, addOp.getLeft());
        assertInstanceOf(Uint256Literal.class, addOp.getRight());
        assertSame(UINT256, initializer.getType());
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
        assertNotNull(block);
        assertEquals(0, block.getStatements().size());
        assertTrue(
            functionDeclaration.toString().contains("function func () public pure"));
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
        assertNotNull(block);
        assertEquals(1, block.getStatements().size());
        assertEquals(Default, function.getInputParameters().get(0).getLocation());
        assertSame(UINT256, function.getInputParameters().get(0).getType());
        assertSame(UINT256, function.getReturnParameters().get(0).getType());
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
        assertNotNull(block);
        assertEquals(1, block.getStatements().size());
        Statement exprStmnt = block.getStatements().get(0);
        assertInstanceOf(ExpressionStatement.class, exprStmnt);
        assertInstanceOf(AssignmentExpression.class,
            ((ExpressionStatement) exprStmnt).getExpression());
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
        assertSame(Default, v.getLocation());
        assertSame(INT256, v.getType());
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
            contractDeclaration.getFunctions().get(0).getInputParameters().get(0).getLocation());
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
        assertEquals(1, structs.getFirst().getFields().size());
        assertEquals(Storage,
            contractDeclaration.getFieldDeclarations().get(0).getProgramVariable().getLocation());
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
        assertNotNull(decl);
        String contractStr = contractDeclaration.toString();
        assertTrue(contractStr.contains("Person memory alice"));
        assertEquals(Memory, decl.getProgramVariable().getLocation());
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
        assertEquals(Storage, bob.getLocation());
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
        FunctionDeclaration g = contractDec.getFunctions().get(1);
        assertSame(ADDRESS, g.getInputParameters().get(0).getType());
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
        String contractS = contractDec.toString();
        assertTrue(contractS.contains("catch Error(string memory reason)"));
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
        TupleType type = contractDec.getFunctions().getFirst().getType();
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
        assertNotNull(constructor.getBody());
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
        for (ModifierDeclaration mod : modDecls) {
            assertEquals(1, mod.getChildCount());
            assertInstanceOf(Block.class, mod.getChild(0));
            assertThrows(IndexOutOfBoundsException.class, () -> mod.getChild(1));
        }
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
        assertEquals(Memory, bob.getLocation());
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
        assertInstanceOf(TupleType.class, type);
        assertEquals(0, ((TupleType) type).getTypes().size());

        FunctionDeclaration refDecl = fRef.referencedDeclaration;
        assertNotNull(refDecl);
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
        assertNotNull(contractType);
        assertInstanceOf(ContractDeclaration.class, contractType);
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
        TupleType fType = contractDec.getFunctions().get(0).getType();
        TupleType gType = contractDec.getFunctions().get(1).getType();
        Services services = new Services();
        assertSame(fType, gType);
        assertSame(fType.getSort(services), gType.getSort(services));
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
        Services services = new Services();
        assertSame(m1Type, m2Type);
        assertSame(m1Type.getSort(services), m2Type.getSort(services));
    }
}
