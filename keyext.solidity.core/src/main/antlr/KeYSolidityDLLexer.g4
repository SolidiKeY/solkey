lexer grammar KeYSolidityDLLexer;

import KeYLexer;

@header
{
package org.key_project.solidity.parser;
}

SAME_AS_TERM   : '\\sameAsTerm';
TYPE_BOUNDS : '\\typeBounds';
SIGNED_TYPE_BOUNDS : '\\signedTypeBounds';
FIELD_TYPE_BOUNDS : '\\fieldTypeBounds';
ELEMENT_TYPE_BOUNDS : '\\elementTypeBounds';
HAS_FIELD_SORT: '\\hasFieldSort';
HAS_MEMORY_FIELD_SORT: '\\hasMemoryFieldSort';
HAS_ELEMENT_SORT: '\\hasElementSort';
HAS_MEMORY_ELEMENT_SORT: '\\hasMemoryElementSort';
NEW_LOCAL_VARS : '\\newLocalVars';
STORE_TERM_IN : '\\storeTermIn';
STORE_EXPR_IN : '\\storeExprIn';
HAS_INVARIANT : '\\hasInvariant';
GET_INVARIANT : '\\getInvariant';
GET_VARIANT   : '\\getVariant';
IS_LABELED    : '\\isLabeled';
DIFFERENT     : '\\different';
NO_FREE_VAR_IN : '\\noFreeVarIn';

ALIAS: '\\alias';

OPENTYPEPARAMS:'<' '[';
CLOSETYPEPARAMS:']' '>';

SORT: '\\sort';

NON_RIGID: '\\nonRigid';

CONST : 'const';

CHOOSECONTRACT : '\\chooseContract';
CONTRACTS : '\\contracts';
INVARIANTS : '\\invariants';
