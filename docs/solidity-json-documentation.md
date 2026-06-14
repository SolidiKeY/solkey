# Solidity AST JSON Export Documentation

This document describes the JSON format produced by the Solidity compiler's AST export feature (`--ast-compact-json`). It is intended for tools that consume this JSON to reconstruct or analyze the AST.

## Table of Contents

1. [Overview](#overview)
2. [How to Generate](#how-to-generate)
3. [Top-Level Structure](#top-level-structure)
4. [Universal Node Fields](#universal-node-fields)
5. [Source Location Encoding](#source-location-encoding)
6. [Type Description System](#type-description-system)
7. [Node Types Reference](#node-types-reference)
   - [SourceUnit](#sourceunit)
   - [PragmaDirective](#pragmadirective)
   - [ImportDirective](#importdirective)
   - [ContractDefinition](#contractdefinition)
   - [InheritanceSpecifier](#inheritancespecifier)
   - [UsingForDirective](#usingfordirective)
   - [StructDefinition](#structdefinition)
   - [EnumDefinition & EnumValue](#enumdefinition--enumvalue)
   - [UserDefinedValueTypeDefinition](#userdefinedvaluetypedefinition)
   - [FunctionDefinition](#functiondefinition)
   - [ModifierDefinition](#modifierdefinition)
   - [ModifierInvocation](#modifierinvocation)
   - [EventDefinition](#eventdefinition)
   - [ErrorDefinition](#errordefinition)
   - [VariableDeclaration](#variabledeclaration)
   - [ParameterList](#parameterlist)
   - [OverrideSpecifier](#overridespecifier)
   - [StructuredDocumentation](#structureddocumentation)
   - [StorageLayoutSpecifier](#storagelayoutspecifier)
   - [Type Names](#type-names)
   - [Statements](#statements)
   - [Expressions](#expressions)
   - [Inline Assembly (Yul)](#inline-assembly-yul)
8. [Complete Examples](#complete-examples)
9. [Notes on Optional Fields and Null Removal](#notes-on-optional-fields-and-null-removal)

---

## Overview

When you compile a Solidity file with the `--ast-compact-json` flag (or request `ast` in the JSON input's `outputSelection`), the compiler outputs one JSON object per source file. Each object represents the root `SourceUnit` node, with the entire AST as a tree of nested JSON objects.

Node IDs are globally unique integers within a compilation run. They are used to cross-reference nodes (e.g., `referencedDeclaration`, `scope`, `baseFunctions`).

---

## How to Generate

```bash
# Via CLI
solc --ast-compact-json MyContract.sol

# Via Standard JSON input (outputSelection)
{
  "language": "Solidity",
  "sources": { "MyContract.sol": { "content": "..." } },
  "settings": {
    "outputSelection": { "*": { "": ["ast"] } }
  }
}
```

The output is either:
- A single JSON object (one source file)
- A JSON array of objects (multiple source files)

---

## Top-Level Structure

The root of each source file's AST is a `SourceUnit` node:

```json
{
  "id": 6,
  "nodeType": "SourceUnit",
  "src": "0:83:0",
  "absolutePath": "/path/to/MyContract.sol",
  "license": "MIT",
  "exportedSymbols": {
    "MyContract": [5]
  },
  "nodes": [ ... ]
}
```

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `id` | integer | Unique node ID |
| `nodeType` | string | Always `"SourceUnit"` |
| `src` | string | Source location (see [Source Location Encoding](#source-location-encoding)) |
| `absolutePath` | string | The absolute path of the source file |
| `license` | string \| null | SPDX license identifier from `// SPDX-License-Identifier:` comment |
| `exportedSymbols` | object | Map from symbol name to array of node IDs it resolves to |
| `nodes` | array | Top-level declarations: pragmas, imports, contracts, free functions, etc. |
| `experimentalSolidity` | boolean | Present and `true` if `pragma experimental solidity` is used |

---

## Universal Node Fields

Every AST node includes these fields:

| Field | Type | Description |
|-------|------|-------------|
| `id` | integer | Unique identifier for this node within the compilation |
| `nodeType` | string | The kind of AST node (e.g., `"ContractDefinition"`, `"FunctionDefinition"`) |
| `src` | string | Source location: `"<start>:<length>:<fileIndex>"` |

Named nodes additionally include:

| Field | Type | Description |
|-------|------|-------------|
| `name` | string | The identifier name |
| `nameLocation` | string | Source location of just the identifier token |

Documented nodes additionally include:

| Field | Type | Description |
|-------|------|-------------|
| `documentation` | object \| (absent) | A `StructuredDocumentation` node if a NatSpec comment precedes the declaration |

---

## Source Location Encoding

The `src` field is a string with the format:

```
"<start>:<length>:<fileIndex>"
```

- **start**: Byte offset (0-indexed) from the beginning of the source file where this construct starts.
- **length**: Number of bytes the construct spans. `-1` means the location is invalid/synthetic.
- **fileIndex**: Index into the compiler's source file list (0-indexed). Matches the order files appear in the compilation unit. `-1` means no file mapping.

**Example:**

Given the source `contract C {}` (14 bytes):

```
"src": "0:13:0"   →  starts at byte 0, 13 bytes long, in file index 0
"src": "9:1:0"    →  starts at byte 9 (the 'C'), 1 byte long, in file index 0
```

When there is no valid location (e.g., auto-generated nodes), the value is `"-1:-1:-1"`.

---

## Type Description System

Expressions and type name nodes carry a `typeDescriptions` object:

```json
"typeDescriptions": {
  "typeString": "uint256",
  "typeIdentifier": "t_uint256"
}
```

- **`typeString`**: Human-readable type name as Solidity would print it.
- **`typeIdentifier`**: A unique string key for the type, used for programmatic matching.

### Common Type Identifiers

| Type | `typeIdentifier` |
|------|-----------------|
| `uint256` | `t_uint256` |
| `uint8` | `t_uint8` |
| `int256` | `t_int256` |
| `bool` | `t_bool` |
| `address` | `t_address` |
| `address payable` | `t_address_payable` |
| `bytes` | `t_bytes_storage_ptr` |
| `string` | `t_string_storage_ptr` |
| `bytes32` | `t_bytes32` |
| `uint256[]` (dynamic, memory) | `t_array$_t_uint256_$dyn_memory_ptr` |
| `uint256[3]` (static, storage) | `t_array$_t_uint256_$3_storage` |
| `mapping(address => uint256)` | `t_mapping$_t_address_$_t_uint256_$` |
| `contract MyContract` | `t_contract$_MyContract_$<id>` |
| `struct MyContract.S` | `t_struct$_S_$<id>_storage` |
| `enum MyContract.E` | `t_enum$_E_$<id>` |
| `function() external view returns (uint256)` | `t_function_external_view$__$returns$_t_uint256_$` |
| integer constant `1` | `t_rational_1_by_1` |
| integer constant `2` | `t_rational_2_by_1` |

The `$` characters act as delimiters. Nested types are recursively embedded.

---

## Node Types Reference

### SourceUnit

The root node for each compiled source file.

```json
{
  "id": 14,
  "nodeType": "SourceUnit",
  "src": "0:83:0",
  "absolutePath": "a",
  "license": null,
  "exportedSymbols": {
    "C": [13],
    "L": [4],
    "f": [10]
  },
  "nodes": [ ... ]
}
```

---

### PragmaDirective

Represents `pragma solidity ...` or `pragma experimental ...`.

```json
{
  "id": 1,
  "nodeType": "PragmaDirective",
  "src": "0:23:0",
  "literals": ["solidity", "^", "0.8", ".0"]
}
```

The `literals` array contains the tokenized pragma value. For `pragma solidity ^0.8.0`, this is `["solidity", "^", "0.8", ".0"]`.

---

### ImportDirective

Represents `import "file.sol"` or `import {Foo as Bar} from "file.sol"`.

```json
{
  "id": 3,
  "nodeType": "ImportDirective",
  "src": "25:26:0",
  "file": "./other.sol",
  "absolutePath": "/project/other.sol",
  "sourceUnit": 42,
  "scope": 100,
  "unitAlias": "",
  "symbolAliases": [
    {
      "foreign": {
        "id": 1,
        "nodeType": "IdentifierPath",
        "name": "Foo",
        "nameLocations": ["33:3:0"],
        "referencedDeclaration": 10,
        "src": "33:3:0"
      },
      "local": "Bar",
      "nameLocation": "40:3:0"
    }
  ]
}
```

| Field | Description |
|-------|-------------|
| `file` | The import path as written in the source |
| `absolutePath` | Resolved absolute path (null if unresolved) |
| `sourceUnit` | Node ID of the imported `SourceUnit` (null if unresolved) |
| `scope` | ID of enclosing `SourceUnit` |
| `unitAlias` | The alias for `import "..." as Alias` syntax; empty string if none |
| `symbolAliases` | Array of imported symbol aliases; empty for bare imports |

---

### ContractDefinition

Represents a `contract`, `library`, or `interface`.

```json
{
  "id": 5,
  "nodeType": "ContractDefinition",
  "src": "0:34:0",
  "name": "C",
  "nameLocation": "9:1:0",
  "abstract": false,
  "contractKind": "contract",
  "canonicalName": "C",
  "fullyImplemented": true,
  "linearizedBaseContracts": [5],
  "baseContracts": [],
  "contractDependencies": [],
  "nodes": [ ... ],
  "scope": 6,
  "usedEvents": [],
  "usedErrors": [],
  "internalFunctionIDs": {}
}
```

| Field | Type | Description |
|-------|------|-------------|
| `contractKind` | string | `"contract"`, `"library"`, or `"interface"` |
| `abstract` | boolean | `true` if declared `abstract` |
| `canonicalName` | string | Fully qualified name (same as `name` for top-level contracts) |
| `fullyImplemented` | boolean | `false` if any function is unimplemented (abstract) |
| `linearizedBaseContracts` | integer[] | C3-linearized list of base contract IDs (most derived first) |
| `baseContracts` | array | `InheritanceSpecifier` nodes for direct base contracts |
| `contractDependencies` | integer[] | IDs of contracts this contract depends on |
| `nodes` | array | Member declarations (functions, variables, events, etc.) |
| `scope` | integer | ID of enclosing `SourceUnit` |
| `usedEvents` | integer[] | IDs of all `EventDefinition` nodes emitted by this contract |
| `usedErrors` | integer[] | IDs of all `ErrorDefinition` nodes used by this contract |
| `internalFunctionIDs` | object | Map of `{functionNodeId: internalDispatchId}` for internal function pointers (optional) |
| `storageLayout` | object \| (absent) | `StorageLayoutSpecifier` node if layout is annotated |
| `documentation` | object \| (absent) | NatSpec comment |

---

### InheritanceSpecifier

Appears within `ContractDefinition.baseContracts`. Represents one base in `contract C is Base1, Base2`.

```json
{
  "id": 3,
  "nodeType": "InheritanceSpecifier",
  "src": "30:2:0",
  "baseName": {
    "id": 2,
    "nodeType": "IdentifierPath",
    "name": "C1",
    "nameLocations": ["30:2:0"],
    "referencedDeclaration": 1,
    "src": "30:2:0"
  },
  "arguments": null
}
```

`arguments` is an array of `Expression` nodes for constructor arguments (e.g., `is Base(42)`), or `null`/absent if none.

---

### UsingForDirective

Represents `using L for T` or `using {f, g} for T`.

**Library style** (`using L for *` inside a contract):
```json
{
  "id": 12,
  "nodeType": "UsingForDirective",
  "src": "66:14:0",
  "global": false,
  "libraryName": {
    "id": 11,
    "nodeType": "IdentifierPath",
    "name": "L",
    "nameLocations": ["72:1:0"],
    "referencedDeclaration": 4,
    "src": "72:1:0"
  }
}
```

**Function list style** (`using {f} for uint` at file level):
```json
{
  "id": 3,
  "nodeType": "UsingForDirective",
  "src": "0:19:0",
  "global": false,
  "typeName": {
    "id": 2,
    "nodeType": "ElementaryTypeName",
    "name": "uint",
    "src": "14:4:0",
    "typeDescriptions": { "typeIdentifier": "t_uint256", "typeString": "uint256" }
  },
  "functionList": [
    {
      "function": {
        "id": 1,
        "nodeType": "IdentifierPath",
        "name": "f",
        "nameLocations": ["7:1:0"],
        "referencedDeclaration": 10,
        "src": "7:1:0"
      }
    }
  ]
}
```

For operator overloads, each entry in `functionList` may have `"definition"` (IdentifierPath) and `"operator"` (string) instead of `"function"`.

| Field | Description |
|-------|-------------|
| `global` | `true` if `using ... for ... global` |
| `typeName` | The type this directive applies to; absent when `using L for *` |
| `libraryName` | IdentifierPath to the library (old-style only) |
| `functionList` | Array of function/operator bindings (new-style `{...}` syntax) |

---

### StructDefinition

```json
{
  "id": 5,
  "nodeType": "StructDefinition",
  "src": "13:32:0",
  "name": "S",
  "nameLocation": "20:1:0",
  "canonicalName": "C.S",
  "visibility": "public",
  "scope": 10,
  "members": [
    {
      "id": 2,
      "nodeType": "VariableDeclaration",
      "src": "24:8:0",
      "name": "x",
      "nameLocation": "31:1:0",
      "constant": false,
      "mutability": "mutable",
      "stateVariable": false,
      "storageLocation": "default",
      "visibility": "internal",
      "scope": 5,
      "typeName": {
        "id": 1,
        "nodeType": "ElementaryTypeName",
        "src": "24:6:0",
        "name": "uint256",
        "typeDescriptions": { "typeIdentifier": "t_uint256", "typeString": "uint256" }
      },
      "typeDescriptions": { "typeIdentifier": "t_uint256", "typeString": "uint256" }
    }
  ]
}
```

---

### EnumDefinition & EnumValue

```json
{
  "id": 4,
  "nodeType": "EnumDefinition",
  "src": "17:18:0",
  "name": "E",
  "nameLocation": "22:1:0",
  "canonicalName": "C.E",
  "members": [
    { "id": 1, "nodeType": "EnumValue", "src": "26:1:0", "name": "A", "nameLocation": "26:1:0" },
    { "id": 2, "nodeType": "EnumValue", "src": "29:1:0", "name": "B", "nameLocation": "29:1:0" },
    { "id": 3, "nodeType": "EnumValue", "src": "32:1:0", "name": "C", "nameLocation": "32:1:0" }
  ]
}
```

---

### UserDefinedValueTypeDefinition

Represents `type MyType is uint256`.

```json
{
  "id": 3,
  "nodeType": "UserDefinedValueTypeDefinition",
  "src": "0:23:0",
  "name": "MyType",
  "nameLocation": "5:6:0",
  "canonicalName": "MyType",
  "underlyingType": {
    "id": 2,
    "nodeType": "ElementaryTypeName",
    "src": "15:7:0",
    "name": "uint256",
    "typeDescriptions": { "typeIdentifier": "t_uint256", "typeString": "uint256" }
  }
}
```

---

### FunctionDefinition

Represents regular functions, constructors, fallback functions, receive functions, and free functions.

```json
{
  "id": 11,
  "nodeType": "FunctionDefinition",
  "src": "13:40:0",
  "name": "f",
  "nameLocation": "22:1:0",
  "kind": "function",
  "visibility": "public",
  "stateMutability": "nonpayable",
  "virtual": false,
  "implemented": true,
  "functionSelector": "26121ff0",
  "scope": 12,
  "parameters": {
    "id": 1,
    "nodeType": "ParameterList",
    "src": "23:2:0",
    "parameters": []
  },
  "returnParameters": {
    "id": 2,
    "nodeType": "ParameterList",
    "src": "33:0:0",
    "parameters": []
  },
  "modifiers": [],
  "overrides": null,
  "body": {
    "id": 10,
    "nodeType": "Block",
    "src": "33:20:0",
    "statements": [ ... ]
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `kind` | string | `"function"`, `"constructor"`, `"fallback"`, `"receive"`, or `"freeFunction"` |
| `visibility` | string | `"public"`, `"private"`, `"internal"`, `"external"` |
| `stateMutability` | string | `"pure"`, `"view"`, `"nonpayable"`, `"payable"` |
| `virtual` | boolean | `true` if declared `virtual` |
| `implemented` | boolean | `false` for abstract functions (no body) |
| `functionSelector` | string | 4-byte hex selector for external/public functions (absent for internal/private) |
| `parameters` | ParameterList | Input parameters |
| `returnParameters` | ParameterList | Output parameters |
| `modifiers` | array | `ModifierInvocation` nodes |
| `overrides` | OverrideSpecifier \| null | Present if `override` keyword used |
| `body` | Block \| null | `null` for abstract functions |
| `scope` | integer | ID of enclosing contract or source unit |
| `baseFunctions` | integer[] | IDs of overridden base functions (if overriding) |
| `documentation` | object \| (absent) | NatSpec comment |

**Constructor example** (note: `name` is `""` and `nameLocation` is `"-1:-1:-1"`):
```json
{
  "id": 4,
  "nodeType": "FunctionDefinition",
  "src": "14:18:0",
  "name": "",
  "nameLocation": "-1:-1:-1",
  "kind": "constructor",
  "visibility": "public",
  "stateMutability": "nonpayable",
  "virtual": false,
  "implemented": true,
  "parameters": { "id": 1, "nodeType": "ParameterList", "src": "25:2:0", "parameters": [] },
  "returnParameters": { "id": 2, "nodeType": "ParameterList", "src": "28:0:0", "parameters": [] },
  "modifiers": [],
  "body": { "id": 3, "nodeType": "Block", "src": "28:4:0", "statements": [] },
  "scope": 5
}
```

---

### ModifierDefinition

```json
{
  "id": 6,
  "nodeType": "ModifierDefinition",
  "src": "13:25:0",
  "name": "M",
  "nameLocation": "22:1:0",
  "visibility": "internal",
  "virtual": false,
  "parameters": {
    "id": 3,
    "nodeType": "ParameterList",
    "src": "23:8:0",
    "parameters": [
      {
        "id": 2,
        "nodeType": "VariableDeclaration",
        "src": "24:6:0",
        "name": "i",
        "nameLocation": "29:1:0",
        "constant": false,
        "mutability": "mutable",
        "stateVariable": false,
        "storageLocation": "default",
        "visibility": "internal",
        "scope": 6,
        "typeName": {
          "id": 1,
          "nodeType": "ElementaryTypeName",
          "src": "24:4:0",
          "name": "uint",
          "typeDescriptions": { "typeIdentifier": "t_uint256", "typeString": "uint256" }
        },
        "typeDescriptions": { "typeIdentifier": "t_uint256", "typeString": "uint256" }
      }
    ]
  },
  "body": {
    "id": 5,
    "nodeType": "Block",
    "src": "32:6:0",
    "statements": [
      { "id": 4, "nodeType": "PlaceholderStatement", "src": "34:1:0" }
    ]
  },
  "overrides": null,
  "baseModifiers": []
}
```

The `PlaceholderStatement` (`_`) is the placeholder where the modified function's body is inserted.

---

### ModifierInvocation

Appears in `FunctionDefinition.modifiers`. Represents applying a modifier to a function.

```json
{
  "id": 10,
  "nodeType": "ModifierInvocation",
  "src": "52:4:0",
  "kind": "modifierInvocation",
  "modifierName": {
    "id": 8,
    "nodeType": "IdentifierPath",
    "name": "M",
    "nameLocations": ["52:1:0"],
    "referencedDeclaration": 6,
    "src": "52:1:0"
  },
  "arguments": [
    {
      "id": 9,
      "nodeType": "Literal",
      "src": "54:1:0",
      "kind": "number",
      "value": "1",
      "hexValue": "31",
      "isPure": true,
      "isConstant": false,
      "isLValue": false,
      "lValueRequested": false,
      "typeDescriptions": { "typeIdentifier": "t_rational_1_by_1", "typeString": "int_const 1" }
    }
  ]
}
```

`kind` is `"modifierInvocation"` for regular modifiers or `"baseConstructorSpecifier"` when used in constructor inheritance lists.

---

### EventDefinition

```json
{
  "id": 2,
  "nodeType": "EventDefinition",
  "src": "13:10:0",
  "name": "E",
  "nameLocation": "19:1:0",
  "anonymous": false,
  "eventSelector": "92bbf6e823a631f3c8e09b1c8df90f378fb56f7fbc9701827e1ff8aad7f6a028",
  "parameters": {
    "id": 1,
    "nodeType": "ParameterList",
    "src": "20:2:0",
    "parameters": []
  }
}
```

- `eventSelector`: The 32-byte keccak256 topic hash (hex string, 64 chars, no `0x` prefix).
- `anonymous`: `true` if `event E() anonymous`.
- Event parameters may have `"indexed": true` on their `VariableDeclaration` nodes.
- `documentation`: NatSpec comment if present.

---

### ErrorDefinition

Represents `error MyError(...)`.

```json
{
  "id": 3,
  "nodeType": "ErrorDefinition",
  "src": "0:22:0",
  "name": "MyError",
  "nameLocation": "6:7:0",
  "errorSelector": "a1b2c3d4",
  "parameters": {
    "id": 2,
    "nodeType": "ParameterList",
    "src": "13:8:0",
    "parameters": [ ... ]
  }
}
```

- `errorSelector`: 4-byte ABI selector hex string.

---

### VariableDeclaration

Used for state variables, local variables, function parameters, and struct members.

```json
{
  "id": 9,
  "nodeType": "VariableDeclaration",
  "src": "40:20:0",
  "name": "a",
  "nameLocation": "59:1:0",
  "constant": false,
  "mutability": "mutable",
  "stateVariable": true,
  "storageLocation": "default",
  "visibility": "internal",
  "scope": 23,
  "typeName": { ... },
  "typeDescriptions": {
    "typeIdentifier": "t_mapping$_t_address_$_t_bool_$",
    "typeString": "mapping(address => bool)"
  },
  "value": null,
  "overrides": null
}
```

| Field | Type | Description |
|-------|------|-------------|
| `constant` | boolean | `true` if declared `constant` |
| `mutability` | string | `"mutable"`, `"immutable"`, or `"constant"` |
| `stateVariable` | boolean | `true` if this is a contract-level state variable |
| `storageLocation` | string | `"default"`, `"memory"`, `"storage"`, `"calldata"`, or `"transient"` |
| `visibility` | string | `"public"`, `"private"`, `"internal"`, `"external"` |
| `scope` | integer | ID of the enclosing function, modifier, or contract |
| `typeName` | TypeName node | The type of this variable |
| `typeDescriptions` | object | Type info after analysis |
| `value` | Expression \| (absent) | Initializer expression |
| `indexed` | boolean | `true` for indexed event parameters |
| `functionSelector` | string | 4-byte hex selector for public state variable getter |
| `overrides` | OverrideSpecifier \| (absent) | If `override` is present |
| `baseFunctions` | integer[] | IDs of overridden base functions (for public variable getters) |
| `documentation` | object \| (absent) | NatSpec comment for state variables |

---

### ParameterList

A container for function/event/error parameters.

```json
{
  "id": 3,
  "nodeType": "ParameterList",
  "src": "23:8:0",
  "parameters": [
    { ... VariableDeclaration ... }
  ]
}
```

---

### OverrideSpecifier

```json
{
  "id": 5,
  "nodeType": "OverrideSpecifier",
  "src": "30:8:0",
  "overrides": [
    {
      "id": 4,
      "nodeType": "UserDefinedTypeName",
      "src": "39:4:0",
      "pathNode": { ... },
      "referencedDeclaration": 1,
      "typeDescriptions": { ... }
    }
  ]
}
```

The `overrides` array lists the specific contracts being overridden. It is empty for plain `override` without a list.

---

### StructuredDocumentation

Represents a NatSpec comment (`///` or `/** */`).

```json
{
  "id": 1,
  "nodeType": "StructuredDocumentation",
  "src": "0:27:0",
  "text": "This contract is empty"
}
```

The `text` field contains the stripped comment text (without `///` or `/** */` delimiters). Multi-line comments preserve internal newlines.

---

### StorageLayoutSpecifier

Represents an explicit storage layout annotation (experimental feature).

```json
{
  "id": 3,
  "nodeType": "StorageLayoutSpecifier",
  "src": "...",
  "baseSlotExpression": { ... Expression ... }
}
```

---

## Type Names

Type name nodes describe the type of a variable or parameter as written in source.

### ElementaryTypeName

For built-in types: `uint`, `int`, `bool`, `address`, `bytes`, `string`, `bytesN`, `uintN`, `intN`.

```json
{
  "id": 1,
  "nodeType": "ElementaryTypeName",
  "src": "24:4:0",
  "name": "uint",
  "typeDescriptions": {
    "typeIdentifier": "t_uint256",
    "typeString": "uint256"
  }
}
```

For `address payable`, the node includes `"stateMutability": "payable"`.

### UserDefinedTypeName

For references to contracts, structs, enums, and user-defined value types.

```json
{
  "id": 6,
  "nodeType": "UserDefinedTypeName",
  "src": "48:1:0",
  "pathNode": {
    "id": 5,
    "nodeType": "IdentifierPath",
    "name": "C",
    "nameLocations": ["48:1:0"],
    "referencedDeclaration": 23,
    "src": "48:1:0"
  },
  "referencedDeclaration": 23,
  "typeDescriptions": {
    "typeIdentifier": "t_contract$_C_$23",
    "typeString": "contract C"
  }
}
```

### ArrayTypeName

For array types like `uint[]` or `uint[3]`.

```json
{
  "id": 4,
  "nodeType": "ArrayTypeName",
  "src": "0:9:0",
  "baseType": {
    "id": 2,
    "nodeType": "ElementaryTypeName",
    "src": "0:7:0",
    "name": "uint256",
    "typeDescriptions": { "typeIdentifier": "t_uint256", "typeString": "uint256" }
  },
  "length": null,
  "typeDescriptions": {
    "typeIdentifier": "t_array$_t_uint256_$dyn_storage_ptr",
    "typeString": "uint256[]"
  }
}
```

`length` is an `Expression` node for fixed-size arrays (e.g., `uint[3]`), or absent/`null` for dynamic arrays.

### Mapping

For `mapping(KeyType => ValueType)`.

```json
{
  "id": 12,
  "nodeType": "Mapping",
  "src": "66:24:0",
  "keyType": {
    "id": 10,
    "nodeType": "ElementaryTypeName",
    "src": "74:7:0",
    "name": "address",
    "typeDescriptions": { "typeIdentifier": "t_address", "typeString": "address" }
  },
  "keyName": "",
  "keyNameLocation": "-1:-1:-1",
  "valueType": {
    "id": 11,
    "nodeType": "ElementaryTypeName",
    "src": "85:4:0",
    "name": "bool",
    "typeDescriptions": { "typeIdentifier": "t_bool", "typeString": "bool" }
  },
  "valueName": "",
  "valueNameLocation": "-1:-1:-1",
  "typeDescriptions": {
    "typeIdentifier": "t_mapping$_t_address_$_t_bool_$",
    "typeString": "mapping(address => bool)"
  }
}
```

For named mapping parameters (e.g., `mapping(address keyAddress => uint256 value)`), `keyName`, `keyNameLocation`, `valueName`, `valueNameLocation` are populated.

### FunctionTypeName

For function type variables: `function(uint) external returns (bool)`.

```json
{
  "id": 8,
  "nodeType": "FunctionTypeName",
  "src": "14:40:0",
  "visibility": "external",
  "stateMutability": "payable",
  "parameterTypes": {
    "id": 4,
    "nodeType": "ParameterList",
    "src": "23:0:0",
    "parameters": []
  },
  "returnParameterTypes": {
    "id": 6,
    "nodeType": "ParameterList",
    "src": "43:7:0",
    "parameters": [ ... ]
  },
  "typeDescriptions": {
    "typeIdentifier": "t_function_external_payable$__$returns$_t_uint256_$",
    "typeString": "function () external payable returns (uint256)"
  }
}
```

### IdentifierPath

A dot-separated qualified name used in inheritance, using-for, and type references.

```json
{
  "id": 5,
  "nodeType": "IdentifierPath",
  "src": "48:1:0",
  "name": "C",
  "nameLocations": ["48:1:0"],
  "referencedDeclaration": 23
}
```

For multi-segment paths like `A.B.C`, `name` is `"A.B.C"` and `nameLocations` contains one `src` string per segment.

---

## Statements

All statement nodes include the universal fields (`id`, `nodeType`, `src`).

### Block / UncheckedBlock

```json
{
  "id": 10,
  "nodeType": "Block",
  "src": "33:20:0",
  "statements": [ ... ]
}
```

`UncheckedBlock` has `"nodeType": "UncheckedBlock"` and the same structure.

### IfStatement

```json
{
  "id": 10,
  "nodeType": "IfStatement",
  "src": "...",
  "condition": { ... Expression ... },
  "trueBody": { ... Statement ... },
  "falseBody": { ... Statement or null ... }
}
```

### WhileStatement / DoWhileStatement

```json
{
  "id": 5,
  "nodeType": "WhileStatement",
  "src": "...",
  "condition": { ... Expression ... },
  "body": { ... Statement ... }
}
```

### ForStatement

```json
{
  "id": 10,
  "nodeType": "ForStatement",
  "src": "...",
  "initializationExpression": { ... ExpressionStatement or VariableDeclarationStatement ... },
  "condition": { ... Expression ... },
  "loopExpression": { ... ExpressionStatement ... },
  "body": { ... Statement ... },
  "isSimpleCounterLoop": false
}
```

All three loop parts (`initializationExpression`, `condition`, `loopExpression`) may be absent if omitted in source.

### TryStatement / TryCatchClause

```json
{
  "id": 20,
  "nodeType": "TryStatement",
  "src": "...",
  "externalCall": { ... FunctionCall expression ... },
  "clauses": [
    {
      "id": 10,
      "nodeType": "TryCatchClause",
      "src": "...",
      "errorName": "",
      "parameters": null,
      "block": { ... Block ... }
    },
    {
      "id": 18,
      "nodeType": "TryCatchClause",
      "src": "...",
      "errorName": "Error",
      "parameters": { ... ParameterList ... },
      "block": { ... Block ... }
    }
  ]
}
```

### VariableDeclarationStatement

```json
{
  "id": 6,
  "nodeType": "VariableDeclarationStatement",
  "src": "35:10:0",
  "assignments": [4],
  "declarations": [
    {
      "id": 4,
      "nodeType": "VariableDeclaration",
      "src": "35:6:0",
      ...
    }
  ],
  "initialValue": {
    "id": 5,
    "nodeType": "Literal",
    "src": "44:1:0",
    "kind": "number",
    "value": "2",
    "hexValue": "32",
    ...
  }
}
```

For tuple destructuring (`(uint x, uint y) = f()`), `assignments` may contain `null` entries for omitted slots, and `declarations` may contain `null` for those slots too.

### ExpressionStatement

```json
{
  "id": 9,
  "nodeType": "ExpressionStatement",
  "src": "47:3:0",
  "expression": { ... Expression ... }
}
```

### Return

```json
{
  "id": 5,
  "nodeType": "Return",
  "src": "...",
  "expression": { ... Expression or null ... },
  "functionReturnParameters": 2
}
```

`functionReturnParameters` is the node ID of the return `ParameterList` of the enclosing function.

### EmitStatement

```json
{
  "id": 5,
  "nodeType": "EmitStatement",
  "src": "...",
  "eventCall": { ... FunctionCall ... }
}
```

### RevertStatement

```json
{
  "id": 5,
  "nodeType": "RevertStatement",
  "src": "...",
  "errorCall": { ... FunctionCall ... }
}
```

### Simple Control Flow

These nodes have no additional fields beyond the universals:

```json
{ "id": 3, "nodeType": "Break", "src": "..." }
{ "id": 3, "nodeType": "Continue", "src": "..." }
{ "id": 3, "nodeType": "PlaceholderStatement", "src": "..." }
```

`Throw` is legacy Solidity and similarly has no extra fields.

### InlineAssembly

See the [Inline Assembly (Yul)](#inline-assembly-yul) section below.

---

## Expressions

All expression nodes include `typeDescriptions` and a set of boolean annotation fields after the analysis phase:

| Field | Type | Description |
|-------|------|-------------|
| `typeDescriptions` | object | `{typeString, typeIdentifier}` |
| `isConstant` | boolean | Whether the expression is a constant |
| `isLValue` | boolean | Whether the expression can be assigned to |
| `isPure` | boolean | Whether the expression has no side-effects and depends only on its inputs |
| `lValueRequested` | boolean | Whether this expression is used as an lvalue (i.e., on the left side of assignment) |
| `argumentTypes` | array | (optional) Types of arguments for call-like contexts |

### Identifier

A reference to a declared name.

```json
{
  "id": 7,
  "nodeType": "Identifier",
  "src": "47:1:0",
  "name": "x",
  "referencedDeclaration": 4,
  "overloadedDeclarations": [],
  "typeDescriptions": { "typeIdentifier": "t_uint256", "typeString": "uint256" },
  "isConstant": false,
  "isLValue": true,
  "isPure": false,
  "lValueRequested": false
}
```

`overloadedDeclarations` lists all overload IDs for overloaded functions (usually empty).

### Literal

```json
{
  "id": 5,
  "nodeType": "Literal",
  "src": "44:1:0",
  "kind": "number",
  "value": "2",
  "hexValue": "32",
  "subdenomination": null,
  "typeDescriptions": { "typeIdentifier": "t_rational_2_by_1", "typeString": "int_const 2" },
  "isConstant": false,
  "isLValue": false,
  "isPure": true,
  "lValueRequested": false
}
```

| `kind` value | Description |
|-------------|-------------|
| `"number"` | Numeric literal (integer or decimal) |
| `"string"` | Regular string literal |
| `"unicodeString"` | `unicode"..."` literal |
| `"hexString"` | `hex"..."` literal |
| `"bool"` | `true` or `false` |

- `value`: The string representation of the literal value.
- `hexValue`: The hex encoding of the raw bytes (for all literal kinds).
- `subdenomination`: For number literals with units: `"wei"`, `"gwei"`, `"ether"`, `"seconds"`, `"minutes"`, `"hours"`, `"days"`, `"weeks"`, `"years"`. Absent or `null` if no unit.

### UnaryOperation

```json
{
  "id": 8,
  "nodeType": "UnaryOperation",
  "src": "47:3:0",
  "operator": "++",
  "prefix": false,
  "subExpression": { ... Expression ... },
  "typeDescriptions": { ... },
  "isConstant": false,
  "isLValue": false,
  "isPure": false,
  "lValueRequested": false,
  "function": null
}
```

Operators: `++`, `--`, `+`, `-`, `~`, `!`, `delete`.

`function` is present (non-null integer) when the operator is user-defined.

### BinaryOperation

```json
{
  "id": 10,
  "nodeType": "BinaryOperation",
  "src": "...",
  "operator": "+",
  "leftExpression": { ... Expression ... },
  "rightExpression": { ... Expression ... },
  "commonType": {
    "typeIdentifier": "t_uint256",
    "typeString": "uint256"
  },
  "typeDescriptions": { ... },
  "function": null
}
```

`commonType` is the common type used for the operation (after implicit conversions).

Operators: `+`, `-`, `*`, `/`, `%`, `**`, `==`, `!=`, `<`, `<=`, `>`, `>=`, `&&`, `||`, `^`, `&`, `|`, `<<`, `>>`, `>>>`.

### Assignment

```json
{
  "id": 5,
  "nodeType": "Assignment",
  "src": "...",
  "operator": "=",
  "leftHandSide": { ... Expression ... },
  "rightHandSide": { ... Expression ... },
  "typeDescriptions": { ... }
}
```

Operators: `=`, `+=`, `-=`, `*=`, `/=`, `%=`, `**=`, `&=`, `|=`, `^=`, `<<=`, `>>=`.

### Conditional (Ternary)

```json
{
  "id": 8,
  "nodeType": "Conditional",
  "src": "...",
  "condition": { ... Expression ... },
  "trueExpression": { ... Expression ... },
  "falseExpression": { ... Expression ... },
  "typeDescriptions": { ... }
}
```

### FunctionCall

```json
{
  "id": 8,
  "nodeType": "FunctionCall",
  "src": "...",
  "expression": { ... Expression (what's being called) ... },
  "arguments": [ ... Expression nodes ... ],
  "names": [],
  "nameLocations": [],
  "tryCall": false,
  "kind": "functionCall",
  "typeDescriptions": { ... }
}
```

| Field | Description |
|-------|-------------|
| `kind` | `"functionCall"`, `"typeConversion"`, or `"structConstructorCall"` |
| `names` | Non-empty only for named argument calls; contains the argument names |
| `nameLocations` | Source locations of the named argument names |
| `tryCall` | `true` if this is the call in a `try` statement |

### FunctionCallOptions

Represents `f{value: 1, gas: 21000}(...)` — sets call options before invocation.

```json
{
  "id": 6,
  "nodeType": "FunctionCallOptions",
  "src": "...",
  "expression": { ... Expression ... },
  "names": ["value", "gas"],
  "options": [
    { ... Expression for value ... },
    { ... Expression for gas ... }
  ],
  "typeDescriptions": { ... }
}
```

### NewExpression

Represents the `new T` part of `new T(...)`. The full call is a `FunctionCall` wrapping this.

```json
{
  "id": 4,
  "nodeType": "NewExpression",
  "src": "...",
  "typeName": { ... TypeName ... },
  "typeDescriptions": { ... }
}
```

### MemberAccess

Represents `expr.member`.

```json
{
  "id": 5,
  "nodeType": "MemberAccess",
  "src": "...",
  "expression": { ... Expression ... },
  "memberName": "balance",
  "memberLocation": "...",
  "referencedDeclaration": null,
  "typeDescriptions": { ... }
}
```

`referencedDeclaration` is the node ID of the member's declaration (null for built-in members like `.balance`, `.transfer`, etc.).

### IndexAccess

Represents `expr[index]`.

```json
{
  "id": 5,
  "nodeType": "IndexAccess",
  "src": "...",
  "baseExpression": { ... Expression ... },
  "indexExpression": { ... Expression ... },
  "typeDescriptions": { ... }
}
```

### IndexRangeAccess

Represents `arr[start:end]` (array slices).

```json
{
  "id": 6,
  "nodeType": "IndexRangeAccess",
  "src": "...",
  "baseExpression": { ... Expression ... },
  "startExpression": { ... Expression or null ... },
  "endExpression": { ... Expression or null ... },
  "typeDescriptions": { ... }
}
```

### TupleExpression

Represents `(a, b)` tuples or `[a, b]` inline arrays.

```json
{
  "id": 5,
  "nodeType": "TupleExpression",
  "src": "...",
  "isInlineArray": false,
  "components": [
    { ... Expression ... },
    { ... Expression ... }
  ],
  "typeDescriptions": { ... }
}
```

For destructuring assignments, components may be `null` for omitted slots.

### ElementaryTypeNameExpression

Represents a type used as an expression (e.g., `uint256(x)` — the `uint256` part).

```json
{
  "id": 3,
  "nodeType": "ElementaryTypeNameExpression",
  "src": "...",
  "typeName": {
    "id": 2,
    "nodeType": "ElementaryTypeName",
    "src": "...",
    "name": "uint256",
    "typeDescriptions": { ... }
  },
  "typeDescriptions": { ... }
}
```

---

## Inline Assembly (Yul)

Inline assembly nodes embed a Yul AST subtree.

```json
{
  "id": 10,
  "nodeType": "InlineAssembly",
  "src": "...",
  "evmVersion": "paris",
  "eofVersion": null,
  "flags": [],
  "AST": {
    "nodeType": "YulBlock",
    "src": "...",
    "statements": [
      {
        "nodeType": "YulAssignment",
        "src": "...",
        "variableNames": [
          { "nodeType": "YulIdentifier", "src": "...", "name": "result" }
        ],
        "value": {
          "nodeType": "YulFunctionCall",
          "src": "...",
          "functionName": { "nodeType": "YulIdentifier", "src": "...", "name": "add" },
          "arguments": [
            { "nodeType": "YulIdentifier", "src": "...", "name": "a" },
            { "nodeType": "YulLiteral", "src": "...", "kind": "number", "type": "", "value": "1", "hexValue": "01" }
          ]
        }
      }
    ]
  },
  "externalReferences": [
    {
      "src": "...",
      "declaration": 4,
      "isSlot": false,
      "isOffset": false,
      "suffix": "",
      "valueSize": 1
    }
  ]
}
```

### External References

`externalReferences` describes which Solidity variables are referenced from within the assembly block:

| Field | Description |
|-------|-------------|
| `declaration` | Node ID of the Solidity variable being referenced |
| `isSlot` | `true` if accessed with `.slot` suffix |
| `isOffset` | `true` if accessed with `.offset` suffix |
| `suffix` | The suffix string (`.slot`, `.offset`, `.length`, etc.) |
| `valueSize` | Number of EVM stack slots occupied by the value |

### Yul Node Types

| nodeType | Description |
|----------|-------------|
| `YulBlock` | `{ ... }` block of statements |
| `YulVariableDeclaration` | `let x := expr` |
| `YulAssignment` | `x := expr` |
| `YulExpressionStatement` | standalone expression |
| `YulIf` | `if cond { ... }` |
| `YulSwitch` | `switch expr case ... default ...` |
| `YulCase` | individual case in a switch |
| `YulForLoop` | `for { init } cond { post } { body }` |
| `YulBreak` | `break` |
| `YulContinue` | `continue` |
| `YulLeave` | `leave` (exit function) |
| `YulFunctionDefinition` | `function name(...) -> (...) { ... }` |
| `YulFunctionCall` | `name(arg1, arg2)` |
| `YulIdentifier` | a name |
| `YulLiteral` | a literal value |
| `YulTypedName` | a named Yul parameter with optional type |

---

## Complete Examples

### Example 1: Minimal Contract

**Source:**
```solidity
contract C {}
```

**AST JSON:**
```json
{
  "absolutePath": "a",
  "exportedSymbols": {
    "C": [1]
  },
  "id": 2,
  "nodeType": "SourceUnit",
  "nodes": [
    {
      "abstract": false,
      "baseContracts": [],
      "canonicalName": "C",
      "contractDependencies": [],
      "contractKind": "contract",
      "fullyImplemented": true,
      "id": 1,
      "linearizedBaseContracts": [1],
      "name": "C",
      "nameLocation": "9:1:1",
      "nodeType": "ContractDefinition",
      "nodes": [],
      "scope": 2,
      "src": "0:13:1",
      "usedErrors": [],
      "usedEvents": []
    }
  ],
  "src": "0:14:1"
}
```

---

### Example 2: Function with Local Variables and Expressions

**Source:**
```solidity
contract C { function f() public { uint x = 2; x++; } }
```

**AST JSON (key parts):**
```json
{
  "nodeType": "FunctionDefinition",
  "name": "f",
  "kind": "function",
  "visibility": "public",
  "stateMutability": "nonpayable",
  "functionSelector": "26121ff0",
  "body": {
    "nodeType": "Block",
    "statements": [
      {
        "nodeType": "VariableDeclarationStatement",
        "assignments": [4],
        "declarations": [{
          "id": 4,
          "nodeType": "VariableDeclaration",
          "name": "x",
          "typeName": { "nodeType": "ElementaryTypeName", "name": "uint" },
          "typeDescriptions": { "typeIdentifier": "t_uint256", "typeString": "uint256" }
        }],
        "initialValue": {
          "nodeType": "Literal",
          "kind": "number",
          "value": "2",
          "hexValue": "32",
          "typeDescriptions": { "typeIdentifier": "t_rational_2_by_1", "typeString": "int_const 2" }
        }
      },
      {
        "nodeType": "ExpressionStatement",
        "expression": {
          "nodeType": "UnaryOperation",
          "operator": "++",
          "prefix": false,
          "subExpression": {
            "nodeType": "Identifier",
            "name": "x",
            "referencedDeclaration": 4,
            "typeDescriptions": { "typeIdentifier": "t_uint256", "typeString": "uint256" }
          }
        }
      }
    ]
  }
}
```

---

### Example 3: Mappings

**Source:**
```solidity
contract C {
    mapping(address => bool) b;
    mapping(address keyAddress => uint256 value) d;
}
```

**Mapping without named keys:**
```json
{
  "nodeType": "Mapping",
  "keyType": {
    "nodeType": "ElementaryTypeName",
    "name": "address",
    "typeDescriptions": { "typeIdentifier": "t_address", "typeString": "address" }
  },
  "keyName": "",
  "keyNameLocation": "-1:-1:-1",
  "valueType": {
    "nodeType": "ElementaryTypeName",
    "name": "bool",
    "typeDescriptions": { "typeIdentifier": "t_bool", "typeString": "bool" }
  },
  "valueName": "",
  "valueNameLocation": "-1:-1:-1",
  "typeDescriptions": {
    "typeIdentifier": "t_mapping$_t_address_$_t_bool_$",
    "typeString": "mapping(address => bool)"
  }
}
```

**Mapping with named keys:**
```json
{
  "nodeType": "Mapping",
  "keyType": { "nodeType": "ElementaryTypeName", "name": "address", ... },
  "keyName": "keyAddress",
  "keyNameLocation": "140:10:1",
  "valueType": { "nodeType": "ElementaryTypeName", "name": "uint256", ... },
  "valueName": "value",
  "valueNameLocation": "162:5:1",
  "typeDescriptions": {
    "typeIdentifier": "t_mapping$_t_address_$_t_uint256_$",
    "typeString": "mapping(address => uint256)"
  }
}
```

---

### Example 4: Inheritance and Events

**Source:**
```solidity
contract C1 {}
contract C2 is C1 {}
```

**C2's ContractDefinition (key parts):**
```json
{
  "nodeType": "ContractDefinition",
  "name": "C2",
  "baseContracts": [
    {
      "nodeType": "InheritanceSpecifier",
      "baseName": {
        "nodeType": "IdentifierPath",
        "name": "C1",
        "referencedDeclaration": 1
      }
    }
  ],
  "linearizedBaseContracts": [4, 1]
}
```

---

### Example 5: NatSpec Documentation

**Source:**
```solidity
/// This contract is empty
contract C {}
```

**ContractDefinition with documentation:**
```json
{
  "nodeType": "ContractDefinition",
  "name": "C",
  "documentation": {
    "id": 1,
    "nodeType": "StructuredDocumentation",
    "src": "0:27:0",
    "text": "This contract is empty"
  },
  ...
}
```

---

### Example 6: Modifier with Placeholder

**Source:**
```solidity
contract C {
    modifier M(uint i) { _; }
    function F() M(1) public {}
}
```

The function's `modifiers` array contains the invocation:
```json
{
  "nodeType": "ModifierInvocation",
  "kind": "modifierInvocation",
  "modifierName": {
    "nodeType": "IdentifierPath",
    "name": "M",
    "referencedDeclaration": 6
  },
  "arguments": [
    {
      "nodeType": "Literal",
      "kind": "number",
      "value": "1",
      "hexValue": "31"
    }
  ]
}
```

The modifier body contains a `PlaceholderStatement`:
```json
{
  "nodeType": "Block",
  "statements": [
    { "nodeType": "PlaceholderStatement", "src": "34:1:0" }
  ]
}
```

---

## Notes on Optional Fields and Null Removal

1. **Null removal**: The exporter automatically strips all fields with `null` values from the output. Do not assume absent means different from `null`—they are equivalent.

2. **Parse-only vs. full analysis**: When the compiler only parses (without type analysis), many fields are absent or have empty values:
   - `typeDescriptions` is absent on expressions/type names
   - `referencedDeclaration` may be absent or 0
   - `canonicalName` may be absent
   - `functionSelector`, `eventSelector`, `errorSelector` are absent
   - `isConstant`, `isLValue`, `isPure`, `lValueRequested` are absent on expressions
   - `linearizedBaseContracts`, `contractDependencies`, `usedEvents`, `usedErrors` are absent or empty

3. **Source file index**: File indices in `src` are 1-based in multi-file compilations (the index matches the order of `SourceUnit` objects in the output array). For single-file compilations, the index is typically `1`.

4. **Cross-file node IDs**: Node IDs are globally unique across the entire compilation unit. All IDs across all source files in a single `solc` invocation are distinct integers.

5. **`-1:-1:-1` locations**: Compiler-generated nodes (e.g., implicit constructors, auto-generated return parameters) use `"-1:-1:-1"` as their `src` and `nameLocation`.

6. **`exportedSymbols`**: Maps to arrays (not single IDs) because overloaded free functions may share the same name.
