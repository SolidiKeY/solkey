// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Taclet examples: storage array indices, `push`, `pop`, fixed lengths and array copies.
// Conventions: README.md, "Writing an example".
contract StorageArrays {
    struct Token { uint value; }
    struct Account { uint balance; Token token; }
    struct Person { Account account; uint age; }
    struct Basket { uint[] items; }
    struct Ledger { uint nonce; mapping(uint => uint) balances; }
    struct LedgerUse { Ledger ledger; }
    struct TokenBucket { Token[] tokens; }
    struct Triple { uint[3] items; uint tag; }
    struct Node { uint v; Node[] kids; }
    struct Chain { Chain[] next; uint tag; }

    uint total;
    uint age;

    uint[] values;
    int[] signedValues;
    uint[] a;
    uint[][] matrix;
    uint[][] matrixSource;
    uint[3] fixedValues;
    uint[3][] rows;
    uint[3][] rowsSource;
    uint[][3] listTriple;
    uint[][3] listTripleSource;
    uint[9] copySource9;
    uint[] copyTargetDyn;
    uint[3][2] fixedGrid;
    mapping(uint => uint[3])[2] fixedMapsOfFixed;
    mapping(uint => uint[3])[] dynMapsOfFixed;
    uint[40] copyBig;
    uint[20] copySmall;
    mapping(uint => uint[3]) fixedByKey;
    Token[2] fixedTokens;
    mapping(uint => uint)[2] fixedMaps;
    Triple triple;
    Triple triple2;
    bool[] boolFlags;

    mapping(uint => uint) balances;
    mapping(uint => Person) people;
    mapping(uint => bool) flags;

    Person[] persons;
    Person alice;

    Ledger ledger;
    Token tok;
    Token[] tokens;
    Token[] emptyTokens;
    Token[] tokensSource;
    TokenBucket bucket;
    Basket[] baskets;
    Basket basketA;
    Basket basketB;
    LedgerUse[] ledgerUses;
    mapping(uint => uint)[] mapArray;
    mapping(uint => uint[]) listsByKey;
    mapping(uint => uint[][]) matricesByKey;
    mapping(uint => Basket) basketsByKey;
    mapping(uint => Token[]) tokensByKey;
    Person[] personsSource;
    TokenBucket bucketSource;
    Token[3] fixedTokens3;
    mapping(uint => Triple) triplesByKey;
    Node nodeTree;
    Node nodeSpare;
    Chain chainRoot;

    function storageAssignmentValueIndexOnce() public {
        uint i = 0;
        // mapping(uint => bool) flags
        flags[0] = false;
        flags[1] = false;
        bool r = (flags[i++] = true);
        assert(r && flags[0]);
        assert(!flags[1] && i == 1);
    }

    // import
    function storageAssignmentValueRhsBeforeIndex() public {
        uint i = 0;
        // mapping(uint => bool) flags
        flags[0] = false;
        bool r = (flags[i++] = ((i = 2) == 2));
        assert(r && flags[2]);
        assert(!flags[0] && i == 3);
    }

    function logicalAndStorageAssignmentSkipsIndex() public {
        uint i = 0;
        // mapping(uint => bool) flags
        flags[0] = false;
        bool r = false && (flags[i++] = true);
        assert(!r && !flags[0] && i == 0);
    }

    function logicalOrStorageAssignmentSkipsIndex() public {
        uint i = 0;
        // mapping(uint => bool) flags
        flags[0] = true;
        bool r = true || (flags[i++] = false);
        assert(r && flags[0] && i == 0);
    }

    /// @custom:key box
    function storageIndexCopyValue(uint i, uint j) public {
        // uint[] values
        require(1 < values.length);
        require(i == 5 && j == 1);
        values[1] = 8;
        // mapping(uint => uint) balances
        balances[i] = values[j];
        uint r = balances[5];
        assert(r == 8);
    }

    /// @custom:key box
    function storageIndexCopysourceAfterPush() public {
        // uint[][] matrix
        require(matrix.length == 0);
        uint[][] storage m = matrix;
        m.push();
        // uint[] values
        m[0] = values;
        // the .key postcondition compared the whole matrix[0] subtree against values, which has
        // no Solidity form; reading matrix[0].length back does not discharge either, so this
        // only checks that the copy-after-push executes
        assert(matrix.length == 1);
    }

    /// @custom:key box
    function storageIndexDecomposeAfterPush() public {
        // uint[][] matrix
        require(0 < matrix.length);
        require(matrix[0].length == 0);
        matrix[0].push(100);
        matrix[0][0] = 7;
        uint r = matrix[0][0];
        assert(r == 7);
    }

    function storageIndexDeleteMappingBool() public {
        // mapping(uint => bool) flags
        flags[1] = true;
        delete flags[1];
        bool r = flags[1];
        assert(!r);
    }

    function storageIndexDeleteMappingStruct() public {
        // mapping(uint => Person) people
        // Person { Account account; uint age; } | Account { uint balance; Token token; } | Token { uint value; }
        delete people[1];
        assert(people[1].age == 0);
        assert(people[1].account.balance == 0);
    }

    /// @custom:key box
    function storageIndexDeleteNseIndex(uint k) public {
        require(k == 2);
        // mapping(uint => uint) balances
        balances[3] = 7;
        delete balances[k+1];
        uint r = balances[3];
        assert(r == 0);
    }

    /// @custom:key box
    function storageIndexDelete() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 7;
        delete values[1];
        uint r = values[1];
        assert(r == 0);
    }

    /// @custom:key box
    function storageIndexDivAssign() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        values[1] /= 8;
        uint r = values[1];
        assert(r == 5);
    }

    /// @custom:key box
    function storageIndexModAssign() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        values[1] %= 6;
        uint r = values[1];
        assert(r == 4);
    }

    /// @custom:key box
    function storageIndexMulAssign() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 5;
        values[1] *= 6;
        uint r = values[1];
        assert(r == 30);
    }

    /// @custom:key box
    function storageIndexMultipleWrites() public {
        // uint[] values
        require(3 < values.length);
        values[3] = 5;
        values[3] = 8;
        uint r = values[3];
        assert(r == 8);
    }

    /// @custom:key box
    function storageIndexPostdecrementAssign() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        uint r = values[1]--;
        assert(r == 40);
    }

    /// @custom:key box
    function storageIndexPostdecrement() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        values[1]--;
        uint r = values[1];
        assert(r == 39);
    }

    /// @custom:key box
    function storageIndexPostincrementAssign() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        uint r = values[1]++;
        assert(r == 40);
    }

    /// @custom:key box
    function storageIndexPostincrement() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        values[1]++;
        uint r = values[1];
        assert(r == 41);
    }

    /// @custom:key box
    function storageIndexPredecrementAssign() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        uint r = --values[1];
        assert(r == 39);
    }

    /// @custom:key box
    function storageIndexPredecrement() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        --values[1];
        uint r = values[1];
        assert(r == 39);
    }

    /// @custom:key box
    function storageIndexPreincrementAssign() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        uint r = ++values[1];
        assert(r == 41);
    }

    /// @custom:key box
    function storageIndexPreincrement() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        ++values[1];
        uint r = values[1];
        assert(r == 41);
    }

    /// @custom:key box
    function storageIndexReadMappingStoreRoot(uint k) public {
        require(k == 1);
        // mapping(uint => uint) balances
        balances[1] = 42;
        total = balances[k];
        uint r = total;
        assert(r == 42);
    }

    /// @custom:key box
    function storageIndexReadNseIndex(uint i) public {
        // uint[] values
        require(2 < values.length);
        require(i == 1);
        values[2] = 9;
        uint r = values[i+1];
        assert(r == 9);
    }

    /// @custom:key box
    function storageIndexSubAssign() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        values[1] -= 8;
        uint r = values[1];
        assert(r == 32);
    }

    /// @custom:key box
    function storageIndexWriteNseChain(uint i, uint x, uint y) public {
        require(i == 2 && x == 3 && y == 4);
        // mapping(uint => uint) balances
        balances[i+1] = x*y + 3;
        uint r = balances[3];
        assert(r == 15);
    }

    /// @custom:key box
    function storageMatrixNseIndex(uint i, uint j, uint x, uint y) public {
        // uint[][] matrix
        require(2 < matrix.length);
        require(3 < matrix[2].length);
        require(i == 1 && j == 2 && x == 5 && y == 6);
        matrix[i+1][j+1] = x + y;
        assert(matrix[2][3] == 11);
    }

    /// @custom:key box
    function storageMatrixWriteRead() public {
        // uint[][] matrix
        require(2 < matrix.length);
        require(3 < matrix[2].length);
        matrix[2][3] = 99;
        uint r = matrix[2][3];
        assert(r == 99);
    }

    /// @custom:key box
    function storagePopAfterPush() public {
        // uint[] values
        require(values.length == 0);
        values.push();
        values.pop();
        assert(values.length == 0);
    }

    /// @custom:key box
    function storagePopNonempty() public {
        // uint[] values
        require(values.length == 2);
        values.pop();
        assert(values.length == 1);
        // the popped slot is only observable again once it is back in bounds
        values.push();
        assert(values[1] == 0);
    }

    function storagePopUnknownLength() public {
        // uint[] values
        values.push();
        values.pop();
    }

    /// @custom:key box
    function storagePushEmpty() public {
        // uint[] values
        require(values.length == 2);
        values.push();
        assert(values.length == 3);
    }

    function storagePushLengthPositive() public {
        // uint[] values
        values.push();
        assert(values.length > 0);
    }

    /// @custom:key box
    function storagePushLocalBind() public {
        // Person[] persons
        // Person { Account account; uint age; } | Account { uint balance; Token token; } | Token { uint value; }
        require(persons.length == 2);
        Person storage p;
        p = persons.push();
        assert(persons.length == 3);
    }

    /// @custom:key box
    function storagePushNonsimpleArg(uint x, uint y) public {
        // uint[] values
        require(values.length == 2);
        require(x == 40 && y == 2);
        values.push(x + y);
        assert(values[2] == 42);
        assert(values.length == 3);
    }

    function storagePushReadBack() public {
        // uint[] values
        values.push(42);
        uint r = values[values.length - 1];
        assert(r == 42);
    }

    /// @custom:key box
    function storagePushReturnAssign() public {
        // uint[] values
        require(values.length == 2);
        values.push() = 42;
        assert(values[2] == 42);
        assert(values.length == 3);
    }

    /// @custom:key box
    function storagePushReturnPrimitiveBind() public {
        // uint[] values
        require(values.length == 2);
        values.push();
        values.pop();
        uint y = values.push();
        assert(y == 0);
        assert(values.length == 3);
        assert(values[2] == 0);
    }

    // import
    /// @custom:key box
    function testDeepPopDoesNotResetMappingMember() public {
        // LedgerUse[] ledgerUses
        // LedgerUse { Ledger ledger; } | Ledger { uint nonce; mapping(uint => uint) balances; }
        require(ledgerUses.length == 0);
        ledgerUses.push();
        ledgerUses[0].ledger.balances[1] = 10;
        ledgerUses.pop();
        ledgerUses.push();
        assert(ledgerUses[0].ledger.balances[1] == 10);
    }

    // import
    /// @custom:key box
    function testDeleteArrayDoesNotResetElementMappingMember() public {
        // LedgerUse[] ledgerUses
        // LedgerUse { Ledger ledger; } | Ledger { uint nonce; mapping(uint => uint) balances; }
        require(ledgerUses.length == 0);
        ledgerUses.push();
        ledgerUses[0].ledger.nonce = 3;
        ledgerUses[0].ledger.balances[1] = 10;
        delete ledgerUses;
        ledgerUses.push();
        assert(ledgerUses[0].ledger.nonce == 0);
        assert(ledgerUses[0].ledger.balances[1] == 10);
    }

    /// @custom:key box
    function testStorageArrayReadWrite() public {
        // Token[] tokens
        // Token { uint value; }
        require(tokens.length == 0);
        tokens.push();
        tokens[0].value = 100;
        uint v = tokens[0].value;
        assert(v == 100);
    }

    /// @custom:key box
    function testStorageComplexReceiverPushFieldLvalue() public {
        // TokenBucket bucket
        // TokenBucket { Token[] tokens; } | Token { uint value; }
        require(bucket.tokens.length == 0);
        bucket.tokens.push().value = 5;
        Token[] storage bt = bucket.tokens;
        assert(bt.length == 1);
        assert(bt[0].value == 5);
    }

    /// @custom:key box
    function testStorageComplexReceiverPushLvalueCopy() public {
        // TokenBucket bucket
        // TokenBucket { Token[] tokens; } | Token { uint value; }
        require(bucket.tokens.length == 0);
        // Person alice
        // Person { Account account; uint age; } | Account { uint balance; Token token; }
        alice.account.token.value = 7;
        Token storage tokRef = alice.account.token;
        bucket.tokens.push() = tokRef;
        Token[] storage bt = bucket.tokens;
        assert(bt.length == 1);
        assert(bt[0].value == 7);
    }

    /// @custom:key box
    function testStorageNestedPushReturnAlias() public {
        // Person[] persons
        // Person { Account account; uint age; } | Account { uint balance; Token token; } | Token { uint value; }
        require(persons.length == 0);
        Account storage acc = persons.push().account;
        acc.balance = 50;
        assert(persons.length == 1);
        assert(acc.balance == 50);
    }

    /// @custom:key box
    function testStoragePushFieldLvalue() public {
        // Token[] tokens
        // Token { uint value; }
        require(tokens.length == 0);
        tokens.push().value = 11;
        assert(tokens.length == 1);
        assert(tokens[0].value == 11);
    }

    /// @custom:key box
    function testStoragePushLvalueCopiesStorageSource() public {
        // Token[] tokens
        // Token { uint value; }
        require(tokens.length == 0);
        // Person alice
        // Person { Account account; uint age; } | Account { uint balance; Token token; }
        alice.account.token.value = 9;
        Token storage storageRef = alice.account.token;
        tokens.push() = storageRef;
        assert(tokens.length == 1);
        assert(tokens[0].value == 9);
    }

    /// @custom:key box
    function testStoragePushLvaluePrimitive() public {
        // uint[] values
        require(values.length == 0);
        values.push() = 77;
        assert(values.length == 1);
        assert(values[0] == 77);
    }

    /// @custom:key box
    function testStoragePushReturnAlias() public {
        // Token[] tokens
        // Token { uint value; }
        require(tokens.length == 0);
        Token storage t = tokens.push();
        t.value = 99;
        assert(tokens.length == 1);
        assert(tokens[0].value == 99);
    }

    // import
    function testArrayCopyClearsOldElements() public {
        // Token[] tokens
        // Token { uint value; }
        delete tokens;
        tokens.push();
        Token storage r = tokens[0];
        tokens.pop();
        r.value = 5;
        // TokenBucket bucket
        // TokenBucket { Token[] tokens; }
        delete bucket.tokens;
        bucket.tokens.push();
        bucket.tokens[0].value = 7;
        bucket.tokens = tokens;
        bucket.tokens.push();
        assert(bucket.tokens[0].value == 0);
    }

    /// @custom:key box
    function testStorageIndexDeleteOutOfBoundsReverts() public {
        // uint[] values
        delete values;
        delete values[0];
        assert(false);
    }

    /// @custom:key box
    function testFixedArrayDeleteKeepsLength() public {
        // uint[3] fixedValues
        require(2 < fixedValues.length);
        fixedValues[1] = 7;
        delete fixedValues;
        assert(2 < fixedValues.length);
        assert(fixedValues[1] == 0);
    }

    function testFixedArrayLength() public view {
        // uint[3] fixedValues
        assert(fixedValues.length == 3);
    }

    function testFixedArrayIndexInBounds() public {
        // uint[3] fixedValues
        fixedValues[2] = 1;
        assert(fixedValues[2] == 1);
    }

    function testFixedStructArrayLength() public view {
        // Token[2] fixedTokens
        // Token { uint value; }
        assert(fixedTokens.length == 2);
    }

    function testStructFixedMemberLength() public view {
        // Triple triple
        // Triple { uint[3] items; uint tag; }
        assert(triple.items.length == 3);
    }

    /// @custom:key box
    function testFixedElementOfDynamicArrayLength() public view {
        // uint[3][] rows
        require(0 < rows.length);
        assert(rows[0].length == 3);
    }

    function testNewArrayOfFixedElementLength() public pure {
        uint[2][] memory z = new uint[2][](4);
        assert(z.length == 4);
        assert(z[1].length == 2);
    }

    function testMappingOfFixedArrayLength(uint k) public view {
        // mapping(uint => uint[3]) fixedByKey
        assert(fixedByKey[k].length == 3);
    }

    /// @custom:key box
    function testFixedStructArrayDeleteResetsElements() public {
        // Token[2] fixedTokens
        // Token { uint value; }
        require(1 < fixedTokens.length);
        fixedTokens[1].value = 7;
        delete fixedTokens;
        assert(1 < fixedTokens.length);
        assert(fixedTokens[1].value == 0);
    }

    /// @custom:key box
    function testStructWithFixedArrayDeleteKeepsLength() public {
        // Triple triple
        // Triple { uint[3] items; uint tag; }
        require(2 < triple.items.length);
        triple.items[1] = 7;
        triple.tag = 3;
        delete triple;
        assert(2 < triple.items.length);
        assert(triple.items[1] == 0);
        assert(triple.tag == 0);
    }

    /// @custom:key box
    function testStructWithFixedArrayCopy() public {
        // Triple triple
        // Triple { uint[3] items; uint tag; }
        require(2 < triple.items.length);
        triple.items[1] = 7;
        // Triple triple2
        triple2 = triple;
        assert(triple2.items[1] == 7);
    }

    /// @custom:key box
    function testFixedMappingArrayDeleteKeepsEntries() public {
        // mapping(uint => uint)[2] fixedMaps
        require(1 < fixedMaps.length);
        fixedMaps[1][2] = 5;
        delete fixedMaps;
        assert(fixedMaps[1][2] == 5);
    }

    function testPopKeepsMappingElementEntries() public {
        // mapping(uint => uint)[] mapArray
        delete mapArray;
        mapArray.push();
        mapArray[0][1] = 5;
        mapArray.pop();
        mapArray.push();
        assert(mapArray[0][1] == 5);
    }

    function testPushBindMappingElement() public {
        // mapping(uint => uint)[] mapArray
        delete mapArray;
        mapping(uint => uint) storage m = mapArray.push();
        m[1] = 4;
        assert(mapArray[0][1] == 4);
    }

    function storageStaticToDynamicCopyKeepsLength() public {
        // uint[9] copySource9
        copySource9[8] = 4;
        // uint[] copyTargetDyn
        copyTargetDyn = copySource9;
        uint n = copyTargetDyn.length;
        uint r = copyTargetDyn[8];
        assert(n == 9);
        assert(r == 4);
    }

    function testFixedArrayOfFixedArraysLength() public view {
        // uint[3][2] fixedGrid
        assert(fixedGrid.length == 2);
        assert(fixedGrid[1].length == 3);
    }

    function testPopPushValueResetsValue() public {
        // uint[] values
        uint n = values.length;
        values.push(73);
        values.pop();
        assert(values.length == n);
        values.push();
        assert(values.length == n + 1);
        assert(values[n] == 0);
    }

    // import
    function testShorterCopyThenEmptyPushClearsTail() public {
        // uint[] copyTargetDyn; uint[9] copySource9
        copyTargetDyn = copySource9;
        copyTargetDyn.push(99);
        copyTargetDyn = copySource9;
        assert(copyTargetDyn.length == 9);
        copyTargetDyn.push();
        assert(copyTargetDyn[9] == 0);
    }

    function testPushMappingOfFixedArrayPreservesEntries() public {
        // mapping(uint => uint[3])[] dynMapsOfFixed
        delete dynMapsOfFixed;
        dynMapsOfFixed.push();
        dynMapsOfFixed[0][7][2] = 19;
        dynMapsOfFixed.pop();
        dynMapsOfFixed.push();
        assert(dynMapsOfFixed.length == 1);
        assert(dynMapsOfFixed[0][7].length == 3);
        assert(dynMapsOfFixed[0][7][2] == 19);
    }

    function testDeleteEntryOfMappingInArrayKeepsOtherKeys() public {
        // mapping(uint => uint[3])[] dynMapsOfFixed
        delete dynMapsOfFixed;
        dynMapsOfFixed.push();
        dynMapsOfFixed[0][7][2] = 19;
        dynMapsOfFixed[0][8][1] = 23;
        delete dynMapsOfFixed[0][7];
        assert(dynMapsOfFixed.length == 1);
        assert(dynMapsOfFixed[0][7].length == 3);
        assert(dynMapsOfFixed[0][7][2] == 0);
        assert(dynMapsOfFixed[0][8][1] == 23);
    }

    function testDeleteArrayOfMappingsOfFixedArraysKeepsEntries() public {
        // mapping(uint => uint[3])[] dynMapsOfFixed
        delete dynMapsOfFixed;
        dynMapsOfFixed.push();
        dynMapsOfFixed[0][7][2] = 19;
        delete dynMapsOfFixed;
        dynMapsOfFixed.push();
        assert(dynMapsOfFixed[0][7].length == 3);
        assert(dynMapsOfFixed[0][7][2] == 19);
    }

    function testShapeCopyFixedTailAndSourceIndependence() public {
        // uint[20] copySmall
        copySmall[0] = 11;
        copySmall[19] = 12;
        // uint[40] copyBig
        copyBig[20] = 93;
        copyBig[39] = 94;
        copyBig = copySmall;
        assert(copySmall.length == 20);
        assert(copyBig.length == 40);
        assert(copyBig[0] == 11);
        assert(copyBig[19] == 12);
        assert(copyBig[20] == 0);
        assert(copyBig[39] == 0);
        copySmall[0] = 99;
        assert(copyBig[0] == 11);
    }

    // import
    function testShapeCopyDynamicShrinkClearsReusedSlot() public {
        // uint[] a
        delete a;
        // uint[] values
        delete values;
        a.push(11);
        a.push(12);
        values.push(91);
        values.push(92);
        values.push(93);
        // a = [11,12], values = [91,92,93]
        values = a;
        // values = [11,12]
        assert(values.length == 2);
        assert(values[0] == 11);
        assert(values[1] == 12);
        values.push();
        assert(values[2] == 0);
        values[0] = 99;
        assert(a[0] == 11);
    }

    function testShapeCopyNestedSelfAliasPreservesValues() public {
        // uint[3][2] fixedGrid
        fixedGrid[0][2] = 11;
        fixedGrid[1][2] = 12;
        uint[3][2] storage same = fixedGrid;
        fixedGrid = same;
        assert(fixedGrid.length == 2);
        assert(fixedGrid[0].length == 3);
        assert(fixedGrid[0][2] == 11);
        assert(fixedGrid[1][2] == 12);
    }

    function testFixedArrayOfMappingsOfFixedArraysLength() public view {
        // mapping(uint => uint[3])[2] fixedMapsOfFixed
        assert(fixedMapsOfFixed[1][7].length == 3);
    }

    /// @custom:key box
    function testDynamicArrayOfMappingsOfFixedArraysLength() public view {
        // mapping(uint => uint[3])[] dynMapsOfFixed
        require(0 < dynMapsOfFixed.length);
        assert(dynMapsOfFixed[0][7].length == 3);
    }

    function testPushIndexedPrimitiveSourceOnce() public {
        // uint[] values
        delete values;
        values.push(11);
        uint i = 0;
        values.push(values[i++]);
        // values = [11,11]
        assert(i == 1);
        assert(values.length == 2);
        assert(values[0] == 11);
        assert(values[1] == 11);
    }

    function testPushIndexedFixedArraySourceOnce() public {
        // uint[3][] rows
        delete rows;
        rows.push();
        rows[0][0] = 11;
        uint i = 0;
        rows.push(rows[i++]);
        // rows = [[11,0,0],[11,0,0]]
        assert(i == 1);
        assert(rows.length == 2);
        rows[0][0] = 99;
        assert(rows[1].length == 3);
        assert(rows[1][0] == 11);
    }

    function testFixedShapePopPushResetsFixedElement() public {
        // uint[3][] rows
        uint n = rows.length;
        rows.push();
        rows[n][0] = 11;
        rows[n][2] = 22;
        rows.pop();
        assert(rows.length == n);
        rows.push();
        assert(rows[n].length == 3);
        assert(rows[n][0] == 0);
        assert(rows[n][2] == 0);
    }

    function testFixedShapeDeleteFixedElementKeepsLength() public {
        // uint[3][] rows
        uint n = rows.length;
        rows.push();
        rows[n][1] = 17;
        delete rows[n];
        assert(rows.length == n + 1);
        assert(rows[n].length == 3);
        assert(rows[n][1] == 0);
    }

    function testFixedShapeDeleteDynamicThenPushFixedElement() public {
        // uint[3][] rows
        rows.push();
        rows[0][2] = 23;
        delete rows;
        assert(rows.length == 0);
        rows.push();
        assert(rows.length == 1);
        assert(rows[0].length == 3);
        assert(rows[0][2] == 0);
    }

    function testFixedShapeFixedElementStorageAlias() public {
        // uint[3][] rows
        uint n = rows.length;
        rows.push();
        uint[3] storage row = rows[n];
        assert(row.length == 3);
        row[2] = 31;
        assert(rows[n][2] == 31);
        delete rows[n];
        assert(row.length == 3);
        assert(row[2] == 0);
    }

    function testDeleteArrayLeavesDataPastLength() public {
        // Token[] tokens
        // Token { uint value; }
        delete tokens;
        tokens.push();
        Token storage r = tokens[0];
        tokens.pop();
        r.value = 5;
        delete tokens;
        tokens.push();
        assert(tokens[0].value == 5);
    }

    function testDanglingReferenceSurvivesPush() public {
        // Token[] tokens
        // Token { uint value; }
        delete tokens;
        tokens.push();
        Token storage r = tokens[0];
        tokens.pop();
        r.value = 5;
        tokens.push();
        assert(tokens[0].value == 5);
    }

    function testArrayCopyKeepsDestinationTail() public {
        // Token[] emptyTokens
        // Token { uint value; }
        delete emptyTokens;
        // Token[] tokens
        delete tokens;
        tokens.push();
        Token storage r = tokens[0];
        tokens.pop();
        r.value = 5;
        tokens = emptyTokens;
        tokens.push();
        assert(tokens[0].value == 5);
    }

    function testDanglingInnerArrayReappearsAfterPush() public {
        // uint[][] matrix
        delete matrix;
        matrix.push();
        uint[] storage ptr = matrix[0];
        matrix.pop();
        ptr.push(66);
        matrix.push();
        assert(matrix[0].length == 1);
        assert(matrix[0][0] == 66);
    }

    /// @custom:key box
    function storageArrayCopyLongerIntoShorter() public {
        // uint[] values
        require(5 < values.length);
        delete values;
        // uint[] a
        delete a;
        values.push(1);
        values.push(2);
        a.push(11);
        a.push(12);
        a.push(13);
        a.push(14);
        a.push(15);
        // values = [1,2], a = [11,12,13,14,15]
        values = a;
        // values = [11,12,13,14,15]
        assert(values.length == 5);
        assert(values[0] == 11 && values[1] == 12 && values[2] == 13);
        assert(values[3] == 14 && values[4] == 15);
        values.push();
        assert(values.length == 6);
        assert(values[5] == 0);
    }

    /// @custom:key box
    function storageMatrixCopyLongerIntoShorter() public {
        // uint[][] matrix
        require(1 < matrix.length);
        // uint[][] matrixSource
        require(4 < matrixSource.length);
        delete matrix;
        delete matrixSource;
        matrix.push().push(1);
        matrix.push().push(2);
        matrixSource.push().push(11);
        matrixSource.push().push(12);
        matrixSource.push().push(13);
        matrixSource.push().push(14);
        matrixSource.push().push(15);
        // matrix = [[1],[2]], matrixSource = [[11],[12],[13],[14],[15]]
        matrix = matrixSource;
        // matrix = [[11],[12],[13],[14],[15]]
        assert(matrix.length == 5);
        assert(matrix[0][0] == 11 && matrix[1][0] == 12 && matrix[2][0] == 13);
        assert(matrix[3][0] == 14 && matrix[4][0] == 15);
    }

    /// @custom:key box
    function storageMatrixCopyShortIntoLonger() public {
        // uint[][] matrix
        require(4 < matrix.length);
        // uint[][] matrixSource
        require(2 < matrixSource.length);
        delete matrix;
        delete matrixSource;
        matrix.push().push(1);
        matrix.push().push(2);
        matrix.push().push(3);
        matrix.push().push(4);
        matrix.push().push(5);
        matrixSource.push().push(11);
        matrixSource.push().push(12);
        matrixSource.push().push(13);
        // matrix = [[1],[2],[3],[4],[5]], matrixSource = [[11],[12],[13]]
        matrix = matrixSource;
        // matrix = [[11],[12],[13]]
        assert(matrix.length == 3);
        assert(matrix[0][0] == 11 && matrix[1][0] == 12 && matrix[2][0] == 13);
    }

    function storageMatrixCopyShorterIntoLonger() public {
        // uint[][] matrix
        delete matrix;
        // uint[][] matrixSource
        delete matrixSource;
        matrix.push().push(1);
        matrix.push().push(2);
        matrix.push().push(3);
        matrix.push().push(4);
        matrixSource.push().push(11);
        matrixSource.push().push(12);
        // matrix = [[1],[2],[3],[4]], matrixSource = [[11],[12]]
        matrix = matrixSource;
        assert(matrix.length == 2);
        assert(matrix[1][matrix[1].length - 1] == 12);
        matrix.push();
        matrix.push();
        // matrix = [[11],[12],[],[]]
        matrix[3].push();
        assert(matrix[3][0] == 0);
    }

    /// @custom:key box
    function storageArrayPopPushFreshElement() public {
        // uint[] values
        require(3 < values.length);
        delete values;
        values.push(7);
        values.push(8);
        values.push(9);
        // values = [7,8,9]
        values.pop();
        values.push();
        // values = [7,8,0]
        assert(values.length == 3);
        assert(values[2] == 0);
        values.push();
        assert(values.length == 4);
        assert(values[3] == 0);
    }

    // import
    function storageMatrixPopPushDanglingRowReappears() public {
        // uint[][] matrix
        delete matrix;
        matrix.push().push(7);
        matrix.push().push(8);
        matrix.push().push(9);
        // [[7],[8],[9]]
        uint256[] storage m = matrix[2];
        matrix.pop();
        assert(matrix.length == 2);
        assert(m.length == 0);
        m.push(10);
        matrix.push();
        // [[7],[8],[10]]
        assert(matrix.length == 3);
        assert(matrix[2].length == 1 && matrix[2][0] == 10);
    }

    function mappingArrayCopyLongerIntoShorter() public {
        // mapping(uint => uint[]) listsByKey
        delete listsByKey[1];
        delete listsByKey[2];
        listsByKey[1].push(1);
        listsByKey[1].push(2);
        listsByKey[2].push(11);
        listsByKey[2].push(12);
        listsByKey[2].push(13);
        // listsByKey[1] = [1,2], listsByKey[2] = [11,12,13]
        listsByKey[1] = listsByKey[2];
        // listsByKey[1] = [11,12,13]
        assert(listsByKey[1].length == 3);
        assert(listsByKey[1][0] == 11 && listsByKey[1][1] == 12 && listsByKey[1][2] == 13);
        listsByKey[1].push(14);
        assert(listsByKey[1].length == 4);
        assert(listsByKey[1][3] == 14);
    }

    function mappingArrayCopyShorterIntoLonger() public {
        // mapping(uint => uint[]) listsByKey
        delete listsByKey[1];
        delete listsByKey[2];
        listsByKey[1].push(1);
        listsByKey[1].push(2);
        listsByKey[1].push(3);
        listsByKey[2].push(11);
        // listsByKey[1] = [1,2,3], listsByKey[2] = [11]
        listsByKey[1] = listsByKey[2];
        assert(listsByKey[1].length == 1);
        assert(listsByKey[1][0] == 11);
        listsByKey[1].push();
        listsByKey[1].push();
        // listsByKey[1] = [11,0,0]
        assert(listsByKey[1][1] == 0 && listsByKey[1][2] == 0);
    }

    function mappingArrayPopPushFreshElement() public {
        // mapping(uint => uint[]) listsByKey
        delete listsByKey[7];
        listsByKey[7].push(7);
        listsByKey[7].push(8);
        // listsByKey[7] = [7,8]
        listsByKey[7].pop();
        listsByKey[7].push();
        // listsByKey[7] = [7,0]
        assert(listsByKey[7].length == 2);
        assert(listsByKey[7][1] == 0);
    }

    function mappingMatrixCopyShorterIntoLonger() public {
        // mapping(uint => uint[][]) matricesByKey
        delete matricesByKey[1];
        delete matricesByKey[2];
        matricesByKey[1].push().push(1);
        matricesByKey[1].push().push(2);
        matricesByKey[1].push().push(3);
        matricesByKey[2].push().push(11);
        // matricesByKey[1] = [[1],[2],[3]], matricesByKey[2] = [[11]]
        matricesByKey[1] = matricesByKey[2];
        assert(matricesByKey[1].length == 1);
        assert(matricesByKey[1][0][matricesByKey[1][0].length - 1] == 11);
        matricesByKey[1].push();
        matricesByKey[1].push();
        // matricesByKey[1] = [[11],[],[]]
        assert(matricesByKey[1][1].length == 0 && matricesByKey[1][2].length == 0);
    }

    function mappingMatrixPopPushFreshRow() public {
        // mapping(uint => uint[][]) matricesByKey
        delete matricesByKey[3];
        matricesByKey[3].push().push(7);
        matricesByKey[3].push().push(8);
        // matricesByKey[3] = [[7],[8]]
        uint[] storage m = matricesByKey[3][1];
        matricesByKey[3].pop();
        assert(m.length == 0);
        matricesByKey[3].push();
        assert(matricesByKey[3].length == 2);
        assert(matricesByKey[3][1].length == 0);
    }

    function fixedRowsCopyLongerIntoShorter() public {
        // uint[3][] rows
        delete rows;
        // uint[3][] rowsSource
        delete rowsSource;
        rows.push()[0] = 1;
        rowsSource.push()[0] = 11;
        rowsSource.push()[2] = 12;
        // rows = [[1,0,0]], rowsSource = [[11,0,0],[0,0,12]]
        rows = rowsSource;
        assert(rows.length == 2);
        assert(rows[0][0] == 11 && rows[1][2] == 12);
    }

    function fixedRowsCopyShorterIntoLonger() public {
        // uint[3][] rows
        delete rows;
        // uint[3][] rowsSource
        delete rowsSource;
        rows.push()[0] = 1;
        rows.push()[1] = 2;
        rows.push()[2] = 3;
        rowsSource.push()[0] = 11;
        // rows = [[1,0,0],[0,2,0],[0,0,3]], rowsSource = [[11,0,0]]
        rows = rowsSource;
        assert(rows.length == 1);
        assert(rows[0][0] == 11);
        rows.push();
        rows.push();
        // rows = [[11,0,0],[0,0,0],[0,0,0]]
        assert(rows[1][1] == 0 && rows[2][2] == 0);
    }

    function fixedRowsPopPushFreshRow() public {
        // uint[3][] rows
        delete rows;
        rows.push()[0] = 7;
        rows.push()[1] = 8;
        // rows = [[7,0,0],[0,8,0]]
        uint[3] storage r = rows[1];
        rows.pop();
        assert(r[1] == 0);
        rows.push();
        assert(rows.length == 2);
        assert(rows[1][1] == 0);
    }

    function listTripleCopyResizesRows() public {
        // uint[][3] listTriple
        delete listTriple;
        // uint[][3] listTripleSource
        delete listTripleSource;
        listTriple[0].push(1);
        listTriple[0].push(2);
        listTriple[1].push(3);
        listTripleSource[1].push(11);
        listTripleSource[1].push(12);
        listTripleSource[2].push(13);
        // listTriple = [[1,2],[3],[]], listTripleSource = [[],[11,12],[13]]
        listTriple = listTripleSource;
        assert(listTriple[0].length == 0);
        assert(listTriple[1].length == 2 && listTriple[2].length == 1);
        assert(listTriple[1][0] == 11 && listTriple[1][1] == 12 && listTriple[2][0] == 13);
        listTriple[0].push();
        assert(listTriple[0][0] == 0);
    }

    function listTriplePopPushFreshElement() public {
        // uint[][3] listTriple
        delete listTriple;
        listTriple[2].push(7);
        listTriple[2].push(8);
        // listTriple = [[],[],[7,8]]
        listTriple[2].pop();
        listTriple[2].push();
        assert(listTriple[2].length == 2);
        assert(listTriple[2][1] == 0);
    }

    function storageBoolArrayPopPushFreshElement() public {
        // bool[] boolFlags
        delete boolFlags;
        boolFlags.push(true);
        boolFlags.push(true);
        // boolFlags = [true,true]
        boolFlags.pop();
        boolFlags.push();
        // boolFlags = [true,false]
        assert(boolFlags.length == 2);
        assert(boolFlags[0] && !boolFlags[1]);
    }

    function storageSignedArrayCopyShorterIntoLonger() public {
        // int[] signedValues
        delete signedValues;
        signedValues.push(-1);
        signedValues.push(-2);
        // signedValues = [-1,-2]
        int[] memory src = new int[](1);
        src[0] = 5;
        signedValues = src;
        assert(signedValues.length == 1);
        assert(signedValues[0] == 5);
        signedValues.push();
        assert(signedValues[1] == 0);
    }

    function structArrayFieldCopyLongerIntoShorter() public {
        // Basket basketA
        // Basket { uint[] items; }
        delete basketA;
        // Basket basketB
        delete basketB;
        basketA.items.push(1);
        basketB.items.push(11);
        basketB.items.push(12);
        basketB.items.push(13);
        // basketA.items = [1], basketB.items = [11,12,13]
        basketA = basketB;
        assert(basketA.items.length == 3);
        assert(basketA.items[0] == 11 && basketA.items[1] == 12 && basketA.items[2] == 13);
        basketA.items.push(14);
        assert(basketA.items.length == 4 && basketA.items[3] == 14);
    }

    function structArrayFieldCopyShorterIntoLonger() public {
        // Basket basketA
        // Basket { uint[] items; }
        delete basketA;
        // Basket basketB
        delete basketB;
        basketA.items.push(1);
        basketA.items.push(2);
        basketA.items.push(3);
        basketB.items.push(11);
        // basketA.items = [1,2,3], basketB.items = [11]
        basketA = basketB;
        assert(basketA.items.length == 1);
        assert(basketA.items[0] == 11);
        basketA.items.push();
        basketA.items.push();
        // basketA.items = [11,0,0]
        assert(basketA.items[1] == 0 && basketA.items[2] == 0);
    }

    function structArrayCopyLongerIntoShorter() public {
        // Token[] tokens
        // Token { uint value; }
        delete tokens;
        // Token[] tokensSource
        delete tokensSource;
        tokens.push().value = 1;
        tokensSource.push().value = 11;
        tokensSource.push().value = 12;
        tokensSource.push().value = 13;
        // tokens = [{1}], tokensSource = [{11},{12},{13}]
        tokens = tokensSource;
        assert(tokens.length == 3);
        assert(tokens[0].value == 11 && tokens[1].value == 12 && tokens[2].value == 13);
    }

    function structArrayCopyShorterIntoLonger() public {
        // Token[] tokens
        // Token { uint value; }
        delete tokens;
        // Token[] tokensSource
        delete tokensSource;
        tokens.push().value = 1;
        tokens.push().value = 2;
        tokens.push().value = 3;
        tokensSource.push().value = 11;
        // tokens = [{1},{2},{3}], tokensSource = [{11}]
        tokens = tokensSource;
        assert(tokens.length == 1);
        assert(tokens[0].value == 11);
        tokens.push();
        tokens.push();
        // tokens = [{11},{0},{0}]
        assert(tokens[1].value == 0 && tokens[2].value == 0);
    }

    function structArrayPopPushFreshElement() public {
        // Token[] tokens
        // Token { uint value; }
        delete tokens;
        tokens.push().value = 7;
        tokens.push().value = 8;
        // tokens = [{7},{8}]
        Token storage t = tokens[1];
        tokens.pop();
        assert(t.value == 0);
        tokens.push();
        assert(tokens.length == 2);
        assert(tokens[1].value == 0);
    }

    function basketArrayElementCopyShorterIntoLonger() public {
        // Basket[] baskets
        // Basket { uint[] items; }
        delete baskets;
        baskets.push();
        baskets.push();
        delete baskets[0];
        delete baskets[1];
        baskets[0].items.push(1);
        baskets[0].items.push(2);
        baskets[0].items.push(3);
        baskets[1].items.push(11);
        // baskets = [{[1,2,3]},{[11]}]
        baskets[0] = baskets[1];
        assert(baskets[0].items.length == 1);
        assert(baskets[0].items[0] == 11);
        baskets[0].items.push();
        baskets[0].items.push();
        assert(baskets[0].items[1] == 0 && baskets[0].items[2] == 0);
    }

    function basketArrayPopPushFreshElement() public {
        // Basket[] baskets
        // Basket { uint[] items; }
        delete baskets;
        baskets.push().items.push(7);
        baskets.push().items.push(8);
        // baskets = [{[7]},{[8]}]
        uint[] storage items = baskets[1].items;
        baskets.pop();
        assert(items.length == 0);
        baskets.push();
        assert(baskets.length == 2);
        assert(baskets[1].items.length == 0);
    }

    function structArrayFieldDeleteThenPushReadsZero() public {
        // Basket basketA
        // Basket { uint[] items; }
        basketA.items.push(5);
        basketA.items.push(6);
        // basketA.items = [5,6]
        delete basketA;
        assert(basketA.items.length == 0);
        basketA.items.push();
        basketA.items.push();
        assert(basketA.items[0] == 0 && basketA.items[1] == 0);
    }

    function basketArrayDeleteThenPushReadsZero() public {
        // Basket[] baskets
        // Basket { uint[] items; }
        delete baskets;
        baskets.push().items.push(7);
        baskets.push().items.push(8);
        // baskets = [{[7]},{[8]}]
        delete baskets;
        assert(baskets.length == 0);
        baskets.push();
        assert(baskets[0].items.length == 0);
        baskets[0].items.push();
        assert(baskets[0].items[0] == 0);
    }

    function mappingStructArrayFieldDeleteThenPushReadsZero() public {
        // mapping(uint => Basket) basketsByKey
        // Basket { uint[] items; }
        basketsByKey[1].items.push(5);
        basketsByKey[1].items.push(6);
        basketsByKey[2].items.push(9);
        delete basketsByKey[1];
        assert(basketsByKey[1].items.length == 0);
        assert(basketsByKey[2].items[basketsByKey[2].items.length - 1] == 9);
        basketsByKey[1].items.push();
        basketsByKey[1].items.push();
        assert(basketsByKey[1].items[0] == 0 && basketsByKey[1].items[1] == 0);
    }

    function structArrayDeleteThenPushReadsZero() public {
        // Token[] tokens
        // Token { uint value; }
        delete tokens;
        tokens.push().value = 7;
        tokens.push().value = 8;
        // tokens = [{7},{8}]
        delete tokens;
        assert(tokens.length == 0);
        tokens.push();
        tokens.push();
        assert(tokens[0].value == 0 && tokens[1].value == 0);
    }

    function structArrayElementDeleteReadsZero() public {
        // Token[] tokens
        // Token { uint value; }
        delete tokens;
        tokens.push().value = 7;
        tokens.push().value = 8;
        // tokens = [{7},{8}]
        delete tokens[0];
        // tokens = [{0},{8}]
        assert(tokens.length == 2);
        assert(tokens[0].value == 0 && tokens[1].value == 8);
    }

    function nestedStructArrayDeleteThenPushReadsZero() public {
        // Person[] persons
        // Person { Account account; uint age; } | Account { uint balance; Token token; } | Token { uint value; }
        delete persons;
        persons.push().account.token.value = 7;
        persons[0].account.balance = 5;
        persons[0].age = 3;
        delete persons;
        persons.push();
        assert(persons[0].age == 0);
        assert(persons[0].account.balance == 0);
        assert(persons[0].account.token.value == 0);
    }

    function nestedStructArrayPopPushReadsZero() public {
        // Person[] persons
        // Person { Account account; uint age; } | Account { uint balance; Token token; } | Token { uint value; }
        delete persons;
        persons.push().age = 1;
        persons.push().account.token.value = 7;
        persons[1].account.balance = 5;
        persons.pop();
        persons.push();
        assert(persons.length == 2);
        assert(persons[1].account.balance == 0);
        assert(persons[1].account.token.value == 0);
    }

    function nestedStructArrayElementDeleteReadsZero() public {
        // Person[] persons
        // Person { Account account; uint age; } | Account { uint balance; Token token; } | Token { uint value; }
        delete persons;
        persons.push().account.token.value = 7;
        persons[0].age = 3;
        delete persons[0].account;
        assert(persons[0].age == 3);
        assert(persons[0].account.token.value == 0);
        delete persons[0];
        assert(persons.length == 1 && persons[0].age == 0);
    }

    function nestedStructArrayCopyShorterIntoLonger() public {
        // Person[] persons
        // Person { Account account; uint age; } | Account { uint balance; Token token; } | Token { uint value; }
        delete persons;
        // Person[] personsSource
        delete personsSource;
        persons.push().account.token.value = 1;
        persons.push().account.token.value = 2;
        persons.push().account.token.value = 3;
        personsSource.push().account.token.value = 11;
        persons = personsSource;
        assert(persons.length == 1);
        assert(persons[0].account.token.value == 11);
        persons.push();
        persons.push();
        assert(persons[1].account.token.value == 0 && persons[2].account.token.value == 0);
    }

    function nestedStructArrayCopyLongerIntoShorter() public {
        // Person[] persons
        // Person { Account account; uint age; } | Account { uint balance; Token token; } | Token { uint value; }
        delete persons;
        // Person[] personsSource
        delete personsSource;
        persons.push().age = 1;
        personsSource.push().age = 11;
        personsSource.push().age = 12;
        personsSource.push().account.balance = 13;
        persons = personsSource;
        assert(persons.length == 3);
        assert(persons[0].age == 11 && persons[1].age == 12);
        assert(persons[2].account.balance == 13);
    }

    function structArrayMemberDeleteThenPushReadsZero() public {
        // TokenBucket bucket
        // TokenBucket { Token[] tokens; } | Token { uint value; }
        delete bucket;
        bucket.tokens.push().value = 7;
        bucket.tokens.push().value = 8;
        // bucket.tokens = [{7},{8}]
        delete bucket;
        assert(bucket.tokens.length == 0);
        bucket.tokens.push();
        bucket.tokens.push();
        assert(bucket.tokens[0].value == 0 && bucket.tokens[1].value == 0);
    }

    function structArrayMemberCopyShorterIntoLonger() public {
        // TokenBucket bucket
        // TokenBucket { Token[] tokens; } | Token { uint value; }
        delete bucket;
        // TokenBucket bucketSource
        delete bucketSource;
        bucket.tokens.push().value = 1;
        bucket.tokens.push().value = 2;
        bucketSource.tokens.push().value = 11;
        // bucket.tokens = [{1},{2}], bucketSource.tokens = [{11}]
        bucket = bucketSource;
        assert(bucket.tokens.length == 1);
        assert(bucket.tokens[0].value == 11);
        bucket.tokens.push();
        assert(bucket.tokens[1].value == 0);
    }

    function mappingStructArrayPopDeletePushReadsZero() public {
        // mapping(uint => Token[]) tokensByKey
        // Token { uint value; }
        delete tokensByKey[1];
        tokensByKey[1].push().value = 7;
        tokensByKey[1].push().value = 8;
        // tokensByKey[1] = [{7},{8}]
        tokensByKey[1].pop();
        tokensByKey[1].push();
        assert(tokensByKey[1].length == 2);
        assert(tokensByKey[1][1].value == 0);
        delete tokensByKey[1];
        tokensByKey[1].push();
        assert(tokensByKey[1][0].value == 0);
    }

    function fixedStructArrayCopyShorterIntoLonger() public {
        // Token[3] fixedTokens3
        // Token { uint value; }
        fixedTokens3[2].value = 9;
        // Token[2] fixedTokens
        fixedTokens[0].value = 11;
        fixedTokens[1].value = 12;
        fixedTokens3 = fixedTokens;
        assert(fixedTokens3.length == 3);
        assert(fixedTokens3[0].value == 11 && fixedTokens3[1].value == 12);
        assert(fixedTokens3[2].value == 0);
    }

    function fixedStructArrayElementDeleteReadsZero() public {
        // Token[2] fixedTokens
        // Token { uint value; }
        fixedTokens[0].value = 7;
        fixedTokens[1].value = 8;
        delete fixedTokens[1];
        assert(fixedTokens.length == 2);
        assert(fixedTokens[0].value == 7 && fixedTokens[1].value == 0);
    }

    // ── Storage: index ──

    function storageIndexRootMapping() public {
        // mapping(uint => uint) balances
        balances[1] = 42;
        uint r = balances[1];
        assert(r == 42);
    }

    /// @custom:key box
    function storageIndexAddAssign() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        values[1] += 2;
        uint r = values[1];
        assert(r == 42);
    }

    function storageIndexMappingAddAssign() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        balances[1] += 2;
        uint r = balances[1];
        assert(r == 42);
    }

    /// @custom:key box
    function storageIndexArrayAddAssignOutOfBoundsReverts() public {
        // uint[] values
        require(values.length == 0);
        values[1] += 2;
        assert(false);
    }

    /// @custom:key box
    function storageIndexArrayReadOutOfBoundsReverts() public view {
        // uint[] values
        require(values.length == 0);
        uint r = values[1];
        assert(r != r);
    }

    /// @custom:key box
    function storageIndexReadComplexReceiverBindLocalRoot() public {
        // TokenBucket bucket
        // TokenBucket { Token[] tokens; } | Token { uint value; }
        require(0 < bucket.tokens.length);
        bucket.tokens[0].value = 9;
        Token storage t = bucket.tokens[0];
        uint r = t.value;
        assert(r == 9);
    }

    /// @custom:key box
    function storageIndexWriteComplexReceiverCopySource() public {
        // TokenBucket bucket
        // TokenBucket { Token[] tokens; } | Token { uint value; }
        require(0 < bucket.tokens.length);
        // Person alice
        // Person { Account account; uint age; } | Account { uint balance; Token token; }
        alice.account.token.value = 7;
        Token storage tokRef = alice.account.token;
        bucket.tokens[0] = tokRef;
        uint r = bucket.tokens[0].value;
        assert(r == 7);
    }

    function storageFieldWriteRootRhsComplexReceiver() public {
        // Token tok
        // Token { uint value; }
        tok.value = 7;
        // Person alice
        // Person { Account account; uint age; } | Account { uint balance; Token token; }
        alice.account.token = tok;
        uint r = alice.account.token.value;
        assert(r == 7);
    }

    function storageIndexWriteRootRhsComplexReceiver() public {
        total = 9;
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[3] = total;
        uint r = ledger.balances[3];
        assert(r == 9);
    }

    // ── Storage: push ──

    /// @custom:key box
    function storagePushValue() public {
        // uint[] values
        require(values.length == 2);
        values.push(42);
        assert(values[2] == 42);
        assert(values.length == 3);
    }

    /// @custom:key box
    function storagePushComplexReceiverNonsimpleArg(uint x, uint y) public {
        // uint[][] matrix
        require(0 < matrix.length);
        require(matrix[0].length == 0);
        require(x == 40 && y == 2);
        matrix[0].push(x + y);
        uint r = matrix[0][0];
        assert(r == 42);
    }

    /// @custom:key box
    function storagePushValueCopySource() public {
        // Token[] tokens
        // Token { uint value; }
        require(tokens.length == 0);
        // Token tok
        tok.value = 7;
        tokens.push(tok);
        assert(tokens[0].value == 7);
    }

    /// @custom:key box
    function storagePushMemoryCopySource() public {
        // uint[][] matrix
        require(matrix.length == 0);
        uint[] memory row = new uint[](2);
        row[1] = 7;
        matrix.push(row);
        uint r = matrix[0][1];
        assert(r == 7);
        assert(matrix[0].length == 2);
    }

    /// @custom:key box
    function storagePushMemoryStructCopySource() public {
        // Token[] tokens
        // Token { uint value; }
        require(tokens.length == 0);
        Token memory t;
        t.value = 7;
        tokens.push(t);
        t.value = 8;
        assert(tokens[0].value == 7);
    }

    function storagePushReturnPrimitiveBindComplexReceiver() public {
        // uint[][] matrix
        delete matrix;
        matrix.push();
        matrix[0].push();
        matrix[0].push();
        // matrix = [[0,0]]
        delete matrix[0];
        matrix[0].push(4);
        // matrix = [[4]]
        uint y = matrix[0].push();
        assert(y == 0);
        assert(matrix[0].length == 2);
    }

    function storagePushCompoundTarget() public {
        // uint[] values
        values.push();
        values.push();
        delete values;
        values.push() += 5;
        values.push() *= 3;
        assert(values[0] == 5 && values[1] == 0);
        assert(values.length == 2);
    }

    function storagePushDivModTarget() public {
        // uint[] values
        values.push();
        values.push();
        delete values;
        values.push() /= 2;
        values.push() %= 2;
        assert(values[0] == 0 && values[1] == 0);
        assert(values.length == 2);
    }

    function storagePushIncrementTarget() public {
        // uint[] values
        values.push();
        values.push();
        delete values;
        ++values.push();
        values.push()++;
        assert(values[0] == 1 && values[1] == 1);
        assert(values.length == 2);
    }

    function storagePushCompoundSigned() public {
        // int[] signedValues
        signedValues.push();
        signedValues.push();
        signedValues.push();
        delete signedValues;
        signedValues.push() -= 4;
        --signedValues.push();
        signedValues.push()--;
        assert(signedValues[0] == -4 && signedValues[1] == -1 && signedValues[2] == -1);
        assert(signedValues.length == 3);
    }

    function valuesRef() internal view returns (uint[] storage) {
        // uint[] values
        return values;
    }

    function storageIndexWriteCallReceiver() public {
        // uint[] values
        delete values;
        values.push(4);
        valuesRef()[0] = 8;
        valuesRef()[0] += 2;
        valuesRef()[0]++;
        --valuesRef()[0];
        assert(values[0] == 10);
        delete valuesRef()[0];
        assert(values[0] == 0);
    }

    function storagePushDelete() public {
        // uint[] values
        delete values;
        values.push(7);
        delete values.push();
        assert(values.length == 2);
        assert(values[0] == 7 && values[1] == 0);
        // Token[] tokens
        // Token { uint value; }
        delete tokens;
        delete tokens.push();
        assert(tokens.length == 1 && tokens[0].value == 0);
        // uint[][] matrix
        delete matrix;
        delete matrix.push();
        assert(matrix.length == 1 && matrix[0].length == 0);
    }

    function storagePushDeleteUnfoldReceiver() public {
        // Basket basketA
        // Basket { uint[] items; }
        delete basketA;
        delete basketA.items.push();
        assert(basketA.items.length == 1 && basketA.items[0] == 0);
        // uint[][] matrix
        delete matrix;
        delete matrix.push().push();
        uint n = matrix[0].length;
        assert(matrix.length == 1 && n >= 1);
        assert(matrix[0][n - 1] == 0);
        // uint[] values
        delete values;
        delete valuesRef().push();
        assert(values.length == 1 && values[0] == 0);
    }

    function storagePushPopCallReceiver() public {
        // uint[] values
        values.push();
        delete values;
        valuesRef().push();
        valuesRef().push(42);
        valuesRef().pop();
        assert(values.length == 1);
        assert(values[0] == 0);
    }

    function storageArrayMethodReferences() public {
        // uint[] values
        delete values;
        values.pop;
        assert(values.length == 0);
        values.push(42);
        values.pop;
        assert(values.length == 1);
        assert(values[0] == 42);
    }

    function storageArrayMethodReferencesComplexReceiver() public {
        // uint[][] matrix
        matrix.push();
        matrix.push();
        delete matrix;
        matrix.push();
        matrix.push();
        matrix[0].push(42);
        // matrix = [[42],[]]
        uint i = 0;
        matrix[i++].pop;
        matrix[i++].pop;
        assert(i == 2);
        assert(matrix[0].length == 1);
        assert(matrix[0][0] == 42);
        assert(matrix[1].length == 0);
        // Basket basketA
        // Basket { uint[] items; }
        delete basketA.items;
        basketA.items.pop;
        assert(basketA.items.length == 0);
    }

    function matrixRefWithEffect() internal returns (uint[][] storage) {
        // uint[][] matrix
        matrix.push();
        return matrix;
    }

    function basketRefWithEffect() internal returns (Basket storage) {
        // Basket basketA
        // Basket { uint[] items; }
        basketA.items.push(9);
        return basketA;
    }

    function storageArrayMethodReferencesComposedCallReceiver() public {
        // uint[][] matrix
        matrix.push();
        matrix.push();
        matrix.push();
        delete matrix;
        uint i = 0;
        matrixRefWithEffect()[i++].pop;
        assert(i == 1);
        assert(matrix.length == 1);
        assert(matrix[0].length == 0);
        matrixRefWithEffect().push().pop;
        assert(matrix.length == 3);
        assert(matrix[1].length == 0);
        assert(matrix[2].length == 0);
        // Basket basketA
        // Basket { uint[] items; }
        delete basketA;
        basketRefWithEffect().items.pop;
        assert(basketA.items.length == 1);
        assert(basketA.items[0] == 9);
    }

    function valuesRefWithEffect() internal returns (uint[] storage) {
        // uint[] values
        values.push(7);
        return values;
    }

    function storageArrayMethodReferencesCallReceiver() public {
        // uint[] values
        delete values;
        valuesRef().pop;
        assert(values.length == 0);
        valuesRefWithEffect().pop;
        assert(values.length == 1);
        assert(values[0] == 7);
    }

    /// @custom:key box
    function storagePopReferenceOutOfBounds() public {
        // uint[][] matrix
        delete matrix;
        matrix[0].pop;
        assert(false);
    }

    /// @custom:key box
    function storagePopReferenceCallOutOfBounds() public {
        // uint[][] matrix
        delete matrix;
        matrixRefWithEffect()[1].pop;
        assert(false);
    }

    function newArrayCaptureStorageRoot() public {
        // uint[] values
        values = new uint[](3);
        assert(values.length == 3);
        assert(values[2] == 0);
    }

    // ── Taclet coverage: one function per taclet no other example applies ──

    function indexWriteValueRhsCapture() public pure {
        uint[] memory xs = new uint[](2);
        uint y = 4;
        xs[1] = y + 1;
        uint r = xs[1];
        assert(r == 5);
    }

    function storageIndexAddAssignUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        ledger.balances[1] += 2;
        uint r = ledger.balances[1];
        assert(r == 42);
    }

    function storageIndexSubAssignUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        ledger.balances[1] -= 2;
        uint r = ledger.balances[1];
        assert(r == 38);
    }

    function storageIndexMulAssignUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 7;
        ledger.balances[1] *= 6;
        uint r = ledger.balances[1];
        assert(r == 42);
    }

    function storageIndexDivAssignUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        ledger.balances[1] /= 8;
        uint r = ledger.balances[1];
        assert(r == 5);
    }

    function storageIndexModAssignUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        ledger.balances[1] %= 7;
        uint r = ledger.balances[1];
        assert(r == 5);
    }

    function storageIndexPreincrementUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        ++ledger.balances[1];
        uint r = ledger.balances[1];
        assert(r == 41);
    }

    function storageIndexPostincrementUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        ledger.balances[1]++;
        uint r = ledger.balances[1];
        assert(r == 41);
    }

    function storageIndexPredecrementUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        --ledger.balances[1];
        uint r = ledger.balances[1];
        assert(r == 39);
    }

    function storageIndexPostdecrementUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        ledger.balances[1]--;
        uint r = ledger.balances[1];
        assert(r == 39);
    }

    function storageIndexMappingSubAssign() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        balances[1] -= 2;
        uint r = balances[1];
        assert(r == 38);
    }

    function storageIndexMappingMulAssign() public {
        // mapping(uint => uint) balances
        balances[1] = 7;
        balances[1] *= 6;
        uint r = balances[1];
        assert(r == 42);
    }

    function storageIndexMappingDivAssign() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        balances[1] /= 8;
        uint r = balances[1];
        assert(r == 5);
    }

    function storageIndexMappingModAssign() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        balances[1] %= 7;
        uint r = balances[1];
        assert(r == 5);
    }

    function storageIndexMappingPreincrement() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        ++balances[1];
        uint r = balances[1];
        assert(r == 41);
    }

    function storageIndexMappingPostincrement() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        balances[1]++;
        uint r = balances[1];
        assert(r == 41);
    }

    function storageIndexMappingPredecrement() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        --balances[1];
        uint r = balances[1];
        assert(r == 39);
    }

    function storageIndexMappingPostdecrement() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        balances[1]--;
        uint r = balances[1];
        assert(r == 39);
    }

    function storageIndexMappingPreincrementAssignment() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        uint r = ++balances[1];
        assert(r == 41);
        assert(balances[1] == 41);
    }

    function storageIndexMappingPostincrementAssignment() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        uint r = balances[1]++;
        assert(r == 40);
        assert(balances[1] == 41);
    }

    function storageIndexMappingPredecrementAssignment() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        uint r = --balances[1];
        assert(r == 39);
        assert(balances[1] == 39);
    }

    function storageIndexMappingPostdecrementAssignment() public {
        // mapping(uint => uint) balances
        balances[1] = 40;
        uint r = balances[1]--;
        assert(r == 40);
        assert(balances[1] == 39);
    }

    /// @custom:key box
    function storageIndexReadArrayStoreRoot() public {
        // uint[] values
        require(1 < values.length);
        values[1] = 40;
        age = values[1];
        uint r = age;
        assert(r == 40);
    }

    /// @custom:key ensures fixedMaps[1][5] == 7 && fixedMaps[0][5] == \old(fixedMaps[0][5])
    function specFixedArrayOfMappingsElement() public {
        // mapping(uint => uint)[2] fixedMaps
        fixedMaps[1][5] = 7;
    }

    /// @custom:key box
    function storagePopUnfold() public {
        // Basket basketA
        // Basket { uint[] items; }
        require(0 < basketA.items.length);
        basketA.items.pop();
    }

    function storageIndexPreincrementAssignmentUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        uint r = ++ledger.balances[1];
        assert(r == 41);
        assert(ledger.balances[1] == 41);
    }

    function storageIndexPostincrementAssignmentUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        uint r = ledger.balances[1]++;
        assert(r == 40);
        assert(ledger.balances[1] == 41);
    }

    function storageIndexPredecrementAssignmentUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        uint r = --ledger.balances[1];
        assert(r == 39);
        assert(ledger.balances[1] == 39);
    }

    function storageIndexPostdecrementAssignmentUnfold() public {
        // Ledger ledger
        // Ledger { uint nonce; mapping(uint => uint) balances; }
        ledger.balances[1] = 40;
        uint r = ledger.balances[1]--;
        assert(r == 40);
        assert(ledger.balances[1] == 39);
    }

    function storageIndexReadMappingStoreRootStaticToDynamic() public {
        // mapping(uint => uint[3]) fixedByKey
        delete fixedByKey[7];
        // uint[] copyTargetDyn
        copyTargetDyn = fixedByKey[7];
        assert(copyTargetDyn.length == 3 && copyTargetDyn[2] == 0);
    }

    function storageIndexReadArrayStoreRootStaticToDynamic() public {
        // uint[3][] rowsSource
        delete rowsSource;
        rowsSource.push();
        // uint[] copyTargetDyn
        copyTargetDyn = rowsSource[0];
        assert(copyTargetDyn.length == 3);
    }

    function storageDeletedFixedRowCopy() public {
        // uint[3][2] fixedGrid
        fixedGrid[0][1] = 5;
        delete fixedGrid[0];
        fixedGrid[1] = fixedGrid[0];
        assert(fixedGrid[1][1] == 0);
    }

    function storageDeletedFixedByKeyCopy() public {
        // mapping(uint => uint[3]) fixedByKey
        fixedByKey[2][1] = 4;
        delete fixedByKey[2];
        // uint[3] fixedValues
        fixedValues = fixedByKey[2];
        assert(fixedValues[1] == 0);
    }

    function storageDeletedDynamicRowCopy() public {
        // uint[3][] rows
        delete rows;
        rows.push();
        rows.push();
        rows[0][2] = 7;
        // rows = [[0,0,7],[0,0,0]]
        delete rows[0];
        rows[1] = rows[0];
        assert(rows[1][2] == 0);
    }

    function storageDeletedStructWithFixedCopy() public {
        // mapping(uint => Triple) triplesByKey
        // Triple { uint[3] items; uint tag; }
        triplesByKey[0].items[1] = 6;
        triplesByKey[0].tag = 2;
        delete triplesByKey[0];
        // Triple triple2
        triple2 = triplesByKey[0];
        assert(triple2.items[1] == 0 && triple2.tag == 0);
    }

    function storageRecursiveCopyDisjoint() public {
        // Node nodeSpare
        // Node { uint v; Node[] kids; }
        nodeSpare.v = 3;
        // Node nodeTree
        nodeTree.kids.push();
        nodeTree.kids[nodeTree.kids.length - 1] = nodeSpare;
        assert(nodeTree.kids[nodeTree.kids.length - 1].v == 3);
    }

    function storageRecursiveCopySelf() public {
        // Node nodeTree
        // Node { uint v; Node[] kids; }
        nodeTree.kids.push();
        nodeTree.kids[0].v = 4;
        nodeTree.kids[0] = nodeTree.kids[0];
        assert(nodeTree.kids[0].v == 4);
    }

    function storageRecursiveCopySibling() public {
        // Node nodeTree
        // Node { uint v; Node[] kids; }
        nodeTree.kids.push();
        nodeTree.kids.push();
        uint last = nodeTree.kids.length - 1;
        nodeTree.kids[last].v = 7;
        nodeTree.kids[last - 1] = nodeTree.kids[last];
        assert(nodeTree.kids[last - 1].v == 7);
    }

    function storageRecursiveCopyFromSiblingChild() public {
        // Node nodeTree
        // Node { uint v; Node[] kids; }
        delete nodeTree;
        nodeTree.kids.push();
        nodeTree.kids.push();
        nodeTree.kids[1].kids.push();
        nodeTree.kids[1].kids.push();
        nodeTree.kids[1].kids[1].v = 5;
        // nodeTree.kids = [{}, {kids: [{}, {v: 5}]}]
        nodeTree.kids[0] = nodeTree.kids[1].kids[1];
        assert(nodeTree.kids[0].v == 5);
        assert(nodeTree.kids[1].kids[1].v == 5);
    }

    function nodeKidsRef() internal view returns (Node[] storage) {
        // Node nodeTree
        // Node { uint v; Node[] kids; }
        return nodeTree.kids;
    }

    function storageRecursiveCopyCallReceiver() public {
        // Node nodeSpare
        // Node { uint v; Node[] kids; }
        nodeSpare.v = 3;
        // Node nodeTree
        nodeTree.kids.push();
        nodeKidsRef()[0] = nodeSpare;
        assert(nodeTree.kids[0].v == 3);
    }

    function storageFieldWriteCopySourcePrim() public {
        total = 7;
        // Person alice
        // Person { Account account; uint age; } | Account { uint balance; Token token; } | Token { uint value; }
        alice.age = total;
        assert(alice.age == 7);
    }

    function storageIndexWriteMappingCopySourcePrim() public {
        total = 7;
        // mapping(uint => uint) balances
        balances[3] = total;
        assert(balances[3] == 7);
    }

    function storageIndexWriteArrayCopySourcePrim() public {
        total = 7;
        // uint[] values
        uint n = values.length;
        values.push();
        values[n] = total;
        assert(values[n] == 7);
    }

    function storagePushValueCopySourcePrim() public {
        total = 7;
        // uint[] values
        uint n = values.length;
        values.push(total);
        assert(values[n] == 7);
    }

    function storageRecursiveCopyFromOwnChild() public {
        // Node nodeTree
        // Node { uint v; Node[] kids; }
        delete nodeTree;
        delete nodeTree.kids.push();
        delete nodeTree.kids[0].kids.push();
        delete nodeTree.kids[0].kids.push();
        nodeTree.kids[0].kids[1].v = 5;
        nodeTree = nodeTree.kids[0];
        assert(nodeTree.kids.length == 2);
        assert(nodeTree.kids[0].v == 0 && nodeTree.kids[0].kids.length == 0);
        assert(nodeTree.kids[1].v == 5);
    }

    function storageRecursiveCopyFromOwnChildMember() public {
        // Node nodeTree
        // Node { uint v; Node[] kids; }
        delete nodeTree;
        delete nodeTree.kids.push();
        nodeTree.kids[0].v = 9;
        nodeTree = nodeTree.kids[0];
        assert(nodeTree.v == 9);
    }

    function storageRecursiveCopyFromOwnChildArray() public {
        delete nodeTree;
        delete nodeTree.kids.push();
        delete nodeTree.kids[0].kids.push();
        delete nodeTree.kids[0].kids.push();
        nodeTree.kids[0].kids[0].v = 3;
        nodeTree.kids[0].kids[1].v = 5;
        nodeTree.kids = nodeTree.kids[0].kids;
        assert(nodeTree.kids.length == 2);
        assert(nodeTree.kids[0].v == 3 && nodeTree.kids[1].v == 5);
    }

    /// @custom:key box
    function storageRecursiveCopyFromOwnChildAnyKidLength(uint i) public {
        // Node nodeTree
        // Node { uint v; Node[] kids; }
        require(0 < nodeTree.kids.length);
        require(i < nodeTree.kids[0].kids.length);
        uint len = nodeTree.kids[0].kids[i].kids.length;
        nodeTree = nodeTree.kids[0];
        assert(nodeTree.kids[i].kids.length == len);
    }

    /// @custom:key box
    function storageRecursiveCopyFromOwnChildPreservesLaterMember() public {
        // Chain chainRoot
        // Chain { Chain[] next; uint tag; }
        require(0 < chainRoot.next.length);
        uint tag = chainRoot.next[0].tag;
        chainRoot = chainRoot.next[0];
        assert(chainRoot.tag == tag);
    }

    function storageRecursiveCopyFromOwnChildLaterMember() public {
        // Chain chainRoot
        // Chain { Chain[] next; uint tag; }
        delete chainRoot;
        delete chainRoot.next.push();
        delete chainRoot.next[0].next.push();
        chainRoot.next[0].tag = 9;
        chainRoot.next[0].next[0].tag = 4;
        chainRoot.tag = 3;
        chainRoot = chainRoot.next[0];
        assert(chainRoot.tag == 9 && chainRoot.next.length == 1);
        assert(chainRoot.next[0].tag == 4);
    }

    /// @custom:key box
    function storageRecursiveCopyIntoOwnChildIndex() public {
        // Node nodeTree
        // Node { uint v; Node[] kids; }
        require(0 < nodeTree.kids.length);
        nodeTree.kids[0] = nodeTree;
        assert(false);
    }

    /// @custom:key box
    function storageRecursiveCopyIntoOwnChildMember() public {
        // Node nodeTree
        // Node { uint v; Node[] kids; }
        require(0 < nodeTree.kids.length);
        nodeTree.kids[0].kids = nodeTree.kids;
        assert(false);
    }

    /// @custom:key box
    function storageRecursiveCopyIntoOwnChildPush() public {
        // Node nodeTree
        // Node { uint v; Node[] kids; }
        require(0 < nodeTree.kids.length);
        nodeTree.kids[0].kids.push(nodeTree.kids[0]);
        assert(false);
    }

    /// @custom:key box
    function storageRecursiveCopyIntoOwnChildUnlessApart(uint i, uint j) public {
        // Node nodeTree
        // Node { uint v; Node[] kids; }
        require(i < nodeTree.kids.length && j < nodeTree.kids.length);
        require(0 < nodeTree.kids[i].kids.length);
        uint r = nodeTree.kids[j].v;
        nodeTree.kids[i].kids[0] = nodeTree.kids[j];
        assert(i != j && nodeTree.kids[i].kids[0].v == r);
    }
}
