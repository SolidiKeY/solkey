parser grammar SolidityRules;

returnParameters
  : RETURNS parameterList ;

parameterList
  : SOL_LPAREN ( functionParameter (SOL_COMMA functionParameter)* )? SOL_RPAREN ;

functionParameter
  : typeName storageLocation? identifier? ;

functionTypeParameterList
  : SOL_LPAREN ( functionTypeParameter (SOL_COMMA functionTypeParameter)* )? SOL_RPAREN ;

functionTypeParameter
  : typeName storageLocation? ;

variableDeclaration
  : typeName storageLocation? ( identifier | schemaVariable ) ;

typeName
  : schemaVariable                # SchemaType
  | elementaryTypeName            # ElementaryType
  | userDefinedTypeName           # UserDefinedType
  | mapping                       # MappingType
  | typeName SOL_LBRACKET expression? SOL_RBRACKET  # ArrayType
  | functionTypeName              # FunctionType
  | ADDRESS PayableKeyword           # AddressPayable
  ;

userDefinedTypeName
  : identifier ( SOL_DOT identifier )* ;

mappingKey
  : elementaryTypeName
  | userDefinedTypeName ;

mapping
  : MAPPING SOL_LPAREN mappingKey mappingKeyName? ARROW typeName mappingValueName? SOL_RPAREN ;

mappingKeyName : identifier;
mappingValueName : identifier;

functionTypeName
  : FUNCTION functionTypeParameterList
    ( InternalKeyword | ExternalKeyword | stateMutability )*
    ( RETURNS functionTypeParameterList )? ;

storageLocation
  : MEMORY | STORAGE | CALLDATA;

stateMutability
  : PureKeyword | ConstantKeyword | ViewKeyword | PayableKeyword ;

block
  : normalBlock | contextBlock;

normalBlock
  : SOL_LBRACE statement* SOL_RBRACE;

contextBlock
  : CTX_OPEN statement* CTX_CLOSE;

statement
  : simpleStatement
  | schemaVariable
  | programTransformer
  | functionBodyStatement
  | functionFrame
  | ifStatement
  | tryStatement
  | whileStatement
  | forStatement
  | block
  | doWhileStatement
  | continueStatement
  | breakStatement
  | returnStatement
  | throwStatement
  | emitStatement
  | uncheckedStatement
  | revertStatement;

expressionStatement
  : expression SOL_SEMI ;

ifStatement
  : SOL_IF SOL_LPAREN expression SOL_RPAREN ifStm=statement ( SOL_ELSE elseStm=statement )? ;

tryStatement : TRY expression returnParameters? ( block | schemaVariable ) catchClause+ ;

// In reality catch clauses still are not processed as below
// the identifier can only be a set string: "Error". But plans
// of the Solidity team include possible expansion so we'll
// leave this as is, befitting with the Solidity docs.
catchClause : CATCH ( identifier parameterList? | parameterList )? ( block | schemaVariable ) ;

whileStatement
  : WHILE SOL_LPAREN expression SOL_RPAREN statement ;

simpleStatement
  : ( variableDeclarationStatement | expressionStatement ) ;

uncheckedStatement
  : UNCHECKED block ;

forStatement
  : FOR SOL_LPAREN ( simpleStatement | SOL_SEMI ) ( expressionStatement | SOL_SEMI ) expression? SOL_RPAREN statement ;

doWhileStatement
  : DO statement WHILE SOL_LPAREN expression SOL_RPAREN SOL_SEMI ;

continueStatement
  : ContinueKeyword SOL_SEMI ;

breakStatement
  : BreakKeyword SOL_SEMI ;

returnStatement
  : RETURN expression? SOL_SEMI ;

functionFrame
  : FUNCTION_FRAME SOL_LBRACE statement* SOL_RBRACE ;

throwStatement
  : THROW SOL_SEMI ;

emitStatement
  : EMIT functionCall SOL_SEMI ;

revertStatement
  : REVERT functionCall SOL_SEMI ;

variableDeclarationStatement
  : VAR identifierList ( SOL_ASSIGN expression )? SOL_SEMI            # VarDeclStatement
  | variableDeclaration ( SOL_ASSIGN expression )? SOL_SEMI             # SingleVarDeclStatement
  | SOL_LPAREN variableDeclarationList SOL_RPAREN ( SOL_ASSIGN expression )? SOL_SEMI # MultiVarDeclStatement
  ;

variableDeclarationList
  : variableDeclaration? (SOL_COMMA variableDeclaration? )* ;

identifierList
  : SOL_LPAREN ( identifier? SOL_COMMA )* identifier? SOL_RPAREN ;

elementaryTypeName
  : ADDRESS | BOOL | STRING | VAR | Int | Uint | BYTE | Byte | Fixed | Ufixed ;

expression
  : primaryExpression                                 # Primary
  | SOL_LPAREN expression SOL_RPAREN                                # Grouping
  | expression (INC | DEC)                          # Postfix
  | left=expression SOL_LBRACKET index=expression SOL_RBRACKET                     # IndexAccess
  | base=expression SOL_LBRACKET start=expression? SOL_COLON end=expression? SOL_RBRACKET    # SliceAccess
  | expression SOL_DOT ( identifier | schemaVariable )    # MemberAccess
  | expression SOL_LBRACE nameValueList SOL_RBRACE                  # ObjectInit
  | expression SOL_LPAREN functionCallArguments SOL_RPAREN          # FunctionCallExp
  | SOL_NEW typeName                                    # NewInstance
  | (INC | DEC | SOL_PLUS | SOL_MINUS | SOL_NOT | SOL_TILDE) expression  # UnaryPrefix
  | DELETE expression                               # Delete
  | <assoc=right> expression POW expression          # BinaryOp
  | expression (MUL | DIV | MOD) expression           # BinaryOp
  | expression (SOL_PLUS | SOL_MINUS) expression                 # BinaryOp
  | expression (SHL | SHR) expression               # BinaryOp
  | expression (LT | GT | LE | GE) expression   # BinaryOp
  | expression (EQ | NE) expression               # BinaryOp
  | expression BITAND expression                         # BinaryOp
  | expression BITXOR expression                         # BinaryOp
  | expression BITOR expression                         # BinaryOp
  | expression SOL_AND expression                        # BinaryOp
  | expression SOL_OR expression                        # BinaryOp
  | <assoc=right> condition=expression QUESTION true=expression SOL_COLON false=expression # Ternary
  | <assoc=right> expression
    (SOL_ASSIGN | OR_ASSIGN | XOR_ASSIGN | AND_ASSIGN | SHL_ASSIGN | SHR_ASSIGN | ADD_ASSIGN | SUB_ASSIGN | MUL_ASSIGN | DIV_ASSIGN | MOD_ASSIGN)
    expression                                        # BinaryOp
  ;

primaryExpression
  : schemaVariable
  | BooleanLiteral
  | numberLiteral
  | hexLiteral
  | stringLiteral
  | identifier
  | TypeKeyword
  | PayableKeyword
  | tupleExpression
  | typeName;

expressionList
  : expression (SOL_COMMA expression)* ;

nameValueList
  : nameValue (SOL_COMMA nameValue)* SOL_COMMA? ;

nameValue
  : identifier SOL_COLON expression ;

functionCallArguments
  : SOL_LBRACE nameValueList? SOL_RBRACE
  | expressionList? ;

functionCall
  : expression SOL_LPAREN functionCallArguments SOL_RPAREN ;

labelDefinition
  : identifier SOL_COLON ;

tupleExpression
  : SOL_LPAREN ( expression? ( SOL_COMMA expression? )* ) SOL_RPAREN
  | SOL_LBRACKET ( expression ( SOL_COMMA expression )* )? SOL_RBRACKET ;

numberLiteral
  : (DecimalNumber | HexNumber) NumberUnit? ;

// some keywords need to be added here to avoid ambiguities
// for example, "revert" is a keyword but it can also be a function name
identifier
  : (FROM | CALLDATA | ReceiveKeyword | CALLBACK | REVERT | ERROR | ADDRESS | LAYOUT | AT_KW | GlobalKeyword | ConstructorKeyword | PayableKeyword | LeaveKeyword | Identifier) ;

hexLiteral : HexLiteralFragment+ ;

overrideSpecifier : OVERRIDE ( SOL_LPAREN userDefinedTypeName (SOL_COMMA userDefinedTypeName)* SOL_RPAREN )? ;

stringLiteral
  : StringLiteralFragment+ ;

schemaVariable
   : Schema
   ;

// program transformers (meta constructs) appearing in the replacewith of taclets
programTransformer
   : ExpandFunctionBody SOL_LPAREN schemaVariable SOL_RPAREN   # ExpandFunctionBodyTransformer
   ;

// a call annotated with the declaring contract, standing for the (not yet inlined)
// body of that function, e.g.  withdraw(a)@Contract;  or  r = balanceOf()@Contract;
// or  (q, , r) = divmod(a, b)@Contract;  the optional left-hand side binds the function's
// return values, an empty component discarding one.
functionBodyStatement
   : (lhs=functionBodyTargets SOL_ASSIGN)? fn=identifier SOL_LPAREN functionCallArguments SOL_RPAREN SOL_AT contract=identifier SOL_SEMI ;

functionBodyTargets
   : identifier
   | SOL_LPAREN identifier? ( SOL_COMMA identifier? )+ SOL_RPAREN ;

solidityBlockEOF
  : block EOF ;
