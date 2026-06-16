lexer grammar SchemaLexer;

Identifier
  : IdentifierStart IdentifierPart* ;

fragment
IdentifierStart
  : [a-zA-Z$_] ;

fragment
IdentifierPart
  : [a-zA-Z0-9$_] ;

// program transformers (meta constructs) usable inside schematic modalities;
// must precede the generic Schema token so they win the (equal-length) lexer match
ExpandFunctionBody
   : 's#expand_function_body' ;

 Schema
   : 's#' Identifier ;
