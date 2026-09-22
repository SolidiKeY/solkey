grammar SolSpec;

@header
{
package org.key_project.solidity.parser;
}

// Specification expressions of the @custom:key natspec clauses (see
// keyext.solidity.examples/README.md). Alternatives are listed from the tightest binding to
// the loosest, so a quantifier's body extends as far to the right as possible.

spec
  : expr EOF ;

expr
  : '(' expr ')'                                           # parens
  | 'net' '(' expr ')'                                     # net
  | '\\old' '(' expr ')'                                   # old
  | 'address' '(' expr ')'                                 # cast
  | '\\result'                                             # result
  | INT                                                    # intLit
  | ('true' | 'false')                                     # boolLit
  | IDENT                                                  # ident
  | expr '[' expr ']'                                      # index
  | expr '.' IDENT                                         # member
  | op=('!' | '-') expr                                    # unary
  | expr op=('*' | '/' | '%') expr                         # mul
  | expr op=('+' | '-') expr                               # add
  | expr op=('<' | '<=' | '>' | '>=') expr                 # rel
  | expr op=('==' | '!=') expr                             # eq
  | expr '&&' expr                                         # and
  | expr '||' expr                                         # or
  | <assoc=right> expr '->' expr                           # impl
  | expr '<->' expr                                        # iff
  | q=('\\forall' | '\\exists') sort var=IDENT ';' expr    # quantifier
  ;

sort
  : IDENT | 'address' ;

INT   : [0-9]+ ;
IDENT : [a-zA-Z_] [a-zA-Z0-9_]* ;
WS    : [ \t\r\n]+ -> skip ;
