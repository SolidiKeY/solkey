lexer grammar SchemaLexer;

NON_KEYWORD_IDENTIFIER
   : [a-zA-Z$_] ;

 SCHEMA_IDENTIFIER
   : 's#' NON_KEYWORD_IDENTIFIER ;
