parser grammar KeYSolidityDLParser;

import KeYParser;

options { tokenVocab = KeYSolidityDLLexer; }

@header {
package org.key_project.solidity.parser;
}



parametric_sort_decl
:
    simple_ident_dots
    formal_sort_param_decls
;

formal_sort_param_decls
: OPENTYPEPARAMS
      formal_sort_param_decl (COMMA formal_sort_param_decl)*
      CLOSETYPEPARAMS ;

formal_sort_param_decl
:
    simple_ident | const_param_decl
;

const_param_decl: CONST simple_ident COLON sortId ;

datatype_decl:
  doc=DOC_COMMENT?
  // weigl: all datatypes are free!
  // FREE?
  name=simple_ident formal_sort_param_decls?
  EQUALS
  datatype_constructor (OR datatype_constructor)*
  SEMI
;

datatype_constructor:
  name=simple_ident
  (
    LPAREN
    (argName+=simple_ident COLON argSort+=sortId
     (COMMA argName+=simple_ident COLON argSort+=sortId)*
    )?
    RPAREN
  )?
;

sortId
:
    id=simple_ident_dots formal_sort_args?
;

formal_sort_args
:
    OPENTYPEPARAMS
    formal_sort_arg (COMMA formal_sort_arg)*
    CLOSETYPEPARAMS
;

formal_sort_arg : sortId | CONST term ;


one_sort_decl
:
  doc=DOC_COMMENT?
  (
     GENERIC  sortIds=simple_ident_dots_comma_list
        (ONEOF sortOneOf = oneof_sorts)?
        (EXTENDS sortExt = extends_sorts)? SEMI
    | PROXY  sortIds=simple_ident_dots_comma_list (EXTENDS sortExt=extends_sorts)? SEMI
    | ABSTRACT? (sortIds=simple_ident_dots_comma_list |
                 parametric_sort_decl) (EXTENDS sortExt=extends_sorts)?  SEMI
  )
;

primitive_term:
    termParen
  | ifThenElseTerm
  | ifExThenElseTerm
  | abbreviation
  | accessterm
  | literals
  ;

accessterm
:
  // OLD
  firstName=simple_ident
  formal_sort_args?
  call?
  ( attribute )*
;


funcpred_name
   : (name = simple_colon_dots | num = INT_LITERAL)
   ;

 simple_colon_dots
:
  DOUBLECOLON? simple_ident (DOUBLECOLON simple_ident)*
;