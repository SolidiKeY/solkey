# The solc AST (`--ast-compact-json`)

Read this when working on `SolJSONParser`
(`keyext.solidity.core/src/main/java/org/key_project/solidity/program/parser/SolJSONParser.java`),
which turns `solc`'s compact JSON AST into this fork's `program/ast` nodes.

The format itself is documented upstream and is not reproduced here:
<https://docs.soliditylang.org/en/latest/using-the-compiler.html#compiler-input-and-output-json-description>.

## Generating one

```bash
solc --ast-compact-json keyext.solidity.examples/TestSuite.sol
solc --ast-compact-json --pretty-json Contract.sol | less
```

Every node carries `id`, `nodeType` and `src` (`"byteOffset:byteLength:sourceIndex"`). Expression
nodes additionally carry `typeDescriptions` with `typeIdentifier` and `typeString`; the parser
reads `typeString` (`SolJSONParser.getPrimitiveType`) rather than the mangled identifier.

## What the parser accepts

The parser dispatches on `nodeType` at four places. Anything not listed below raises
`SolidityParseException` — that is the checklist to extend when a new construct is needed.

| Dispatch site | `nodeType` values handled |
|---|---|
| Source unit (`parse`, `parseSourceUnit`) | `SourceUnit`, `ContractDefinition` |
| Contract members (`parseContract`) | `VariableDeclaration`, `FunctionDefinition`, `StructDefinition`, `ModifierDefinition`, `EnumDefinition` |
| Statements (`parseStatement`) | `ExpressionStatement`, `Return`, `IfStatement`, `WhileStatement`, `DoWhileStatement`, `ForStatement`, `TryStatement`, `Continue`, `Break`, `PlaceholderStatement`, plus blocks (any node with `statements`) and declarations (any node with `declarations`) |
| Type names (`parseTypeName`) | `ElementaryTypeName`, `ArrayTypeName`, `Mapping`, `UserDefinedTypeName`, `Identifier` |
| Expressions (`parseExpression`) | `Literal`, `Identifier`, `BinaryOperation`, `UnaryOperation`, `Assignment`, `MemberAccess`, `IndexAccess`, `IndexRangeAccess`, `Conditional`, `TupleExpression`, `FunctionCall`, `ElementaryTypeNameExpression`, `NewExpression`, `ExpressionStatement` |

Note that a block and a variable-declaration statement are recognised by the presence of the
`statements` / `declarations` field, not by their `nodeType`, so they do not appear as `case`
labels in the source.

## Deliberately unhandled

`PragmaDirective` and `ImportDirective` are skipped at the source-unit level. Everything else the
compiler can emit — `UsingForDirective`, `InheritanceSpecifier`, `EventDefinition`,
`ErrorDefinition`, `EmitStatement`, `RevertStatement`, `ModifierInvocation`, `FunctionTypeName`,
`UserDefinedValueTypeDefinition`, `InlineAssembly` and the whole Yul sub-AST — is unsupported and
fails loudly.

## Two rewrites worth knowing

- `msg.sender` and `msg.value` are rewritten in `parseMemberAccess` to the identifiers `msgSender`
  and `msgValue`, which is what the payment rules in `solidityProgramRules.key` match on. See
  `docs/net.md`.
- `Literal` values are read from `kind` (`number`, `bool`, …), not from `value` alone.

Tests: `keyext.solidity.core/src/test/java/org/key_project/solidity/logic/parser/SolJsonParserTest.java`.
