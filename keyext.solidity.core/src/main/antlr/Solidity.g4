grammar Solidity;

import SchemaLexer;

@header
{
package org.key_project.solidity.parser;
}

returnParameters
  : 'returns' parameterList ;

parameterList
  : '(' ( parameter (',' parameter)* )? ')' ;

parameter
  : typeName storageLocation? identifier? ;

functionTypeParameterList
  : '(' ( functionTypeParameter (',' functionTypeParameter)* )? ')' ;

functionTypeParameter
  : typeName storageLocation? ;

variableDeclaration
  : typeName storageLocation? ( identifier | schemaVariable ) ;

typeName
  : schemaVariable                # SchemaType
  | elementaryTypeName            # ElementaryType
  | userDefinedTypeName           # UserDefinedType
  | mapping                       # MappingType
  | typeName '[' expression? ']'  # ArrayType
  | functionTypeName              # FunctionType
  | 'address' 'payable'           # AddressPayable
  ;

userDefinedTypeName
  : identifier ( '.' identifier )* ;

mappingKey
  : elementaryTypeName
  | userDefinedTypeName ;

mapping
  : 'mapping' '(' mappingKey mappingKeyName? '=>' typeName mappingValueName? ')' ;

mappingKeyName : identifier;
mappingValueName : identifier;

functionTypeName
  : 'function' functionTypeParameterList
    ( InternalKeyword | ExternalKeyword | stateMutability )*
    ( 'returns' functionTypeParameterList )? ;

storageLocation
  : 'memory' | 'storage' | 'calldata';

stateMutability
  : PureKeyword | ConstantKeyword | ViewKeyword | PayableKeyword ;

block
  : normalBlock | contextBlock;

normalBlock
  : '{' statement* '}';

contextBlock
  : '{c#' statement* '#c}';

statement
  : schemaVariable
  | programTransformer
  | functionBodyStatement
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
  | simpleStatement
  | uncheckedStatement
  | revertStatement;

expressionStatement
  : expression ';' ;

ifStatement
  : 'if' '(' expression ')' ifStm=statement ( 'else' elseStm=statement )? ;

tryStatement : 'try' expression returnParameters? block catchClause+ ;

// In reality catch clauses still are not processed as below
// the identifier can only be a set string: "Error". But plans
// of the Solidity team include possible expansion so we'll
// leave this as is, befitting with the Solidity docs.
catchClause : 'catch' ( identifier? parameterList )? block ;

whileStatement
  : 'while' '(' expression ')' statement ;

simpleStatement
  : ( variableDeclarationStatement | expressionStatement ) ;

uncheckedStatement
  : 'unchecked' block ;

forStatement
  : 'for' '(' ( simpleStatement | ';' ) ( expressionStatement | ';' ) expression? ')' statement ;

doWhileStatement
  : 'do' statement 'while' '(' expression ')' ';' ;

continueStatement
  : 'continue' ';' ;

breakStatement
  : 'break' ';' ;

returnStatement
  : 'return' expression? ';' ;

throwStatement
  : 'throw' ';' ;

emitStatement
  : 'emit' functionCall ';' ;

revertStatement
  : 'revert' functionCall ';' ;

variableDeclarationStatement
  : 'var' identifierList ( '=' expression )? ';'            # VarDeclStatement
  | variableDeclaration ( '=' expression )? ';'             # SingleVarDeclStatement
  | '(' variableDeclarationList ')' ( '=' expression )? ';' # MultiVarDeclStatement
  ;

variableDeclarationList
  : variableDeclaration? (',' variableDeclaration? )* ;

identifierList
  : '(' ( identifier? ',' )* identifier? ')' ;

elementaryTypeName
  : 'address' | 'bool' | 'string' | 'var' | Int | Uint | 'byte' | Byte | Fixed | Ufixed ;

Int
  : 'int' (NumberOfBits)? ;

Uint
  : 'uint' (NumberOfBits)? ;

Byte
  : 'bytes' (NumberOfBytes)?;

Fixed
  : 'fixed' ( NumberOfBits 'x' [0-9]+ )? ;

Ufixed
  : 'ufixed' ( NumberOfBits 'x' [0-9]+ )? ;

fragment
NumberOfBits
  : '8' | '16' | '24' | '32' | '40' | '48' | '56' | '64' | '72' | '80' | '88' | '96' | '104' | '112' | '120' | '128' | '136' | '144' | '152' | '160' | '168' | '176' | '184' | '192' | '200' | '208' | '216' | '224' | '232' | '240' | '248' | '256' ;

fragment
NumberOfBytes
  : [1-9] | [12] [0-9] | '3' [0-2] ;

expression
  : primaryExpression                                 # Primary
  | '(' expression ')'                                # Grouping
  | expression ('++' | '--')                          # Postfix
  | left=expression '[' index=expression ']'                     # IndexAccess
  | base=expression '[' start=expression? ':' end=expression? ']'    # SliceAccess
  | expression '.' identifier                         # MemberAccess
  | expression '{' nameValueList '}'                  # ObjectInit
  | expression '(' functionCallArguments ')'          # FunctionCallExp
  | 'new' typeName                                    # NewInstance
  | ('++' | '--' | '+' | '-' | '!' | '~') expression  # UnaryPrefix
  | 'delete' expression                               # Delete
  | <assoc=right> expression '**' expression          # BinaryOp
  | expression ('*' | '/' | '%') expression           # BinaryOp
  | expression ('+' | '-') expression                 # BinaryOp
  | expression ('<<' | '>>') expression               # BinaryOp
  | expression ('<' | '>' | '<=' | '>=') expression   # BinaryOp
  | expression ('==' | '!=') expression               # BinaryOp
  | expression '&' expression                         # BinaryOp
  | expression '^' expression                         # BinaryOp
  | expression '|' expression                         # BinaryOp
  | expression '&&' expression                        # BinaryOp
  | expression '||' expression                        # BinaryOp
  | <assoc=right> expression
    ('=' | '|=' | '^=' | '&=' | '<<=' | '>>=' | '+=' | '-=' | '*=' | '/=' | '%=')
    expression                                        # BinaryOp
  | <assoc=right> condition=expression '?' true=expression ':' false=expression # Ternary
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
  : expression (',' expression)* ;

nameValueList
  : nameValue (',' nameValue)* ','? ;

nameValue
  : identifier ':' expression ;

functionCallArguments
  : '{' nameValueList? '}'
  | expressionList? ;

functionCall
  : expression '(' functionCallArguments ')' ;

labelDefinition
  : identifier ':' ;

tupleExpression
  : '(' ( expression? ( ',' expression? )* ) ')'
  | '[' ( expression ( ',' expression )* )? ']' ;

numberLiteral
  : (DecimalNumber | HexNumber) NumberUnit? ;

// some keywords need to be added here to avoid ambiguities
// for example, "revert" is a keyword but it can also be a function name
identifier
  : ('from' | 'calldata' | 'receive' | 'callback' | 'revert' | 'error' | 'address' | 'layout' | 'at' | GlobalKeyword | ConstructorKeyword | PayableKeyword | LeaveKeyword | Identifier) ;

BooleanLiteral
  : 'true' | 'false' ;

DecimalNumber
  : ( DecimalDigits | (DecimalDigits? '.' DecimalDigits) ) ( [eE] '-'? DecimalDigits )? ;

fragment
DecimalDigits
  : [0-9] ( '_'? [0-9] )* ;

HexNumber
  : '0' [xX] HexDigits ;

fragment
HexDigits
  : HexCharacter ( '_'? HexCharacter )* ;

NumberUnit
  : 'wei' | 'gwei' | 'szabo' | 'finney' | 'ether'
  | 'seconds' | 'minutes' | 'hours' | 'days' | 'weeks' | 'years' ;

hexLiteral : HexLiteralFragment+ ;

HexLiteralFragment : 'hex' ('"' HexDigits? '"' | '\'' HexDigits? '\'') ;

fragment
HexCharacter
  : [0-9A-Fa-f] ;

ReservedKeyword
  : 'after'
  | 'alias'
  | 'apply'
  | 'auto'
  | 'case'
  | 'copyof'
  | 'default'
  | 'define'
  | 'final'
  | 'implements'
  | 'in'
  | 'inline'
  | 'let'
  | 'macro'
  | 'match'
  | 'mutable'
  | 'null'
  | 'of'
  | 'partial'
  | 'promise'
  | 'reference'
  | 'relocatable'
  | 'sealed'
  | 'sizeof'
  | 'static'
  | 'supports'
  | 'switch'
  | 'typedef'
  | 'typeof' ;

AnonymousKeyword : 'anonymous' ;
BreakKeyword : 'break' ;
ConstantKeyword : 'constant' ;
TransientKeyword : 'transient' ;
ImmutableKeyword : 'immutable' ;
ContinueKeyword : 'continue' ;
LeaveKeyword : 'leave' ;
ExternalKeyword : 'external' ;
IndexedKeyword : 'indexed' ;
InternalKeyword : 'internal' ;
PayableKeyword : 'payable' ;
PrivateKeyword : 'private' ;
PublicKeyword : 'public' ;
VirtualKeyword : 'virtual' ;
PureKeyword : 'pure' ;
TypeKeyword : 'type' ;
ViewKeyword : 'view' ;
GlobalKeyword : 'global' ;

ConstructorKeyword : 'constructor' ;
FallbackKeyword : 'fallback' ;
ReceiveKeyword : 'receive' ;

overrideSpecifier : 'override' ( '(' userDefinedTypeName (',' userDefinedTypeName)* ')' )? ;

stringLiteral
  : StringLiteralFragment+ ;

StringLiteralFragment
  : 'unicode'? ( '"' DoubleQuotedStringCharacter* '"' | '\'' SingleQuotedStringCharacter* '\'' ) ;

fragment
DoubleQuotedStringCharacter
  : ~["\r\n\\] | ('\\' .) ;

fragment
SingleQuotedStringCharacter
  : ~['\r\n\\] | ('\\' .) ;

VersionLiteral
  : [0-9]+ '.' [0-9]+ ('.' [0-9]+)? ;

WS
  : [ \t\r\n\u000C]+ -> skip ;

COMMENT
  : '/*' .*? '*/' -> channel(HIDDEN) ;

LINE_COMMENT
  : '//' ~[\r\n]* -> channel(HIDDEN) ;

schemaVariable
   : Schema
   ;

// program transformers (meta constructs) appearing in the replacewith of taclets
programTransformer
   : ExpandFunctionBody '(' schemaVariable ')'   # ExpandFunctionBodyTransformer
   ;

// a call annotated with the declaring contract, standing for the (not yet inlined)
// body of that function, e.g.  withdraw(a)@Contract;  or  r = balanceOf()@Contract;
// the optional left-hand side binds the function's (named) return value.
functionBodyStatement
   : (lhs=identifier '=')? fn=identifier '(' functionCallArguments ')' '@' contract=identifier ';' ;