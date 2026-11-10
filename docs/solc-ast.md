# The solc AST (`--ast-compact-json`)

Read this when working on `SolJSONParser`
(`keyext.solidity.core/src/main/java/org/key_project/solidity/program/parser/SolJSONParser.java`),
which turns `solc`'s compact JSON AST into this fork's `program/ast` nodes.

The format itself is documented upstream and is not reproduced here:
<https://docs.soliditylang.org/en/latest/using-the-compiler.html#compiler-input-and-output-json-description>.

## Where it comes from

`SolcWrapper` produces it, with one of two compilers of the same pinned release
(`solcLongVersion` in `keyext.solidity.core/build.gradle`, also written as the resource
`/solc.version`):

- **`NativeSolcCompiler`**, preferred: the native `solc`, forked with `--standard-json
  --no-import-callback`. About fifty times faster: `TestSuite.sol` compiles to bytecode in 0.3 s
  instead of 17 s, so `--solc` (the IDEA *Compile and Run* entry) takes about a second cold. It is
  looked up as `-Dsolkey.solc=PATH` (`off` disables it), `SOLKEY_SOLC`, `solc-native/solc` beside
  the jar, then `solc` on the `PATH`; a candidate whose `--version` is not the pinned release is
  skipped. `shadowJar` stages it beside both fat jars (`downloadNativeSolc`, sha256-checked, for
  linux-amd64, macOS and windows-amd64); offline it only warns.
- **`WasmSolcCompiler`**, the fallback: runs `soljson.js` — the compiler's official WebAssembly
  build, the one `solc-js` ships — on GraalJS with GraalWasm underneath, so the prover needs
  nothing but a JVM. `downloadSoljson` puts it in the jar as the resource `/soljson.js`. Tests
  always use it (`SolcCacheExtension`).

Both produce the same AST for every example under `keyext.solidity.examples`.

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
`--upgrade-module-path` for it when the JDK accepts them. The context runs Truffle with
`engine.Mode=latency`: every run is a fresh JVM, and that mode cuts a cold compile of
`TestSuite.sol` from ~40 s to ~26 s.

Every JVM would still pay that cold compile, so both compilers keep solc's outputs on disk
(`SolcOutputCache`), keyed by the compiler (the SHA-256 of `soljson.js`, or the native version
string) and the SHA-256 of the exact Standard JSON input,
in `$XDG_CACHE_HOME/solkey/solc` (else `~/.cache/solkey/solc`), newest 256 kept. A run on an
unchanged file never starts solc. `-Dsolkey.solcCache=DIR` moves it; `-Dsolkey.solcCache=off`
disables it. `--solc` makes one request (AST plus bytecode) that serves the diagnostics, the
outline and the EVM run.

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
| Expressions (`parseExpression`) | `Literal`, `Identifier`, `BinaryOperation`, `UnaryOperation`, `Assignment`, `MemberAccess`, `IndexAccess`, `IndexRangeAccess`, `Conditional`, `TupleExpression` (a parenthesized expression is unwrapped; a tuple of several components becomes a `TupleExpression`, which only `return` and tuple assignments consume; an inline array is rejected), `FunctionCall`, `ElementaryTypeNameExpression`, `NewExpression`, `ExpressionStatement` |

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
