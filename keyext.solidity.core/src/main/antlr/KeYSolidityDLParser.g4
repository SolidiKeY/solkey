parser grammar KeYSolidityDLParser;

import KeYParser;

options { tokenVocab = KeYSolidityDLLexer; }

@header {
package org.key_project.solidity.parser;
}


varexpId
 : APPLY_UPDATE_ON_RIGID
 | DROP_EFFECTLESS_ELEMENTARIES
 | SIMPLIFY_IF_THEN_ELSE_UPDATE
 | EQUAL_UNIQUE
 | NEW_TYPE_OF
 | NEW_DEPENDING_ON
 | NEW
 | NEW_LOCAL_VARS
 //| STORE_TERM_IN
 //| STORE_EXPR_IN
 | HAS_INVARIANT
 | GET_INVARIANT
 | GET_VARIANT
 //| IS_LABELED
 | DIFFERENT
 | SAME
 | ISSUBTYPE
 | HASSORT
 | NO_FREE_VAR_IN
 ;

 one_sort_decl
 :
    doc=DOC_COMMENT?
     (
        GENERIC  sortIds=simple_ident_dots_comma_list
           (ONEOF sortOneOf = oneof_sorts)?
           (EXTENDS sortExt = extends_sorts)? SEMI
       | PROXY  sortIds=simple_ident_dots_comma_list (EXTENDS sortExt=extends_sorts)? SEMI
       | ABSTRACT? (sortIds=simple_ident_dots_comma_list | parametric_sort_decl) (EXTENDS sortExt=extends_sorts)?  SEMI
       | ALIAS simple_ident_dots EQUALS sortId SEMI
     )
 ;

parametric_sort_decl
:
    simple_ident_dots
    formal_sort_param_decls
;

formal_sort_param_decls
: OPENTYPEPARAMS
      formal_sort_param_decl (COMMA formal_sort_param_decl)*
      CLOSETYPEPARAMS
;

formal_sort_param_decl
:
    (PLUS | MINUS)? simple_ident
;


const_param_decl: CONST simple_ident COLON sortId ;

//datatype_decl:
//  doc=DOC_COMMENT?
//  // weigl: all datatypes are free!
//  // FREE?
//  name=simple_ident formal_sort_param_decls?
//  EQUALS
//  datatype_constructor (OR datatype_constructor)*
//  SEMI
//;
//
//datatype_constructor:
//  name=simple_ident
//  (
//    LPAREN
//    (argName+=simple_ident COLON argSort+=sortId
//     (COMMA argName+=simple_ident COLON argSort+=sortId)*
//    )?
//    RPAREN
//  )?
//;

sortId
:
    id=simple_ident_dots formal_sort_args?
;

formal_sort_args
:
    OPENTYPEPARAMS
    sortId (COMMA sortId)*
    CLOSETYPEPARAMS
;

formal_sort_arg : sortId | CONST term ;

primitive_term:
    termParen
  | ifThenElseTerm
  | ifExThenElseTerm
  | abbreviation
  | accessterm
  | literals
  ;

func_decl
:
    doc=DOC_COMMENT?
    (UNIQUE)?
    func_name = funcpred_name
    formal_sort_param_decls?
	whereToBind=where_to_bind?
    argSorts = arg_sorts
    IMP
    retSort = sortId
    SEMI
;

pred_decl
:
  doc=DOC_COMMENT?
  pred_name = funcpred_name
  formal_sort_param_decls?
  (whereToBind=where_to_bind)?
  argSorts=arg_sorts
  SEMI
;

accessterm
:
  // OLD
  firstName=simple_ident
  formal_sort_args?
  call?
  ( attribute )*
;

varexp_argument
:
    TYPEOF LPAREN y=varId RPAREN
  | SORT LPAREN sortId RPAREN
  | DEPENDINGON LPAREN y=varId RPAREN
  | term
;

funcpred_name
   : (name = simple_colon_dots | num = INT_LITERAL)
   ;

 simple_colon_dots
:
  DOUBLECOLON? simple_ident (DOUBLECOLON simple_ident)*
;