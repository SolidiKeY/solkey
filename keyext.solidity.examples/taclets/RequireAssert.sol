// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Taclet examples: `require` and `assert` under box and diamond.
// Conventions: README.md, "Writing an example".
contract RequireAssert {
    struct Token { uint value; }
    struct Account { uint balance; Token token; }
    struct Person { Account account; uint age; }
    struct TokenBucket { Token[] tokens; }

    uint total;
    uint age;

    uint[] values;
    uint[][] matrix;
    Person alice;
    Person bob;
    TokenBucket bucket;

    /// @custom:key box
    function requireGuardBox(uint x) public {
        require(x > 0);
        age = 1;
        uint r = age;
        assert(r == 1);
    }

    function requireHoldsDiamond() public {
        uint x = 5;
        require(x > 0);
        total = x;
        uint r = total;
        assert(r == 5);
    }

    // ── Require / assert: literal operand ──

    function requireTrueLiteral() public pure {
        require(true);
        assert(true);
    }

    /// @custom:key box
    function requireFalseLiteral() public pure {
        require(false);
        assert(false);
    }

    // ── mainFeatures ──

    /// @custom:key box
    function testStorageComplexReceiverEmptyPush() public {
        require(bucket.tokens.length == 0);
        bucket.tokens.push();
        Token[] storage bt = bucket.tokens;
        assert(bt.length == 1);
    }

    // ── split: one function per modality ──

    function storageFieldDecomposition() public {
        alice.account.balance = 34;
        assert(alice.account.balance == 34);
        uint r = alice.account.balance;
        assert(r == 34);
    }

    function storageFieldDeepValue() public {
        alice.account.token.value = 7;
        assert(alice.account.token.value == 7);
        uint r = alice.account.token.value;
        assert(r == 7);
    }

    /// @custom:key box
    function storageIndexDecomposition() public {
        require(2 < matrix.length);
        require(3 < matrix[2].length);
        matrix[2][3] = 34;
        assert(matrix[2][3] == 34);
        uint r = matrix[2][3];
        assert(r == 34);
    }

    /// @custom:key box
    function storageIndexRootArray() public {
        require(1 < values.length);
        values[1] = 42;
        assert(values[1] == 42);
        uint r = values[1];
        assert(r == 42);
    }

    // ── shared: both conjuncts run the same program ──

    function storageFieldDisjointFields() public {
        alice.account.balance = 1;
        alice.account.token.value = 2;
        assert(alice.account.balance == 1);
        assert(alice.account.token.value == 2);
    }

    function storageFieldDisjointRoots() public {
        alice.age = 1;
        bob.age = 2;
        assert(alice.age == 1);
        assert(bob.age == 2);
    }

    // ── several observations: asserted in the body ──

    function memoryAssignForms() public {
        alice.age = 27;
        Person memory carol;
        carol = alice;
        alice.age = 30;
        uint r1 = carol.age;
        uint[] memory xs;
        xs = new uint[](4);
        xs[1] = 33;
        uint r2 = xs[1];
        assert(r1 == 27);
        assert(r2 == 33);
    }

    function memoryDelete() public pure {
        Person memory carol;
        Account memory acc = carol.account;
        carol.age = 20;
        acc.balance = 100;
        delete carol.age;
        delete carol.account;
        uint result = carol.age;
        uint aliasBalance = acc.balance;
        uint newBalance = carol.account.balance;
        assert(result == 0);
        assert(aliasBalance == 100);
        assert(newBalance == 0);
    }

    function storageAliasRebindOriginal() public {
        uint before = alice.age;
        Person storage alicePath = alice;
        bob.age = 20;
        alicePath = bob;
        assert(alice.age == before);
    }

    function testSimpleAssert() public {
        age = 42;
        uint v = age;
        assert(v == 42);
    }
}
