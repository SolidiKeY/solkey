// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Taclet examples: storage roots, struct fields, aliases, deletes and mappings.
// Conventions: README.md, "Writing an example".
contract Storage {
    struct Token { uint value; }
    struct Account { uint balance; Token token; }
    struct Person { Account account; uint age; }
    struct Basket { uint[] items; }
    struct Ledger { uint nonce; mapping(uint => uint) balances; }
    struct Toggle { bool on; uint n; }
    struct Tree { uint v; mapping(uint => Tree) kids; }

    uint total;
    uint age;
    uint balance;
    int signedTotal;
    bool flag;
    uint fixedLengthCopy;
    bool flag2;
    uint[9] copySource9;
    uint[40] copyBig;
    uint[20] copySmall;
    bool[] boolFlags;

    mapping(uint => uint) balances;
    mapping(uint => Person) people;
    mapping(uint => bool) flags;
    mapping(uint => uint) valuesMap;
    mapping(uint => Account) accountMap;

    Person[] persons;
    Person alice;
    Person bob;
    Toggle toggle;

    Ledger ledger;
    Token tok;
    Token[] tokens;
    Basket basketA;
    Basket basketB;

    mapping(uint => mapping(uint => uint)) grid;
    mapping(uint => Ledger) ledgerMap;
    mapping(uint => uint)[] mapArray;
    mapping(bool => uint) boolKeyed;
    Tree tree;

    function storageAssignmentValueNested() public {
        bool r = (flag = (flag2 = true));
        assert(r && flag && flag2);
        uint n = (total = 7);
        assert(n == 7 && total == 7);
        bool m = (toggle.on = false);
        assert(!m && !toggle.on);
    }

    function storageAliasRebindAlias() public {
        Person storage alicePath = alice;
        bob.age = 20;
        alicePath = bob;
        uint r = alicePath.age;
        assert(r == 20);
    }

    function storageDeepFieldPostincrement() public {
        alice.account.balance = 100;
        alice.account.balance++;
        uint r = alice.account.balance;
        assert(r == 101);
    }

    function storageDeepFieldPreincrement() public {
        alice.account.balance = 100;
        ++alice.account.balance;
        uint r = alice.account.balance;
        assert(r == 101);
    }

    function storageFieldAddAssign() public {
        alice.age = 30;
        alice.age += 4;
        uint r = alice.age;
        assert(r == 34);
    }

    function storageFieldCopyStruct() public {
        bob.account.balance = 11;
        Account storage acc = bob.account;
        alice.account = acc;
        uint r = alice.account.balance;
        assert(r == 11);
    }

    function storageFieldCopyValueField() public {
        bob.age = 30;
        alice.age = bob.age;
        uint r = alice.age;
        assert(r == 30);
    }

    function storageFieldDeepDivAssign() public {
        alice.account.balance = 36;
        alice.account.balance /= 4;
        uint r = alice.account.balance;
        assert(r == 9);
    }

    function storageFieldDeepModAssign() public {
        alice.account.balance = 37;
        alice.account.balance %= 4;
        uint r = alice.account.balance;
        assert(r == 1);
    }

    function storageFieldDeepMulAssign() public {
        alice.account.balance = 8;
        alice.account.balance *= 3;
        uint r = alice.account.balance;
        assert(r == 24);
    }

    function storageFieldDeepSubAssign() public {
        alice.account.balance = 30;
        alice.account.balance -= 4;
        uint r = alice.account.balance;
        assert(r == 26);
    }

    function storageFieldDeepWriteRead() public {
        alice.account.balance = 34;
        uint r = alice.account.balance;
        assert(r == 34);
    }

    function storageFieldDelete() public {
        alice.age = 30;
        delete alice.age;
        uint r = alice.age;
        assert(r == 0);
    }

    function storageFieldDeleteThenCopy() public {
        bob.account.balance = 10;
        delete bob.account;
        alice.account = bob.account;
        uint r = alice.account.balance;
        assert(r == 0);
    }

    function storageFieldDeleteThenCopyDeep() public {
        bob.account.token.value = 7;
        delete bob.account;
        alice.account = bob.account;
        uint r = alice.account.token.value;
        assert(r == 0);
    }

    function storageFieldDivAssign() public {
        alice.age = 30;
        alice.age /= 5;
        uint r = alice.age;
        assert(r == 6);
    }

    function storageFieldModAssign() public {
        alice.age = 30;
        alice.age %= 7;
        uint r = alice.age;
        assert(r == 2);
    }

    function storageFieldMulAssign() public {
        alice.age = 7;
        alice.age *= 4;
        uint r = alice.age;
        assert(r == 28);
    }

    function storageFieldPostdecrementAssign() public {
        alice.age = 30;
        uint r = alice.age--;
        assert(r == 30);
    }

    function storageFieldPostdecrement() public {
        alice.age = 30;
        alice.age--;
        uint r = alice.age;
        assert(r == 29);
    }

    function storageFieldPostincrementAssign() public {
        alice.age = 30;
        uint r = alice.age++;
        assert(r == 30);
    }

    function storageFieldPostincrement() public {
        alice.age = 30;
        alice.age++;
        uint r = alice.age;
        assert(r == 31);
    }

    function storageFieldPredecrementAssign() public {
        alice.age = 30;
        uint r = --alice.age;
        assert(r == 29);
    }

    function storageFieldPredecrement() public {
        alice.age = 30;
        --alice.age;
        uint r = alice.age;
        assert(r == 29);
    }

    function storageFieldPreincrementAssign() public {
        alice.age = 30;
        uint r = ++alice.age;
        assert(r == 31);
    }

    function storageFieldPreincrement() public {
        alice.age = 30;
        ++alice.age;
        uint r = alice.age;
        assert(r == 31);
    }

    function storageFieldReadBindLocal() public {
        Account storage acc = alice.account;
        bob.account.balance = 42;
        acc = bob.account;
        uint r = acc.balance;
        assert(r == 42);
    }

    function storageFieldReadStoreRoot() public {
        alice.age = 17;
        total = alice.age;
        uint r = total;
        assert(r == 17);
    }

    function storageFieldSubAssign() public {
        alice.age = 30;
        alice.age -= 5;
        uint r = alice.age;
        assert(r == 25);
    }

    function storageFieldWriteCaptureSrc() public {
        bob.account.balance = 11;
        alice.account = bob.account;
        uint r = alice.account.balance;
        assert(r == 11);
    }

    /// @custom:key box
    function storageFieldWriteRhsCapture(uint x, uint y, uint z) public {
        require(x == 1 && y == 2 && z == 3);
        alice.age = x + y*z;
        uint r = alice.age;
        assert(r == 7);
    }

    function storageLocalDeclSkip() public {
        Person storage p;
        p = alice;
        age = 7;
        uint r = age;
        assert(r == 7);
    }

    function storageRootAddAssign() public {
        age = 10;
        age += 5;
        uint r = age;
        assert(r == 15);
    }

    function storageRootCopySource() public {
        age = 34;
        balance = age;
        uint r = balance;
        assert(r == 34);
    }

    function storageRootCopyStruct() public {
        bob.age = 7;
        alice = bob;
        uint r = alice.age;
        assert(r == 7);
    }

    function storageRootDeleteStruct() public {
        alice.age = 30;
        delete alice;
        uint r = alice.age;
        assert(r == 0);
    }

    function storageRootDeleteThenCopy() public {
        bob.age = 30;
        delete bob;
        alice = bob;
        uint r = alice.age;
        assert(r == 0);
    }

    function storageRootDelete() public {
        age = 10;
        delete age;
        uint r = age;
        assert(r == 0);
    }

    function storageRootDisjoint() public {
        age = 7;
        balance = 9;
        uint r = age;
        assert(r == 7);
    }

    function storageRootDivAssign() public {
        age = 20;
        age /= 4;
        uint r = age;
        assert(r == 5);
    }

    function storageRootModAssign() public {
        age = 17;
        age %= 5;
        uint r = age;
        assert(r == 2);
    }

    function storageRootMulAssign() public {
        age = 6;
        age *= 3;
        uint r = age;
        assert(r == 18);
    }

    function storageRootMultipleWrites() public {
        age = 1;
        age = 2;
        uint r = age;
        assert(r == 2);
    }

    function storageRootPostdecrementAssign() public {
        age = 10;
        uint r = age--;
        assert(r == 10);
    }

    function storageRootPostincrementAssign() public {
        age = 10;
        uint r = age++;
        assert(r == 10);
    }

    function storageRootPostincrement() public {
        age = 10;
        age++;
        uint r = age;
        assert(r == 11);
    }

    function storageRootPredecrement() public {
        age = 10;
        --age;
        uint r = age;
        assert(r == 9);
    }

    function storageRootPreincrementAssign() public {
        age = 10;
        uint r = ++age;
        assert(r == 11);
    }

    function storageRootPreincrement() public {
        age = 10;
        ++age;
        uint r = age;
        assert(r == 11);
    }

    function storageRootSubAssign() public {
        age = 10;
        age -= 4;
        uint r = age;
        assert(r == 6);
    }

    /// @custom:key box
    function storageRootWriteRhsCapture(uint x, uint y, uint z) public {
        require(x == 1 && y == 2 && z == 3);
        total = x + y*z;
        uint r = total;
        assert(r == 7);
    }

    function testDeleteLocalValue() public pure {
        uint v = 5;
        bool b = true;
        delete v;
        delete b;
        assert(v == 0);
        assert(!b);
    }

    function testNestedStorageWrites() public {
        alice.account.balance = 10;
        alice.account.token.value = 5;
        uint b = alice.account.balance;
        uint v = alice.account.token.value;
        assert(b == 10);
        assert(v == 5);
    }

    function testStorageAliases() public {
        Person storage p = alice;
        Account storage acc = p.account;
        acc.balance = 100;
        p.account.token.value = 3;
        assert(alice.account.balance == 100);
        assert(alice.account.token.value == 3);
    }

    function testStorageDeletePaperCase() public {
        alice.account.balance = 100;
        alice.account.token.value = 7;
        delete alice.account;
        uint b = alice.account.balance;
        uint v = alice.account.token.value;
        assert(b == 0);
        assert(v == 0);
    }

    function testStorageFieldDeepCopy() public {
        bob.account.balance = 10;
        bob.account.token.value = 7;
        Account storage bobAcc = bob.account;
        alice.account = bobAcc;
        bob.account.balance = 20;
        bob.account.token.value = 9;
        assert(alice.account.balance == 10);
        assert(alice.account.token.value == 7);
    }

    function testStorageMapReadWriteAndDelete() public {
        valuesMap[1] = 10;
        valuesMap[2] = 20;
        uint v = valuesMap[1];
        uint kept = valuesMap[2];
        delete valuesMap[1];
        assert(v == 10);
        assert(kept == 20);
        assert(valuesMap[1] == 0);
        assert(valuesMap[2] == 20);
    }

    function testStorageMapStructCopy() public {
        accountMap[1].balance = 10;
        accountMap[1].token.value = 7;
        accountMap[2] = accountMap[1];
        accountMap[1].balance = 20;
        accountMap[1].token.value = 9;
        assert(accountMap[2].balance == 10);
        assert(accountMap[2].token.value == 7);
    }

    function testDeleteKeepsMappingElementEntries() public {
        delete mapArray;
        mapArray.push();
        mapArray[0][1] = 5;
        delete mapArray;
        mapArray.push();
        assert(mapArray[0][1] == 5);
    }

    function testStorageRootDeepCopy() public {
        bob.age = 21;
        bob.account.balance = 11;
        alice = bob;
        bob.age = 30;
        bob.account.balance = 12;
        assert(alice.age == 21);
        assert(alice.account.balance == 11);
    }

    // import
    function testStorageStructDeleteSkipsMappingMember() public {
        ledger.nonce = 5;
        ledger.balances[1] = 10;
        ledger.balances[2] = 20;
        delete ledger;
        assert(ledger.nonce == 0);
        assert(ledger.balances[1] == 10);
        assert(ledger.balances[2] == 20);
        delete ledger.balances[1];
        assert(ledger.balances[1] == 0);
        assert(ledger.balances[2] == 20);
    }

    function testStorageWriteAndRead() public {
        uint x = 20;
        uint y = 1;
        alice.age = x + y;
        uint v = alice.age;
        assert(v == 21);
    }

    function storageShorterStaticCopyClearsTail() public {
        copyBig[30] = 4;
        copySmall[3] = 8;
        copyBig = copySmall;
        uint head = copyBig[3];
        uint tail = copyBig[30];
        assert(head == 8);
        assert(tail == 0);
    }

    function testShiftRightNegativeRoundsDown() public pure {
        int x = -4267;
        int r = x >> 1;
        assert(r == -2134);
        x >>= 256;
        assert(x == -1);
    }

    function testShiftLeftFitsUint8() public pure {
        uint8 x = 3;
        x <<= 6;
        assert(x == 192);
    }

    function testShiftNonSimpleLeftOperand() public pure {
        uint x = 3;
        uint r;
        r = (x + 1) << 2;
        assert(r == 16);
        r = (x * 4) >> 1;
        assert(r == 6);
    }

    // ── Storage: root ──

    function storageRootReadWrite() public {
        age = 34;
        uint r = age;
        assert(r == 34);
    }

    // ── Storage: field ──

    function storageFieldWriteRead() public {
        alice.age = 34;
        uint r = alice.age;
        assert(r == 34);
    }

    function storageFieldDeepAddAssign() public {
        alice.account.balance = 30;
        alice.account.balance += 4;
        uint r = alice.account.balance;
        assert(r == 34);
    }

    function storageFieldGlobalAge() public {
        alice.age = 34;
        assert(alice.age == 34);
        uint r = alice.age;
        assert(r == 34);
    }

    // ── Storage: alias ──

    function storageAliasWrite() public {
        Person storage p = alice;
        Account storage acc = p.account;
        acc.balance = 100;
        p.account.token.value = 3;
        assert(alice.account.balance == 100);
        assert(alice.account.token.value == 3);
    }

    // ── Storage: bool ──

    function storageBoolRootReadWrite() public {
        flag = true;
        bool r = flag;
        assert(r);
    }

    function storageBoolRootCopy() public {
        flag = true;
        flag2 = flag;
        bool r = flag2;
        assert(r);
    }

    function storageBoolFieldRead() public {
        toggle.on = true;
        bool r = toggle.on;
        assert(r);
    }

    function storageBoolMappingRead() public {
        flags[2] = true;
        bool r = flags[2];
        assert(r);
    }

    /// @custom:key box
    function storageBoolArrayRead() public {
        require(boolFlags.length == 1);
        boolFlags[0] = true;
        bool r = boolFlags[0];
        assert(r);
    }

    function storageBoolFieldStoreRoot() public {
        toggle.on = true;
        flag = toggle.on;
        bool r = flag;
        assert(r);
    }

    function storageFixedLengthStoreRoot() public {
        fixedLengthCopy = copySource9.length;
        assert(fixedLengthCopy == 9);
    }

    // ── Storage copy: mapping members stay the target's own ──

    function testCopyRootKeepsValueMembers() public {
        bob.age = 7;
        bob.account.balance = 3;
        alice = bob;
        bob.age = 8;
        assert(alice.age == 7);
        assert(alice.account.balance == 3);
    }

    function testCopyFieldNested() public {
        Account storage acc = bob.account;
        acc.token.value = 5;
        alice.account = acc;
        acc.token.value = 6;
        assert(alice.account.token.value == 5);
    }

    /// @custom:key box
    function testCopyOfCopy() public {
        require(persons.length == 0);
        bob.age = 4;
        alice = bob;
        persons.push();
        persons[0] = alice;
        assert(persons[0].age == 4);
    }

    /// @custom:key box
    function testCopyIntoMappingEntry(uint k) public {
        require(k == 3);
        alice.age = 6;
        people[k] = alice;
        alice.age = 7;
        assert(people[k].age == 6);
    }

    /// @custom:key box
    function testCopyArrayMemberElements() public {
        require(basketA.items.length == 0);
        require(basketB.items.length == 0);
        basketA.items.push(9);
        basketB = basketA;
        basketA.items[0] = 1;
        assert(basketB.items.length == 1);
        assert(basketB.items[0] == 9);
    }

    function testCopyPrimitiveRoot() public {
        age = 3;
        balance = age;
        age = 4;
        assert(balance == 3);
    }

    function testCopyStoreRootFromField() public {
        alice.account.token.value = 2;
        tok = alice.account.token;
        alice.account.token.value = 1;
        assert(tok.value == 2);
    }

    function testCopyStoreRootFromIndex() public {
        people[1].age = 2;
        alice = people[1];
        people[1].age = 3;
        assert(alice.age == 2);
    }

    /// @custom:key box
    function testPushCopyThenDeleteTarget() public {
        require(tokens.length == 0);
        tok.value = 4;
        Token storage src = tok;
        tokens.push() = src;
        delete tokens[0];
        assert(tokens[0].value == 0);
        assert(tok.value == 4);
    }

    // ── Mapping indices: nesting, aliases and memory keys ──

    function nestedMappingWriteRead() public {
        grid[1][2] = 7;
        uint r = grid[1][2];
        assert(r == 7);
    }

    function nestedMappingRowAlias() public {
        mapping(uint => uint) storage row = grid[1];
        row[2] = 5;
        uint r = grid[1][2];
        assert(r == 5);
    }

    function arrayOfMappingsIndex() public {
        mapArray.push();
        mapArray[0][1] = 2;
        uint r = mapArray[0][1];
        assert(r == 2);
    }

    function mappingOfLedgerMember() public {
        ledgerMap[1].nonce = 4;
        ledgerMap[1].balances[2] = 3;
        uint n = ledgerMap[1].nonce;
        uint r = ledgerMap[1].balances[2];
        assert(n == 4);
        assert(r == 3);
    }

    function mappingPointerTernary() public {
        flag = true;
        mapping(uint => uint) storage ptr = flag ? balances : valuesMap;
        ptr[1] = 2;
        uint r = balances[1];
        assert(r == 2);
    }

    function mappingReadAsKey() public {
        balances[1] = 3;
        balances[3] = 9;
        uint r = balances[balances[1]];
        assert(r == 9);
    }

    function memoryFieldAsMappingKey() public {
        Person memory carol;
        carol.age = 3;
        balances[carol.age] = 7;
        uint r = balances[3];
        assert(r == 7);
    }

    function mappingEntryThroughMemoryToMappingEntry() public {
        people[1].age = 3;
        Person memory p = people[1];
        people[2] = p;
        people[1].age = 4;
        uint r = people[2].age;
        assert(r == 3);
    }

    function boolKeyMapping() public {
        boolKeyed[true] = 1;
        boolKeyed[false] = 2;
        uint r = boolKeyed[true];
        uint s = boolKeyed[false];
        assert(r == 1);
        assert(s == 2);
    }

    function boolIsTrueOrFalse(bool b) public pure {
        bool x = b == true;
        bool y = b == false;
        assert(x || y);
    }

    function boolKeyMappingSymbolicKey(bool b) public {
        boolKeyed[true] = 1;
        boolKeyed[false] = 2;
        uint r = boolKeyed[b];
        assert(r == 1 || r == 2);
    }

    function recursiveStructMapping() public {
        mapping(uint => Tree) storage kids = tree.kids;
        Tree storage child = kids[0];
        tree.v = 1;
        child.v = 2;
        mapping(uint => Tree) storage grandKids = child.kids;
        grandKids[3].v = 4;
        assert(tree.v == 1);
        assert(kids[0].v == 2);
        assert(grandKids[3].v == 4);
    }

    // ── Taclet coverage: one function per taclet no other example applies ──

    function storageSignedDivModAssign() public {
        int start = -7;
        signedTotal = start;
        signedTotal /= 2;
        int q = signedTotal;
        int minusThree = -3;
        assert(q == minusThree);
        signedTotal = start;
        signedTotal %= 2;
        int r = signedTotal;
        int minusOne = -1;
        assert(r == minusOne);
    }

    function storageFieldDeleteUnfold() public {
        alice.account.balance = 20;
        delete alice.account.balance;
        uint r = alice.account.balance;
        assert(r == 0);
    }

    function storageFieldPredecrementUnfold() public {
        alice.account.balance = 20;
        --alice.account.balance;
        uint r = alice.account.balance;
        assert(r == 19);
    }

    function storageFieldPostdecrementUnfold() public {
        alice.account.balance = 20;
        alice.account.balance--;
        uint r = alice.account.balance;
        assert(r == 19);
    }

    function storageRootPostdecrement() public {
        age = 10;
        age--;
        uint r = age;
        assert(r == 9);
    }

    function storageRootPredecrementAssign() public {
        age = 10;
        uint r = --age;
        assert(r == 9);
        assert(age == 9);
    }
}
