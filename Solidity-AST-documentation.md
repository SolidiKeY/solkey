# Solidity AST Documentation

This document describes the Abstract Syntax Tree (AST) used to represent Solidity smart contracts in the SolKey verification framework. The AST is built by parsing Solidity source code via the `solc` compiler JSON output and is used as the basis for formal verification with the KeY prover.

## Table of Contents

1. [Overview](#overview)
2. [Core Interfaces](#core-interfaces)
3. [Types](#types)
4. [Declarations](#declarations)
5. [Expressions](#expressions)
6. [Statements](#statements)
7. [References](#references)
8. [Visitor Pattern](#visitor-pattern)
9. [Class Hierarchy](#class-hierarchy)
10. [Examples](#examples)

---

## Overview

Every AST node implements `SyntaxElement` (from the KeY framework) and the project-specific `SolidityProgramElement` interface. All nodes support:

- **`getChild(int n)`** — access the n-th child node (throws `IndexOutOfBoundsException` if out of range)
- **`getChildCount()`** — number of direct children
- **`visit(Visitor v)`** — traverse the AST using the Visitor pattern
- **`toString()`** — reconstruct Solidity source text

The entry point for a parsed file is a list of `ContractDeclaration` objects, one per top-level contract.

---

## Core Interfaces

### `Type`

Represents any Solidity type. Extends `Named` and `SyntaxElement`.

Key method: `Sort getSort(Services)` — returns the corresponding logic sort used in KeY proofs.

Implemented by: `PrimitiveType`, `ArrayType`, `DynamicArrayType`, `MappingType`, `TupleType`, `ContractDeclaration`, `StructDeclaration`, `EnumDeclaration`.

### `Expression`

Extends `SolidityProgramElement`. Any expression node that can be evaluated to a value.

Key method: `Type getType()` — the Solidity type of the expression.

### `Statement`

Marker interface. Any statement node (control flow, declaration, expression statement, etc.).

### `Declaration`

Marker interface. Any declaration node (contract, function, variable, struct, etc.).

---

## Types

### `PrimitiveType`

Singleton-per-type instances for all built-in Solidity types. Accessed as static constants:

| Category        | Examples                                     |
|-----------------|----------------------------------------------|
| Signed integers | `INT`, `INT8`, `INT16`, …, `INT256`           |
| Unsigned integers | `UINT`, `UINT8`, `UINT16`, …, `UINT256`    |
| Fixed bytes     | `BYTES1`, `BYTES2`, …, `BYTES32`             |
| Special         | `ADDRESS`, `BOOL`, `STRING`, `BYTES`         |

```java
assertSame(UINT256, variable.getType());
assertSame(BOOL,    variable.getType());
assertSame(ADDRESS, parameter.getType());
```

### `ArrayType`

A fixed-length array: `ElementType[length]`.

```java
ArrayType arrayType = (ArrayType) fooType;
assertEquals(3, arrayType.length());
assertSame(BOOL, arrayType.getElementType());
// Solidity: bool[3] memory foo;
```

### `DynamicArrayType`

A dynamic-length array: `ElementType[]`.

```java
DynamicArrayType arrayType = (DynamicArrayType) vType;
assertSame(INT, arrayType.getElementType());
// Solidity: int[] v;
```

### `MappingType`

A key-value mapping: `mapping(KeyType => ValueType)`.

```java
MappingType mappingType = (MappingType) bType;
assertSame(BOOL,   mappingType.keyType());
assertSame(INT256, mappingType.valueType());
// Solidity: mapping(bool => int256) public b;
```

Mappings can be nested:

```java
MappingType outer = (MappingType) bType;
assertSame(BOOL, outer.keyType());
MappingType inner = (MappingType) outer.valueType();
assertSame(BOOL,   inner.keyType());
assertSame(INT256, inner.valueType());
// Solidity: mapping(bool => mapping(bool => int256)) public b;
```

Two mappings with the same key/value types are the **same object** (interned):

```java
assertSame(m1Type, m2Type); // structural sharing
```

### `TupleType`

A tuple of types, used for multiple return values.

```java
TupleType type = function.getType();
assertEquals(BOOL, type.getTypes().get(0));
assertEquals(BOOL, type.getTypes().get(1));
// Solidity: returns (bool, bool)
```

Functions with the same return signature share the **same** `TupleType` instance.

### `StructDeclaration` as a Type

Structs act as user-defined types. The `StructDeclaration` object itself is the `Type`:

```java
assertInstanceOf(StructDeclaration.class, bob.getType());
assertEquals("SimpleContract.Person", bob.getType().name().toString());
```

Struct names are qualified with the enclosing contract: `ContractName.StructName`.

### `EnumDeclaration` as a Type

Similarly, enum declarations serve as their own type:

```java
assertInstanceOf(EnumDeclaration.class, sType);
assertSame(stateEnum, sType);
// Solidity: State s = State.Begin;
```

### `ContractDeclaration` as a Type

A contract itself is a type (for storage of contract references):

```java
assertInstanceOf(ContractDeclaration.class, contractType);
assertEquals("SimpleContract", contractType.name().toString());
// Solidity: SimpleContract sc;
```

---

## Declarations

### `ContractDeclaration`

The root node for a single contract. Contains all top-level members.

```
ContractDeclaration
├── StateVariableDeclaration*
├── StructDeclaration*
├── EnumDeclaration*
├── ModifierDeclaration*
└── FunctionDeclaration*
```

Key methods:

| Method                     | Returns                                       |
|----------------------------|-----------------------------------------------|
| `name()`                   | `Name` — the contract name                    |
| `getFieldDeclarations()`   | `ImmutableArray<StateVariableDeclaration>`    |
| `getStructs()`             | `List<StructDeclaration>`                     |
| `getEnumDeclarations()`    | `List<EnumDeclaration>`                       |
| `getModifiers()`           | `ImmutableArray<ModifierDeclaration>`         |
| `getFunctions()`           | `List<FunctionDeclaration>`                   |

`getChild(n)` order: modifiers first, then functions (and other members interleaved by definition order).

```java
// contract with 2 modifiers and 1 function:
assertEquals(3, contractDec.getChildCount());
assertInstanceOf(ModifierDeclaration.class, contractDec.getChild(0));
assertInstanceOf(ModifierDeclaration.class, contractDec.getChild(1));
assertInstanceOf(FunctionDeclaration.class, contractDec.getChild(2));
```

### `FunctionDeclaration`

Represents a function, constructor, or fallback.

```
FunctionDeclaration
├── ProgramVariable*   (return parameters, first)
├── ProgramVariable*   (input parameters, after return params)
└── Block              (body, last)
```

Key methods:

| Method                    | Returns                            |
|---------------------------|------------------------------------|
| `name()`                  | `Name`                             |
| `getKind()`               | `String` — `"function"`, `"constructor"`, etc. |
| `getInputParameters()`    | `ImmutableArray<ProgramVariable>`  |
| `getReturnParameters()`   | `ImmutableArray<ProgramVariable>`  |
| `getType()`               | `TupleType` — return type          |
| `getBody()`               | `Block`                            |
| `getVisibility()`         | `Visibility` — `Public`, `private`, `internal`, `external` |
| `getStateMutability()`    | `StateMutability` — `pure`, `view`, `payable`, `nonpayable` |
| `getModifiers()`          | `ImmutableArray<ModifierReference>` |
| `getDocumentation()`      | `String` — NatSpec doc comment     |

```java
// function func(uint256 v) public pure returns(uint256)
assertEquals(3, function.getChildCount());
assertInstanceOf(ProgramVariable.class, function.getChild(0)); // return param
assertInstanceOf(ProgramVariable.class, function.getChild(1)); // input param
assertInstanceOf(Block.class,           function.getChild(2)); // body

assertSame(Public, function.getVisibility());
assertSame(pure,   function.getStateMutability());
assertSame(UINT256, function.getInputParameters().get(0).getType());
assertSame(UINT256, function.getReturnParameters().get(0).getType());
```

### `StateVariableDeclaration`

A contract-level state variable, with an optional initializer.

```
StateVariableDeclaration (no initializer)
└── ProgramVariable

StateVariableDeclaration (with initializer)
├── ProgramVariable
└── Expression   (initializer)
```

```java
// uint256 balance;
assertEquals(1, balanceDecl.getChildCount());
assertInstanceOf(ProgramVariable.class, balanceDecl.getChild(0));

// uint256 balance = 1000;
assertEquals(2, firstField.getChildCount());
assertInstanceOf(ProgramVariable.class,  firstField.getChild(0));
assertInstanceOf(Uint256Literal.class,   firstField.getChild(1));

assertEquals(1000, ((Uint256Literal) firstField.getInitializer()).getValue().longValue());
assertEquals(Storage, firstField.getProgramVariable().getLocation());
```

### `ProgramVariable`

Holds the name, type, and storage location of a variable.

```java
assertEquals("balance", pv.name().toString());
assertSame(UINT256, pv.getType());
assertSame(Storage, pv.getLocation()); // Storage / Memory / Calldata / Default
```

`DataLocation` values: `Storage`, `Memory`, `Calldata`, `Default`.

### `StructDeclaration`

```
StructDeclaration
└── FieldDeclaration*
```

```java
assertEquals("SimpleContract.Person", struct.name().toString());
assertEquals(1, struct.getChildCount());
assertInstanceOf(FieldDeclaration.class, struct.getChild(0));

FieldDeclaration field = struct.getFields().get(0);
assertEquals("int", field.getTypeReference().getTypeName().toString());
assertNull(field.getInitializer());
```

### `EnumDeclaration`

```
EnumDeclaration
├── MemberEnumDeclaration
└── MemberEnumDeclaration
```

```java
assertEquals("State", stateEnum.name().toString());
assertEquals(2, stateEnum.getChildCount());
assertInstanceOf(MemberEnumDeclaration.class, stateEnum.getChild(0));

MemberEnumDeclaration member = stateEnum.getMembers().get(0);
assertEquals("Begin", member.getName().toString());
assertEquals(0, member.getChildCount()); // leaf node
assertEquals("Begin", member.toString());

// lookup by name:
stateEnum.findMember(new Name("Begin"));
```

### `ModifierDeclaration`

```
ModifierDeclaration (no params)
└── Block

ModifierDeclaration (with params)
├── ProgramVariable*
└── Block
```

```java
// modifier mod(uint256 x, address y) { _; }
assertEquals(3, mod.getChildCount());
assertInstanceOf(ProgramVariable.class, mod.getChild(0)); // x
assertInstanceOf(ProgramVariable.class, mod.getChild(1)); // y
assertInstanceOf(Block.class,           mod.getChild(2)); // body
```

### `StatementVariableDeclaration`

A local variable declaration within a statement.

```java
ProgramVariable v = decl.getProgramVariable();
assertSame(INT256, v.getType());
assertSame(Default, v.getLocation());
assertEquals("alice", v.name().toString());
```

---

## Expressions

All expressions extend `SolidityExpression` and implement `getType()`.

### Literals

| Class           | Solidity        | Notes                          |
|-----------------|-----------------|--------------------------------|
| `BoolLiteral`   | `true`, `false` | Singletons: `BoolLiteral.TRUE`, `BoolLiteral.FALSE` |
| `Uint256Literal`| `1000`, `0`     | `getValue()` returns `BigInteger` |

```java
assertSame(TRUE,  secondField.getInitializer());
assertSame(FALSE, tupleExpr.getChild(0));

Uint256Literal lit = (Uint256Literal) firstField.getInitializer();
assertEquals(1000, lit.getValue().longValue());
```

### Binary Operators

Extend `BinaryOperator`. All have `getLeft()`, `getRight()`, and 2 children.

| Category       | Classes                                                                 |
|----------------|-------------------------------------------------------------------------|
| Arithmetic     | `AddOperator`, `SubtractionOperator`, `MultiplicationOperator`, `DivOperator`, `ModOperator`, `ExponentialOperator` |
| Comparison     | `EqualOperator`, `UnequalOperator`, `LessOperator`, `LessEqualOperator`, `GreaterOperator`, `GreaterEqualOperator` |
| Logical        | `AndOperator`, `OrOperator`                                             |
| Bitwise        | `BitwiseAndOperator`, `BitwiseOrOperator`, `BitwiseXorOperator`, `LeftShiftOperator`, `RightShiftOperator` |
| Assignment     | `AssignmentExpression`, `PlusEqualOperator`, `MinusEqualOperator`, … (one per compound assignment) |

```java
// uint256 deposit = balance + 100;
AddOperator addOp = (AddOperator) initializer;
assertInstanceOf(ProgramVariable.class, addOp.getLeft());  // balance
assertInstanceOf(Uint256Literal.class,  addOp.getRight()); // 100
assertSame(UINT256, addOp.getType());
assertEquals(2, addOp.getChildCount());
assertSame(addOp.getLeft(),  addOp.getChild(0));
assertSame(addOp.getRight(), addOp.getChild(1));
```

### Unary Operators

Extend `UnaryOperator`. All have `getExp()` and 1 child.

| Class                | Solidity  | Position |
|----------------------|-----------|----------|
| `PlusPlusOperator`   | `i++`     | postfix  |
| `MinusMinusOperator` | `i--`     | postfix  |
| `NegateOperator`     | `-x`      | prefix   |
| `NotOperator`        | `!x`      | prefix   |
| `BitwiseNotOperator` | `~x`      | prefix   |
| `DeleteOperator`     | `delete x`| prefix   |

```java
// uint256 v = i++ + j--;
PlusPlusOperator ppOp = (PlusPlusOperator) addOp.getLeft();
assertEquals(1, ppOp.getChildCount());
assertInstanceOf(ProgramVariable.class, ppOp.getChild(0));
```

### `TernaryOperator`

Represents `condition ? falseExpr : trueExpr`. Has 3 children.

> Note: child order is `[condition, falseExpr, trueExpr]` (after `?` is index 2, after `:` is index 1).

```java
// bool v = true ? false : true;
assertEquals(3, ternary.getChildCount());
assertSame(TRUE,  ternary.getChild(0)); // condition
assertSame(TRUE,  ternary.getChild(1)); // expression after ':'
assertSame(FALSE, ternary.getChild(2)); // expression after '?'
```

### `AssignmentExpression`

Extends `AssignOperator`. The left-hand side is `getLeft()`, right-hand side is `getRight()`.

### `FunctionCallExpression`

```
FunctionCallExpression
├── Expression*  (arguments, first)
└── Expression   (function reference, last)
```

Key methods: `getFunctionExp()`, `getArguments()`, `getArgument(int n)`.

```java
// f(v);
assertEquals(2, fCall.getChildCount()); // 1 arg + functionExp
assertInstanceOf(ProgramVariable.class,  fCall.getChild(0)); // v (argument)
assertInstanceOf(FunctionReference.class, fCall.getChild(1)); // f (callee)

assertEquals(1, fCall.getArguments().size());
```

### `MemberExp`

Member access: `obj.field` or `obj.method`. Has 2 children.

```java
// alice.age
assertInstanceOf(ProgramVariable.class,  memberExp.getLeftExp());  // alice
assertInstanceOf(FieldDeclaration.class, memberExp.getRightExp()); // age field

// alice.account.balance (nested)
MemberExp outer = ...; // outer: alice.account.balance
FieldDeclaration balance = (FieldDeclaration) outer.getRightExp();
MemberExp inner = (MemberExp) outer.getLeftExp();    // alice.account
ProgramVariable alice = (ProgramVariable) inner.getLeftExp();
FieldDeclaration account = (FieldDeclaration) inner.getRightExp();
```

### `IndexExpression`

Array/mapping access: `arr[index]`. Has 2 children.

```java
// v[1+1]
assertEquals(2, idxExp.getChildCount());
assertInstanceOf(ProgramVariable.class, idxExp.getChild(0)); // v
assertInstanceOf(AddOperator.class,     idxExp.getChild(1)); // 1+1
```

### `TupleExpression`

A tuple literal `(expr1, expr2, …)`. Children are the elements in order.

```java
// return (false, true);
assertEquals(2, tupleExpr.getChildCount());
assertSame(FALSE, tupleExpr.getChild(0));
assertSame(TRUE,  tupleExpr.getChild(1));
```

### `NewExpression`

Constructor call wrapped in `FunctionCallExpression`:

```java
// new A(0)
FunctionCallExpression call = ...;
NewExpression newExp = (NewExpression) call.getFunctionExp();
assertEquals("function (int256) returns (contract A)", newExp.getFunction());
assertEquals(0, newExp.getChildCount()); // leaf node
```

### `ElementaryExpression`

Type cast: `bool(true)`, `uint256(x)`. Leaf node (0 children via standard traversal).

---

## Statements

### `Block`

A sequence of statements enclosed in `{}`.

```
Block
├── Statement
├── Statement
└── ...
```

```java
assertEquals(1, block.getStatements().size());
assertTrue(block.isEmpty()); // if no statements
assertEquals(0, block.getChildCount()); // empty block
assertInstanceOf(ReturnStatement.class, block.getChild(0));
```

### `ExpressionStatement`

Wraps an expression used as a statement (e.g., assignment, function call). Has 1 child.

```java
assertInstanceOf(AssignmentExpression.class, exprStmt.getExpression());
assertEquals(1, exprStmt.getChildCount());
```

### `DeclarationStatement`

Declares one or more local variables, with an optional initializer.

```
DeclarationStatement (no initializer)
└── StatementVariableDeclaration

DeclarationStatement (with initializer)
├── StatementVariableDeclaration
└── Expression   (initializer)
```

```java
// bool b = true;
assertEquals(2, ds.getChildCount());
assertInstanceOf(StatementVariableDeclaration.class, ds.getChild(0));
assertSame(TRUE, ds.getChild(1));

// (bool a, bool b) = f();
assertEquals(2, decl.getDeclarations().size());
assertEquals("(bool a, bool b) = f();", decl.toString());
```

### `ReturnStatement`

```
ReturnStatement
└── Expression?   (optional return value)
```

```java
assertEquals(1, retStmt.getChildCount());
assertInstanceOf(ProgramVariable.class, retStmt.getChild(0));
```

### `ConditionStatement`

If and if-else statements.

```
ConditionStatement (if only)
├── Expression   (condition)
└── Statement    (true branch)

ConditionStatement (if-else)
├── Expression   (condition)
├── Statement    (true branch)
└── Statement    (false branch)
```

```java
// if(true) i = 0;
assertEquals(2, ifStmt.getChildCount());
assertSame(TRUE, ifStmt.getChild(0));
assertInstanceOf(ExpressionStatement.class, ifStmt.getChild(1));

// if(i == 2) i = 0; else i = 1;
assertEquals(3, ifElseStmt.getChildCount());
assertInstanceOf(EqualOperator.class,      ifElseStmt.getChild(0)); // condition
assertInstanceOf(ExpressionStatement.class, ifElseStmt.getChild(1)); // true branch
assertInstanceOf(ExpressionStatement.class, ifElseStmt.getChild(2)); // false branch
```

### `WhileStatement`

```
WhileStatement
├── Expression   (condition)
└── Statement    (body)
```

### `DoWhileStatement`

```
DoWhileStatement
├── Statement    (body)
└── Expression   (condition)
```

### `ForStatement`

```
ForStatement
├── ForInit?     (optional init)
├── Expression?  (optional condition)
├── ForUpdate?   (optional update)
└── Statement    (body)
```

`ForInit` wraps an init expression; `ForUpdate` wraps an update expression.

```java
// for(i = 0; i<10; i++){}
assertEquals(4, forStmt.getChildCount());
assertInstanceOf(AssignmentExpression.class, forStmt.getInit().getInit());
assertEquals(1, forStmt.getInit().getChildCount());
assertInstanceOf(PlusPlusOperator.class, forStmt.getUpdate().getUpdate());

// for(; ; ){}
assertEquals(1, forStmt.getChildCount());
assertInstanceOf(Block.class, forStmt.getChild(0));
```

### `BreakStatement` / `ContinueStatement`

Leaf nodes with 0 children.

```java
assertEquals(0, contStmt.getChildCount());
assertEquals(0, breakStmt.getChildCount());
```

### `TryStatement`

```
TryStatement
├── Expression              (tried expression, e.g. external call)
├── ProgramVariable*        (return declarations, if any)
├── Block                   (success body)
└── CatchClause*            (one per catch block)
```

```java
// try SimpleContract(target).g() returns (int a) { int b = a; } catch { }
assertEquals(4, tryStatement.getChildCount());
ProgramVariable returnA = tryStatement.getReturnDeclaration().get(0);
assertSame(tryStatement.getChild(1), returnA);
```

### `CatchClause`

Represents a single `catch` block. `Kind` is one of `Error`, `Panic`, `LowLevel`, `ALL`.

```java
CatchClause errorClause = tryStmt.getCatchClause(0);
assertEquals("Error", errorClause.getKind().toString());

StatementVariableDeclaration catchDecl = errorClause.getCatchDeclaration();
assertEquals("string memory reason", catchDecl.toString());
assertSame(STRING, catchDecl.getProgramVariable().getType());

assertEquals("catch Error(string memory reason) {\nint j;\n}\n", errorClause.toString());

CatchClause allClause = tryStmt.getCatchClause(1);
assertEquals("ALL", allClause.getKind().toString());
```

---

## References

### `FunctionReference`

A late-bound reference to a `FunctionDeclaration`. Its type is the function's `TupleType`.

```java
FunctionReference fRef = ...;
assertSame(selfF, fRef.referencedDeclaration);
assertInstanceOf(TupleType.class, fRef.getType());
```

### `ContractReference`

Reference to a contract type (e.g. `SimpleContract(target)` in an external call). Leaf node.

```java
assertEquals(0, contr.getChildCount());
```

### `ModifierReference`

Holds the modifier name string. Leaf node.

```java
assertEquals("mod1", modRefs.get(0).name);
assertEquals("mod1", modRefs.get(0).toString());
assertEquals(0, modRefs.get(0).getChildCount());
```

### `TypeReference`

Forward reference to a type (e.g. a struct type used before it is fully resolved). Holds both a `typeName` and a `referencedType` after resolution.

```java
assertEquals("int", field.getTypeReference().getTypeName().toString());
assertEquals("SimpleContract.Account",
    accountField.getTypeReference().referencedType.name().toString());
```

---

## Visitor Pattern

Implement `SolidityASTVisitor` to traverse the AST. Each node calls the corresponding `performActionOn*` method:

```java
public class MyVisitor extends SolidityASTVisitor {
    @Override
    public void performActionOnBlock(Block x) { ... }

    @Override
    public void performActionOnFunctionCallExpression(FunctionCallExpression x) { ... }

    @Override
    public void performActionOnReturnStatment(ReturnStatement x) { ... }

    @Override
    public void performActionOnConditionStatement(ConditionStatement x) { ... }

    @Override
    public void performActionOnWhileStatement(WhileStatement x) { ... }

    @Override
    public void performActionOnForStatement(ForStatement x) { ... }

    @Override
    public void performActionOnDoWhileStatement(DoWhileStatement x) { ... }

    @Override
    public void performActionOnAssignmentExpression(AssignmentExpression x) { ... }

    @Override
    public void performActionOnMemberExp(MemberExp x) { ... }

    @Override
    public void performActionOnBoolLiteral(BoolLiteral x) { ... }
    // ...
}
```

---

## Class Hierarchy

```
SyntaxElement (KeY framework)
│
├── Type (interface)
│   ├── PrimitiveType               — INT, UINT256, BOOL, ADDRESS, …
│   ├── ArrayType                   — T[n]
│   ├── DynamicArrayType            — T[]
│   ├── MappingType                 — mapping(K => V)
│   ├── TupleType                   — (T1, T2, …)
│   ├── ContractDeclaration         — contract Foo { … }
│   ├── StructDeclaration           — struct S { … }
│   └── EnumDeclaration             — enum E { … }
│
├── Declaration (interface)
│   ├── ContractDeclaration
│   ├── FunctionDeclaration
│   ├── StateVariableDeclaration
│   ├── StructDeclaration
│   ├── FieldDeclaration
│   ├── EnumDeclaration
│   ├── MemberEnumDeclaration
│   ├── ModifierDeclaration
│   └── StatementVariableDeclaration
│
└── SolidityProgramElement (interface)
    │
    ├── Expression (interface)
    │   └── SolidityExpression (abstract)
    │       ├── Literal (abstract)
    │       │   ├── BoolLiteral
    │       │   └── Uint256Literal
    │       ├── ElementaryExpression        — type casts
    │       ├── FunctionCallExpression
    │       ├── MemberExp
    │       ├── IndexExpression
    │       ├── IndexRangeExpression        — arr[a:b]
    │       ├── NewExpression
    │       ├── TupleExpression
    │       ├── FunctionReference
    │       ├── BinaryOperator (abstract)
    │       │   ├── AddOperator, SubtractionOperator, MultiplicationOperator
    │       │   ├── DivOperator, ModOperator, ExponentialOperator
    │       │   ├── EqualOperator, UnequalOperator
    │       │   ├── LessOperator, LessEqualOperator, GreaterOperator, GreaterEqualOperator
    │       │   ├── AndOperator, OrOperator
    │       │   ├── BitwiseAndOperator, BitwiseOrOperator, BitwiseXorOperator
    │       │   ├── LeftShiftOperator, RightShiftOperator
    │       │   └── AssignmentExpression, PlusEqualOperator, MinusEqualOperator, …
    │       ├── UnaryOperator (abstract)
    │       │   ├── PlusPlusOperator, MinusMinusOperator
    │       │   ├── NegateOperator, NotOperator, BitwiseNotOperator, DeleteOperator
    │       └── TernaryOperator
    │
    └── Statement (interface)
        ├── Block
        ├── ExpressionStatement
        ├── DeclarationStatement
        ├── ReturnStatement
        ├── ConditionStatement
        ├── LoopStatement (abstract)
        │   ├── WhileStatement
        │   ├── DoWhileStatement
        │   └── ForStatement
        ├── BreakStatement
        ├── ContinueStatement
        ├── TryStatement
        └── PlaceholdStatement
```

---

## Examples

### Simple contract

```solidity
contract SimpleContract {
    uint256 balance;
}
```

```
ContractDeclaration "SimpleContract"
└── StateVariableDeclaration
    └── ProgramVariable "balance" : UINT256 @ Storage
```

### State variable with initializer

```solidity
contract SimpleContract {
    uint256 balance = 1000;
    bool closed = true;
}
```

```
ContractDeclaration "SimpleContract"
├── StateVariableDeclaration
│   ├── ProgramVariable "balance" : UINT256 @ Storage
│   └── Uint256Literal 1000
└── StateVariableDeclaration
    ├── ProgramVariable "closed" : BOOL @ Storage
    └── BoolLiteral TRUE
```

### Function with body

```solidity
contract SimpleContract {
    function func(uint256 v) public pure returns(uint256) {
        return v;
    }
}
```

```
ContractDeclaration "SimpleContract"
└── FunctionDeclaration "func" [public pure]
    ├── ProgramVariable "_ret0" : UINT256   (return param)
    ├── ProgramVariable "v" : UINT256       (input param)
    └── Block
        └── ReturnStatement
            └── ProgramVariable "v"
```

### Member access on nested structs

```solidity
alice.account.balance = 10;
```

```
AssignmentExpression
├── MemberExp
│   ├── MemberExp
│   │   ├── ProgramVariable "alice"
│   │   └── FieldDeclaration "account" : SimpleContract.Account
│   └── FieldDeclaration "balance" : uint256
└── Uint256Literal 10
```

### Try-catch with return value

```solidity
try SimpleContract(target).g() returns (int a) {
    int b = a;
} catch { }
```

```
TryStatement
├── MemberExp                          (child 0 — the tried expression)
│   ├── FunctionCallExpression
│   │   └── ContractReference
│   └── FunctionReference "g"
├── ProgramVariable "a" : INT          (child 1 — return declaration)
├── Block                              (child 2 — success body)
│   └── DeclarationStatement
│       ├── StatementVariableDeclaration "b" : INT
│       └── ProgramVariable "a"        (same object as return decl)
└── CatchClause [ALL]                  (child 3)
    └── Block (empty)
```

### Enum usage

```solidity
enum State { Begin, End }
State s = State.Begin;
```

```
EnumDeclaration "State"
├── MemberEnumDeclaration "Begin"
└── MemberEnumDeclaration "End"

DeclarationStatement
├── StatementVariableDeclaration
│   └── ProgramVariable "s" : EnumDeclaration("State")
└── MemberExp
    ├── EnumReference "State"
    └── MemberEnumDeclaration "Begin"
```
