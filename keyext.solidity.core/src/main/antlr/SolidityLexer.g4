lexer grammar SolidityLexer;

fragment DEFAULT_MODE_PLACEHOLDER : 'SolidityLexer';

mode SOL;

RETURNS : 'returns';
SOL_LPAREN : '(';
SOL_COMMA : ',';
SOL_RPAREN : ')';
SOL_LBRACKET : '[';
SOL_RBRACKET : ']';
ADDRESS : 'address';
SOL_DOT : '.';
MAPPING : 'mapping';
ARROW : '=>';
FUNCTION : 'function';
MEMORY : 'memory';
STORAGE : 'storage';
CALLDATA : 'calldata';
SOL_LBRACE : '{';
SOL_RBRACE : '}';
CTX_OPEN : '{c#';
CTX_CLOSE : '#c}';
SOL_SEMI : ';';
SOL_IF : 'if';
SOL_ELSE : 'else';
TRY : 'try';
CATCH : 'catch';
WHILE : 'while';
UNCHECKED : 'unchecked';
FOR : 'for';
DO : 'do';
RETURN : 'return';
THROW : 'throw';
EMIT : 'emit';
REVERT : 'revert';
VAR : 'var';
SOL_ASSIGN : '=';
BOOL : 'bool';
STRING : 'string';
BYTE : 'byte';
INC : '++';
DEC : '--';
SOL_COLON : ':';
SOL_NEW : 'new';
SOL_PLUS : '+';
SOL_MINUS : '-';
SOL_NOT : '!';
SOL_TILDE : '~';
DELETE : 'delete';
POW : '**';
MUL : '*';
DIV : '/';
MOD : '%';
SHL : '<<';
SHR : '>>';
LT : '<';
GT : '>';
LE : '<=';
GE : '>=';
EQ : '==';
NE : '!=';
BITAND : '&';
BITXOR : '^';
BITOR : '|';
SOL_AND : '&&';
SOL_OR : '||';
QUESTION : '?';
OR_ASSIGN : '|=';
XOR_ASSIGN : '^=';
AND_ASSIGN : '&=';
SHL_ASSIGN : '<<=';
SHR_ASSIGN : '>>=';
ADD_ASSIGN : '+=';
SUB_ASSIGN : '-=';
MUL_ASSIGN : '*=';
DIV_ASSIGN : '/=';
MOD_ASSIGN : '%=';
FROM : 'from';
CALLBACK : 'callback';
ERROR : 'error';
LAYOUT : 'layout';
AT_KW : 'at';
OVERRIDE : 'override';
SOL_AT : '@';

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

SOL_WS
  : [ \t\r\n\u000C]+ -> skip ;

SOL_COMMENT
  : '/*' .*? '*/' -> channel(HIDDEN) ;

LINE_COMMENT
  : '//' ~[\r\n]* -> channel(HIDDEN) ;

ExpandFunctionBody
   : 's#expand_function_body' ;

Schema
   : 's#' Identifier ;

Identifier
  : IdentifierStart IdentifierPart* ;

fragment
IdentifierStart
  : [a-zA-Z$_] ;

fragment
IdentifierPart
  : [a-zA-Z0-9$_] ;
