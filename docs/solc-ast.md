# The solc AST (`--ast-compact-json`)

Read this when working on `SolJSONParser`
(`keyext.solidity.core/src/main/java/org/key_project/solidity/program/parser/SolJSONParser.java`),
which turns `solc`'s compact JSON AST into this fork's `program/ast` nodes.

The format itself is documented upstream and is not reproduced here:
<https://docs.soliditylang.org/en/latest/using-the-compiler.html#compiler-input-and-output-json-description>.

## Where it comes from

`SolcWrapper` produces it. solc is not forked: `WasmSolcCompiler` runs `soljson.js` — the
compiler's official WebAssembly build, the one `solc-js` ships — on GraalJS with GraalWasm
underneath, so the prover needs nothing but a JVM. `keyext.solidity.core/build.gradle` pins the
version (`soljsonFile`) and downloads it into the jar as the resource `/soljson.js`.

The request is Standard JSON asking for `ast`, whose nodes are the format `--ast-compact-json`
prints, so anything written about that format still applies. Two details of the WebAssembly build
are ironed out in `SolcWrapper`: the 32-bit target exports the negative ids of builtin
declarations (`require` is -18) as their unsigned complement, and the compilation unit is named by
its absolute path rather than relative to the working directory.

To look at one by hand, an external `solc` still prints the same thing:

```bash
solc --ast-compact-json --pretty-json Contract.sol | less
```

Truffle interprets the compiler unless the Graal compiler is in the boot layer, which costs about
a factor of ten; `run-key.sh` and the Gradle tasks add `-XX:+EnableJVMCI` and the
`--upgrade-module-path` for it when the JDK accepts them.

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
