// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract PaperTest {
    struct Token {
        uint value;
    }

    struct Account {
        int balance;
        Token token;
    }

    struct Person {
        uint age;
        Account account;
    }

    struct Ledger {
        uint nonce;
        mapping(uint => uint) balances;
    }

    struct TokenBucket {
        Token[] tokens;
    }

    Person alice;
    Person bob;
    Ledger ledger;
    Token[] tokens;
    Person[] people;
    uint[] a;
    uint[] values;
    TokenBucket bucket;
    mapping(uint => uint) valuesMap;
    mapping(uint => Account) accountMap;
}
