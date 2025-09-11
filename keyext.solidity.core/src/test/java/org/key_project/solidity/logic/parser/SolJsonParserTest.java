/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.solidity.logic.parser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.key_project.logic.SyntaxElement;
import org.key_project.solidity.program.ast.SolidityProgramElement;
import org.key_project.solidity.program.ast.declarations.*;
import org.key_project.solidity.program.ast.expressions.Expression;
import org.key_project.solidity.program.ast.expressions.MemberExp;
import org.key_project.solidity.program.ast.expressions.literals.BoolLiteral;
import org.key_project.solidity.program.ast.expressions.literals.Uint256Literal;
import org.key_project.solidity.program.ast.expressions.operators.*;
import org.key_project.solidity.program.ast.references.StateVariableReference;
import org.key_project.solidity.program.ast.statement.*;
import org.key_project.solidity.program.parser.SolJSONParser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.key_project.solidity.program.parser.SolcWrapper;

import static org.key_project.solidity.program.ast.abstractions.PrimitiveType.INT;


class SolJsonParserTest {

    @Test
    void parse() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(1, contractDeclaration.getFieldDeclarations().size());
    }

    @Test
    void parseContractWithIntAndBool() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance;
                   bool closed;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(2, contractDeclaration.getFieldDeclarations().size());
    }

    @Test
    void parseContractWithIntAndBoolSet() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance = 1000;
                   bool closed = true;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(2, contractDeclaration.getFieldDeclarations().size());
        StateVariableDeclaration firstField = contractDeclaration.getFieldDeclarations().get(0);
        Assertions.assertInstanceOf(Uint256Literal.class, firstField.getInitializer());
        Assertions.assertEquals(1000,
            ((Uint256Literal) firstField.getInitializer()).getValue().longValue());
        StateVariableDeclaration secondField = contractDeclaration.getFieldDeclarations().get(1);
        Assertions.assertInstanceOf(BoolLiteral.class, secondField.getInitializer());
        Assertions.assertSame(BoolLiteral.TRUE, secondField.getInitializer());
    }

    @Test
    void parseContractWithAddition() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance = 1000;
                   uint256 deposit = 5 + 100;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(2, contractDeclaration.getFieldDeclarations().size());
    }

    @Test
    void parseContractWithReferenceAddition() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance = 1000;
                   uint256 deposit = balance + 100;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(2, contractDeclaration.getFieldDeclarations().size());
        Expression initializer = contractDeclaration.getFieldDeclarations().get(1).getInitializer();
        Assertions.assertNotNull(initializer);
        Assertions.assertInstanceOf(AddOperator.class, initializer);
        Assertions.assertInstanceOf(StateVariableReference.class,
            ((AddOperator) initializer).getChild(0));
        Assertions.assertInstanceOf(Uint256Literal.class, ((AddOperator) initializer).getChild(1));
    }

    @Test
    void parseContractWithBoth() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 balance = 1000;
                   SimpleContract other;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(2, contractDeclaration.getFieldDeclarations().size());
    }

    @Test
    void parseFunction() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   function func() public pure {
                   }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(1, contractDeclaration.getFunctions().size());
        FunctionDeclaration functionDeclaration = contractDeclaration.getFunctions().getFirst();
        Block block = functionDeclaration.getBody();
        Assertions.assertNotNull(block);
        Assertions.assertEquals(0, block.getChildCount());
        Assertions.assertTrue(functionDeclaration.toString().contains("function func () public pure"));
    }

    @Test
    void parseComplexFunction() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    function func(uint256 v) public pure returns(uint256) {
                       return v;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(1, contractDeclaration.getFunctions().size());
        FunctionDeclaration function = contractDeclaration.getFunctions().getFirst();
        Assertions.assertEquals(1, function.getInputParameters().size());
        Assertions.assertEquals(1, function.getReturnParameters().size());
        Block block = function.getBody();
        Assertions.assertNotNull(block);
        Assertions.assertEquals(1, block.getChildCount());
    }

    @Test
    void parseSimpleAssignment() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   function func(uint256 v) public pure  {
                      v = 4;
                   }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(1, contractDeclaration.getFunctions().size());
        FunctionDeclaration function = contractDeclaration.getFunctions().getFirst();
        Block block = function.getBody();
        Assertions.assertNotNull(block);
        Assertions.assertEquals(1, block.getChildCount());
        Assertions.assertEquals(1, block.getStatements().size());
        Statement exprStmnt = block.getStatements().get(0);
        Assertions.assertInstanceOf(ExpressionStatement.class, exprStmnt);
        Assertions.assertInstanceOf(AssignmentExpression.class, exprStmnt.getChild(0));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void parseContractWithOperations() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 deposit = 5 ^ 5 + 100 % 4 - 1 * 3 / 3;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(1, contractDeclaration.getFieldDeclarations().size());
        SolidityProgramElement expOpSynt = contractDeclaration.getFieldDeclarations().get(0).getChild(1);
        Assertions.assertInstanceOf(ExponentialOperator.class, expOpSynt);
        ExponentialOperator expOp = (ExponentialOperator) expOpSynt;
        Assertions.assertEquals(INT, expOp.getType());
    }

    @Test
    void parseContractWithManyOperations() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   bool v = true && true || false;
                   int w = ~0;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(2, contractDeclaration.getFieldDeclarations().size());
        Assertions.assertInstanceOf(OrOperator.class,
            contractDeclaration.getFieldDeclarations().get(0).getInitializer());
    }

    @Test
    void parseIf() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   bool v = true ? false : true;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(1, contractDeclaration.getFieldDeclarations().size());
        Assertions.assertInstanceOf(TernaryOperator.class,
                contractDeclaration.getFieldDeclarations().get(0).getInitializer());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void parseContractWithBoolIntOperations() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   bool v = 1 != 0 && 1 == 1 && 0 < 0 && 0 <= 0 && 0 > 0 && 0 > 0;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(1, contractDeclaration.getFieldDeclarations().size());
        Assertions.assertInstanceOf(AndOperator.class,
            contractDeclaration.getFieldDeclarations().get(0).getInitializer());
    }

    @Test
    void parseUnaryOperations() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                   uint256 i;
                   uint256 j;
                   uint256 v = i++ + j--;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(3, contractDeclaration.getFieldDeclarations().size());
        SyntaxElement exp = contractDeclaration.getChild(2).getChild(1).getChild(0);
        Assertions.assertInstanceOf(PlusPlusOperator.class, exp);
    }

    @Test
    void parseComplexAssignment() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    function func(uint256 u, uint256 v, uint256 w) public pure  {
                        v += w = u -= 1;
                    }
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        Assertions.assertEquals(1, contractDeclaration.getFunctions().size());
        Assertions.assertInstanceOf(PlusEqualOperator.class, contractDeclaration.getFunctions().getFirst().getBody().getStatements().get(0).getChild(0));
    }

    @Test
    void parseStruct() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    struct Person {
                       int age;
                    }
                    Person alice;
                }""";
        ContractDeclaration contractDeclaration = getDeclStr(contract);
        List<StructDeclaration> structs = contractDeclaration.getStructs();
        Assertions.assertEquals(1, structs.size());
        Assertions.assertEquals(1, structs.getFirst().getFields().size());
    }

    @Test
    void parseUsingStruct() throws IOException {
        //language=solidity
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
        Assertions.assertEquals(1, structs.size());
        Assertions.assertEquals(1, structs.getFirst().getFields().size());
        FunctionDeclaration function = contractDeclaration.getFunctions().getFirst();
        var retStmSynt = function.getBody().getStatements().get(0);
        Assertions.assertInstanceOf(ReturnStatment.class, retStmSynt);
        ReturnStatment retStm = (ReturnStatment) retStmSynt;
        Expression retExp = retStm.getReturnExp();
        Assertions.assertInstanceOf(MemberExp.class, retExp);
    }

    @Test
    void parseMemory() throws IOException {
        //language=solidity
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
        var declStmS = contractDeclaration.getFunctions().getFirst().getBody().getStatements().get(0);
        Assertions.assertInstanceOf(DeclarationStatement.class, declStmS);
        DeclarationStatement declStms = (DeclarationStatement) declStmS;
        StatementVariableDeclaration decl = (StatementVariableDeclaration) declStms.getDeclarations().getFirst();
        String contractStr = contractDeclaration.toString();
        Assertions.assertTrue(contractStr.contains("Person memory alice"));
    }

    @Test
    void parseElementaryType() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    int[] v;
                
                    function f() public returns (int) {
                        return v[1+1];
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        Assertions.assertTrue(contractDec.toString().contains("v[1 + 1]"));
    }

    @Test
    void parseInlineArray() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        bool[3] memory foo;
                        foo = [false, true, false];
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains("bool[3] memory foo"));
        Assertions.assertTrue(contractS.contains("foo = [false, true, false]"));

    }

    @Test
    void parseSlice() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    function f(bool[] calldata v) public {
                        v[0:1];
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        Assertions.assertTrue(contractDec.toString().contains("v[0:1]"));
    }

    @Test
    void cast() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        bool(true);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        Assertions.assertTrue(contractDec.toString().contains("bool(true)"));
    }

    @Test
    void functionCall() throws IOException {
        //language=solidity
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
        Assertions.assertTrue(contractDec.toString().contains("f(v)"));
    }

    @Test
    void parseIfStatement() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    int i;
                    function f() public {
                        if(true) i = 0;
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains("if"));
        Assertions.assertTrue(contractS.contains("i = 0"));
    }

    @Test
    void parseIfElseStm() throws IOException {
        //language=solidity
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
        Assertions.assertTrue(contractS.contains("i == 2"));
        Assertions.assertTrue(contractS.contains("if"));
        Assertions.assertTrue(contractS.contains("i = 0"));
        Assertions.assertTrue(contractS.contains("else"));
        Assertions.assertTrue(contractS.contains("i = 1"));
    }

    @Test
    void parseWhileStm() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    int i;
                    function f() public {
                        while(i == 0) i = 1;
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains("while"));
        Assertions.assertTrue(contractS.contains("i == 0"));
        Assertions.assertTrue(contractS.contains("i = 1"));
    }

    @Test
    void parseWhileStmYul() throws IOException {
        //language=solidity
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
        Assertions.assertTrue(contractS.contains("while"));
        Assertions.assertTrue(contractS.contains("continue"));
        Assertions.assertTrue(contractS.contains("break"));
    }

    @Test
    void parseFor() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    int i;
                    function f() public {
                        for(i = 0; i<10; i++){}
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains("for(i = 0; i < 10; i ++)"));
    }

    @Test
    void parseForEmpty() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        for(; ; ){}
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains("for(; ; )"));
    }

    @Test
    void parseDoWhile() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        do {} while (true);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains("do"));
        Assertions.assertTrue(contractS.contains("while (true)"));
    }

    @Disabled("Try catch not implemented")
    @Test
    void parseTryCatch() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    function f(address target) public {
                        try SimpleContract(target).g() {}
                        catch {}
                    }
                    function g() external pure {
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains(""));
    }

    @Disabled("Revert and require should be implemented as a regular function")
    @Test
    void parseRevertRequire() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    function f(address target) public {
                        revert("");
                        require(true);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains(""));
    }

    @Test
    void tupleReturn() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public returns (bool, bool) {
                        return (false, true);
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains("false, true"));
    }

    @Test
    void constructor() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    constructor () {
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
    }

    @Disabled("Mapping type is not implemented")
    @Test
    void mapping() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    mapping (bool => bool) b;
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains(""));
    }

    @Test
    void modifier() throws IOException {
        //language=solidity
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
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains("_;"));
        Assertions.assertTrue(contractS.contains("mod1 mod2"));
    }

    @Test
    void selfReference() throws IOException {
        //language=solidity
        String contract = """
                contract SimpleContract {
                    function f() public {
                        f();
                    }
                }""";
        ContractDeclaration contractDec = getDeclStr(contract);
        String contractS = contractDec.toString();
        Assertions.assertTrue(contractS.contains("f()"));
    }

    private static ContractDeclaration getDeclStr(String contract) throws IOException {
        final Path solc = Path.of("/opt", "local", "bin", "solc");
        SolcWrapper solcWrapper = new SolcWrapper(solc);
        String contractJson = solcWrapper.readSol(contract);
        SolidityProgramElement programElement = getSolidityFromStr(contractJson);
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        return (ContractDeclaration) programElement;
    }

    private static ContractDeclaration getDeclaration(String fileName) throws IOException {
        SolidityProgramElement programElement = getSolidityProgramElement(fileName);
        Assertions.assertInstanceOf(ContractDeclaration.class, programElement);
        return (ContractDeclaration) programElement;
    }

    private static SolidityProgramElement getSolidityFromStr(String contract)
            throws IOException {
        SolJSONParser jsonParser = new SolJSONParser();
        List<SolidityProgramElement> unit = jsonParser.parse(contract);
        Assertions.assertNotNull(unit);
        Assertions.assertEquals(1, unit.size());
        SolidityProgramElement programElement = unit.getFirst();
        return programElement;
    }

    private static SolidityProgramElement getSolidityProgramElement(String solFileName)
            throws IOException {
        SolJSONParser jsonParser = new SolJSONParser();
        URI fileURI = getFile(solFileName);
        Assertions.assertNotNull(fileURI);
        List<SolidityProgramElement> unit = jsonParser.parse(fileURI);
        Assertions.assertNotNull(unit);
        Assertions.assertEquals(1, unit.size());
        SolidityProgramElement programElement = unit.getFirst();
        return programElement;
    }

    private static URI getFile(String solFileName) {
        try {
            // return FindResources.getResource(solFileName, SolJsonParserTest.class).toUri();
            return SolJSONParser.class.getResource(solFileName).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
