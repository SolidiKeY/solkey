// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Taclet examples: evaluation order of operands, receivers and indices.
// Conventions: README.md, "Writing an example".
contract EvaluationOrder {
    struct Token { uint value; }
    struct Account { uint balance; Token token; }
    struct Person { Account account; uint age; }
    struct Basket { uint[] items; }
    struct TokenBucket { Token[] tokens; }

    uint[] values;
    uint[] a;
    uint[][] matrix;

    Person[] persons;
    Person alice;
    Person bob;
    Token tok;
    TokenBucket[] buckets;

    // import
    function testMemoryEvaluationOrder() public pure {
        uint[] memory xs = new uint[](3);
        uint i = 0;
        xs[++i] = ++i;
        assert(i == 2);
        assert(xs[2] == 1);
    }

    function testMemoryIndexWriteImpureIndexPrimitiveRhs() public pure {
        uint[] memory xs = new uint[](2);
        uint i = 0;
        xs[i++] = i;
        assert(i == 1);
        assert(xs[0] == 0);
    }

    function testMemoryIndexWriteImpureIndexRefRhs() public pure {
        Token[] memory toks = new Token[](2);
        Token memory tmp;
        tmp.value = 5;
        uint j = 0;
        toks[j++] = tmp;
        assert(j == 1);
        assert(toks[0].value == 5);
    }

    /// @custom:key box
    function testMemoryToStorageIndexCopyImpureIndex() public {
        require(persons.length == 0);
        persons.push();
        persons.push();
        Person memory carol;
        carol.age = 7;
        uint i = 0;
        persons[i++] = carol;
        assert(i == 1);
        assert(persons[0].age == 7);
    }

    // import
    /// @custom:key box
    function testNestedIndexWriteImpureIndexPrimitiveRhs() public {
        require(matrix.length == 0);
        matrix.push();
        matrix.push();
        matrix[1].push(100);
        uint i = 1;
        matrix[i++][0] = i;
        assert(i == 2);
        assert(matrix[1][0] == 1);
    }

    // import
    /// @custom:key box
    function testNestedIndexReadImpureIndex() public {
        require(matrix.length == 0);
        matrix.push();
        matrix.push();
        require(matrix[1].length == 0);
        matrix[1].push(100);
        uint i = 1;
        uint v = matrix[i++][0];
        assert(i == 2);
        assert(v == 100);
    }

    // import
    /// @custom:key box
    function testNestedIndexWriteImpureReceiverAndIndex() public {
        require(matrix.length == 0);
        matrix.push();
        matrix.push();
        require(matrix[0].length == 0);
        require(matrix[1].length == 0);
        matrix[0].push(0);
        matrix[0].push(0);
        matrix[1].push(0);
        matrix[1].push(0);
        uint i = 0;
        matrix[i++][i++] = 77;
        assert(i == 2);
        assert(matrix[0][1] == 77);
        assert(matrix[1][0] == 0);
    }

    // import
    /// @custom:key box
    function testNestedIndexReadImpureReceiverAndIndex() public {
        require(matrix.length == 0);
        matrix.push();
        matrix.push();
        require(matrix[0].length == 0);
        matrix[0].push(0);
        matrix[0].push(11);
        matrix[1].push(22);
        uint i = 0;
        uint v = matrix[i++][i++];
        assert(i == 2);
        assert(v == 11);
    }

    /// @custom:key box
    function testStorageDeleteImpureReceiver() public {
        require(matrix.length == 0);
        matrix.push();
        matrix.push();
        matrix[0].push(9);
        uint i = 0;
        delete matrix[i++][0];
        assert(i == 1);
        assert(matrix[0][0] == 0);
    }

    /// @custom:key box
    function testStoragePushImpureReceiver() public {
        require(matrix.length == 0);
        matrix.push();
        matrix.push();
        require(matrix[0].length == 0);
        uint i = 0;
        matrix[i++].push(i);
        assert(i == 1);
        assert(matrix[0][0] == 1);
    }

    /// @custom:key box
    function testCompoundAssignImpureReceiver() public {
        require(persons.length == 0);
        persons.push();
        persons.push();
        persons[0].age = 5;
        uint i = 0;
        persons[i++].age += i;
        assert(i == 1);
        assert(persons[0].age == 5);
    }

    function testMemoryFieldWriteImpureReceiver() public pure {
        Token[] memory toks = new Token[](2);
        uint i = 0;
        toks[i++].value = i;
        assert(i == 1);
        assert(toks[0].value == 0);
    }

    // import
    /// @custom:key box
    function testIndexWriteReceiverReadsMutatedVar() public {
        require(matrix.length == 0);
        matrix.push();
        matrix.push();
        require(matrix[0].length == 0);
        require(matrix[1].length == 0);
        matrix[0].push(0);
        matrix[1].push(0);
        uint k = 0;
        matrix[k][k++] = 77;
        assert(k == 1);
        assert(matrix[0][0] == 77);
        assert(matrix[1][0] == 0);
    }

    /// @custom:key box
    function testStorageIndexWriteImpureIndexPrimitiveRhs() public {
        require(a.length == 0);
        uint i = 0;
        a.push(100);
        a.push(100);
        a[i++] = i;
        assert(i == 1);
        assert(a[0] == 0);
    }

    /// @custom:key box
    function testStorageIndexWriteImpureIndexRefRhs() public {
        require(persons.length == 0);
        persons.push();
        persons.push();
        bob.age = 0;
        persons[bob.age++] = bob;
        assert(bob.age == 1);
        assert(persons[0].age == 1);
    }

    // import
    /// @custom:key box
    function testStorageEvaluationOrder() public {
        require(a.length == 0);
        uint i = 0;
        a.push(100);
        a.push(100);
        a.push(100);
        a[++i] = ++i;
        assert(a[2] == 1);
    }

    // import
    function testShiftOperandEvaluation() public pure {
        uint x = 8;
        uint r = x >> (x = 2);
        assert(r == 0 && x == 2);
        r = (x + 1) << (x + 2);
        assert(r == 48);
    }

    // ── Reference-source evaluation order ──

    /// @custom:key box
    function storageIndexWriteRefSourceImpureIndex() public {
        require(persons.length == 0);
        persons.push();
        Person memory p;
        persons[p.age++] = p;
        assert(persons[0].age == 1);
    }

    /// @custom:key box
    function storageFieldWriteRefSourceImpureReceiver() public {
        require(persons.length == 0);
        persons.push();
        Account memory acc;
        persons[acc.balance++].account = acc;
        assert(persons[0].account.balance == 1);
    }

    /// @custom:key box
    function storageFieldWriteStorageRefImpureReceiver() public {
        require(persons.length == 0);
        persons.push();
        persons.push();
        alice.account.balance = 9;
        Account storage src = alice.account;
        uint i = 0;
        persons[i++].account = src;
        assert(i == 1);
        assert(persons[0].account.balance == 9);
    }

    /// @custom:key box
    function storageFieldWriteRootRefImpureReceiver() public {
        require(persons.length == 0);
        persons.push();
        persons.push();
        tok.value = 4;
        uint i = 0;
        persons[i++].account.token = tok;
        assert(i == 1);
        assert(persons[0].account.token.value == 4);
    }

    function memoryFieldWriteMemRefImpureReceiver() public pure {
        Person[] memory ps = new Person[](2);
        Account memory src;
        src.balance = 6;
        uint i = 0;
        ps[i++].account = src;
        assert(i == 1);
        assert(ps[0].account.balance == 6);
    }

    /// @custom:key box
    function storageIndexWriteRootRefImpureReceiver() public {
        require(buckets.length == 0);
        buckets.push();
        buckets.push();
        require(buckets[1].tokens.length == 0);
        buckets[1].tokens.push();
        tok.value = 3;
        uint i = 1;
        buckets[i++].tokens[0] = tok;
        assert(i == 2);
        assert(buckets[1].tokens[0].value == 3);
    }

    /// @custom:key box
    function storageIndexWriteStorageRefImpureReceiver() public {
        require(buckets.length == 0);
        buckets.push();
        buckets.push();
        require(buckets[1].tokens.length == 0);
        buckets[1].tokens.push();
        tok.value = 8;
        Token storage src = tok;
        uint i = 1;
        buckets[i++].tokens[0] = src;
        assert(i == 2);
        assert(buckets[1].tokens[0].value == 8);
    }

    /// @custom:key box
    function memoryToStorageIndexImpureReceiver() public {
        require(buckets.length == 0);
        buckets.push();
        buckets.push();
        require(buckets[1].tokens.length == 0);
        buckets[1].tokens.push();
        Token memory src;
        src.value = 5;
        uint i = 1;
        buckets[i++].tokens[0] = src;
        assert(i == 2);
        assert(buckets[1].tokens[0].value == 5);
    }

    function memoryIndexWriteMemRefImpureReceiver() public pure {
        TokenBucket[] memory tbs = new TokenBucket[](2);
        Token[] memory slots = new Token[](2);
        tbs[1].tokens = slots;
        Token memory src;
        src.value = 2;
        uint i = 1;
        tbs[i++].tokens[0] = src;
        assert(i == 2);
        assert(tbs[1].tokens[0].value == 2);
    }

    // ── Binary operand evaluation order: solc evaluates the right operand first ──

    // import
    function additionLeftImpureRightReadFirst() public pure {
        uint i = 1;
        uint x = i++ + i;
        assert(x == 2);
        assert(i == 2);
    }

    // import
    function additionRightImpure() public pure {
        uint i = 1;
        uint x = i + i++;
        assert(x == 3);
        assert(i == 2);
    }

    // import
    function additionBothOperandsImpure() public pure {
        uint i = 1;
        uint x = i++ + i++;
        assert(x == 3);
        assert(i == 3);
    }

    // import
    function subtractionLeftImpureRightReadFirst() public pure {
        uint i = 5;
        uint x = i++ - i;
        assert(x == 0);
        assert(i == 6);
    }

    // import
    function lessThanLeftImpureRightReadFirst() public pure {
        uint i = 1;
        bool b = i++ < i;
        assert(!b);
        assert(i == 2);
    }

    // ── Index writes whose receiver and index are both impure (docs/storage.md section 5) ──

    // import
    /// @custom:key box
    function indexWriteBothImpureStorageRef() public {
        require(buckets.length == 0);
        buckets.push();
        buckets.push();
        require(buckets[1].tokens.length == 0);
        buckets[1].tokens.push();
        buckets[1].tokens.push();
        tok.value = 8;
        Token storage src = tok;
        uint i = 1;
        uint j = 0;
        buckets[i++].tokens[j++] = src;
        assert(i == 2);
        assert(j == 1);
        assert(buckets[1].tokens[0].value == 8);
    }

    // import
    /// @custom:key box
    function indexWriteBothImpureMemToStorage() public {
        require(buckets.length == 0);
        buckets.push();
        buckets.push();
        require(buckets[1].tokens.length == 0);
        buckets[1].tokens.push();
        buckets[1].tokens.push();
        Token memory src;
        src.value = 5;
        uint i = 1;
        uint j = 0;
        buckets[i++].tokens[j++] = src;
        assert(i == 2);
        assert(j == 1);
        assert(buckets[1].tokens[0].value == 5);
    }

    // import
    function indexWriteBothImpureMemoryValue() public pure {
        Basket[] memory bs = new Basket[](2);
        uint[] memory slots = new uint[](2);
        bs[1].items = slots;
        uint i = 1;
        uint j = 0;
        bs[i++].items[j++] = 42;
        assert(i == 2);
        assert(j == 1);
        assert(bs[1].items[0] == 42);
    }

    // import
    function indexWriteBothImpureMemRef() public pure {
        TokenBucket[] memory tbs = new TokenBucket[](2);
        Token[] memory slots = new Token[](2);
        tbs[1].tokens = slots;
        Token memory src;
        src.value = 2;
        uint i = 1;
        uint j = 0;
        tbs[i++].tokens[j++] = src;
        assert(i == 2);
        assert(j == 1);
        assert(tbs[1].tokens[0].value == 2);
    }

    // ── Taclet coverage: one function per taclet no other example applies ──

    function mutateStorageCopySource() internal returns (uint) {
        values[0] = 9;
        return 0;
    }

    function memoryIndexWriteStorageRefImpureIndex() public {
        values = new uint[](1);
        values[0] = 7;
        uint[][][] memory xs = new uint[][][](1);
        xs[0] = new uint[][](1);
        xs[0][mutateStorageCopySource()] = values;
        assert(xs[0][0][0] == 7);
        assert(values[0] == 9);
    }
}
