lexer grammar KeYSolidityDLLexer;

import KeYLexer;

@header
{
package org.key_project.solidity.parser;
}

NEW_LOCAL_VARS: '\\newLocalVars';
STORE_TERM_IN : '\\storeTermIn';
STORE_EXPR_IN : '\\storeExprIn';
HAS_INVARIANT : '\\hasInvariant';
GET_INVARIANT : '\\getInvariant';
GET_VARIANT   : '\\getVariant';
IS_LABELED    : '\\isLabeled';
DIFFERENT     : '\\different';
NO_FREE_VAR_IN : '\\noFreeVarIn';

OPENTYPEPARAMS:'<' '[';
CLOSETYPEPARAMS:']' '>';

SORT: '\\sort';

NON_RIGID: '\\nonRigid';

CONST : 'const';
