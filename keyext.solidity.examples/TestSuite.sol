// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

/// One contract for the taclet example problems. Every `.key` file under
/// keyext.solidity.examples/{taclets,mainFeatures} calls exactly one function declared here, so
/// each test program is real Solidity that `solc` parses and type-checks.
///
/// Conventions:
///   - the function name is the `.key` basename in lowerCamelCase;
///   - a test that observes a single value uses a named return, assigned by plain assignment
///     (`return e;` is not supported by the calculus);
///   - a test that observes storage/memory/net only has no return value;
///   - a test that observes several values asserts in the body and its `.key` postcondition is
///     `true`.
contract TestSuite {
    struct Token { uint value; }
    struct Account { uint balance; Token token; }
    struct Person { Account account; uint age; }
    struct Basket { uint[] items; }
    struct Ledger { uint nonce; mapping(uint => uint) balances; }
    struct LedgerUse { Ledger ledger; }
    struct TokenBucket { Token[] tokens; }

    uint total;
    uint age;
    address owner;
    uint balance;

    uint[] values;
    uint[] a;
    uint[][] matrix;

    mapping(uint => uint) balances;
    mapping(uint => Person) people;
    mapping(uint => bool) flags;
    mapping(uint => uint) valuesMap;
    mapping(uint => Account) accountMap;

    Person[] persons;
    Person alice;
    Person bob;

    Ledger ledger;
    Token[] tokens;
    TokenBucket bucket;
    LedgerUse[] ledgerUses;

    // ── Arithmetic ──

    function additionStorageWrite(uint x, uint y) public returns (uint r) {
        alice.age = x + y;
        r = alice.age;
    }

    // ── Storage: root ──

    function storageRootReadWrite() public returns (uint r) {
        age = 34;
        r = age;
    }

    // ── Storage: field ──

    function storageFieldWriteRead() public returns (uint r) {
        alice.age = 34;
        r = alice.age;
    }

    function storageFieldDeepAddAssign() public returns (uint r) {
        alice.account.balance = 30;
        alice.account.balance += 4;
        r = alice.account.balance;
    }

    function storageFieldGlobalAgeWrite() public {
        alice.age = 34;
    }

    function storageFieldGlobalAgeRead() public returns (uint r) {
        r = alice.age;
    }

    // ── Storage: alias ──

    function storageAliasWrite() public {
        Person storage p = alice;
        Account storage acc = p.account;
        acc.balance = 100;
        p.account.token.value = 3;
    }

    // ── Storage: index ──

    function storageIndexRootMapping() public returns (uint r) {
        balances[1] = 42;
        r = balances[1];
    }

    function storageIndexAddAssign() public returns (uint r) {
        values[1] = 40;
        values[1] += 2;
        r = values[1];
    }

    function storageIndexArrayOutOfBoundsBox() public {
        values[1] = 7;
    }

    // ── Storage: push ──

    function storagePushValue() public {
        values.push(42);
    }

    // ── Memory ──

    function memoryDeclDefault() public returns (uint r) {
        Person memory carol;
        r = carol.age;
    }

    function memoryArrayIndex() public returns (uint r) {
        uint[] memory xs = new uint[](4);
        xs[1] = 33;
        r = xs[1];
    }

    // ── mainFeatures ──

    function testStorageComplexReceiverEmptyPush() public {
        bucket.tokens.push();
        Token[] storage bt = bucket.tokens;
        assert(bt.length == 1);
    }

    // ── generated: one function per example ──

    function additionBothStorage() public returns (uint r) {
        alice.age = 10;
        bob.age = 5;
        r = alice.age + bob.age;
    }

    function additionSimple() public returns (uint r) {
        r = 1 + 2;
    }

    function additionStorageRead() public returns (uint r) {
        alice.age = 10;
        r = alice.age + 1;
    }

    function divisionSimple() public returns (uint r) {
        r = 8 / 2;
    }

    function greaterEqualSimple() public returns (bool r) {
        r = 5 >= 6;
    }

    function greaterThanSimple() public returns (bool r) {
        r = 5 > 3;
    }

    function lessEqualSimple() public returns (bool r) {
        r = 5 <= 5;
    }

    function lessThanSimple() public returns (bool r) {
        r = 3 < 5;
    }

    function logicalAndSimple() public returns (bool r) {
        r = true && false;
    }

    function logicalNotSimple() public returns (bool r) {
        r = !false;
    }

    function logicalOrSimple() public returns (bool r) {
        r = true || false;
    }

    function memoryDeclFresh() public {
        Person memory carol;
    }

    function memoryDeepField() public returns (uint r) {
        Person memory carol;
        carol.account.balance = 10;
        r = carol.account.balance;
    }

    function memoryFieldAlias() public returns (uint r) {
        Person memory carol;
        Account memory acc = carol.account;
        acc.balance = 100;
        r = carol.account.balance;
    }

    function memoryFieldReferenceAssign() public returns (uint r) {
        Person memory carol;
        Person memory david;
        Account memory pv = david.account;
        carol.account = pv;
        carol.account.balance = 60;
        r = david.account.balance;
    }

    function memoryIndexWriteNse(uint i, uint a, uint b) public returns (uint r) {
        uint[] memory xs = new uint[](4);
        xs[i+1] = a + b;
        r = xs[i+1];
    }

    function memoryRootAlias() public returns (uint r) {
        Person memory carol;
        Person memory david;
        david.age = 40;
        carol = david;
        carol.age = 41;
        r = david.age;
    }

    function memoryRootDeleteFresh() public {
        Person memory carol;
        delete carol;
    }

    function memoryStructArrayIndex() public returns (uint r) {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 33;
        r = basket.items[1];
    }

    function memoryToStorage() public returns (uint r) {
        Person memory carol;
        carol.age = 44;
        alice = carol;
        r = alice.age;
    }

    function moduloSimple() public returns (uint r) {
        r = 7 % 3;
    }

    function multiplicationSimple() public returns (uint r) {
        r = 3 * 4;
    }

    function notEqualSimple() public returns (bool r) {
        r = 3 != 4;
    }

    function powerSimple() public returns (uint r) {
        r = 2 ** 3;
    }

    function requireGuardBox(uint x) public returns (uint r) {
        require(x > 0);
        age = 1;
        r = age;
    }

    function requireHoldsDiamond(uint x) public returns (uint r) {
        require(x > 0);
        total = x;
        r = total;
    }

    function storageAliasRebindAlias() public returns (uint r) {
        Person storage alicePath = alice;
        bob.age = 20;
        alicePath = bob;
        r = alicePath.age;
    }

    function storageDeepFieldPostincrement() public returns (uint r) {
        alice.account.balance = 100;
        alice.account.balance++;
        r = alice.account.balance;
    }

    function storageDeepFieldPreincrement() public returns (uint r) {
        alice.account.balance = 100;
        ++alice.account.balance;
        r = alice.account.balance;
    }

    function storageFieldAddAssign() public returns (uint r) {
        alice.age = 30;
        alice.age += 4;
        r = alice.age;
    }

    function storageFieldCopyStruct() public returns (uint r) {
        bob.account.balance = 11;
        Account storage acc = bob.account;
        alice.account = acc;
        r = alice.account.balance;
    }

    function storageFieldCopyValueField() public returns (uint r) {
        bob.age = 30;
        alice.age = bob.age;
        r = alice.age;
    }

    function storageFieldDeepDivAssign() public returns (uint r) {
        alice.account.balance = 36;
        alice.account.balance /= 4;
        r = alice.account.balance;
    }

    function storageFieldDeepModAssign() public returns (uint r) {
        alice.account.balance = 37;
        alice.account.balance %= 4;
        r = alice.account.balance;
    }

    function storageFieldDeepMulAssign() public returns (uint r) {
        alice.account.balance = 8;
        alice.account.balance *= 3;
        r = alice.account.balance;
    }

    function storageFieldDeepSubAssign() public returns (uint r) {
        alice.account.balance = 30;
        alice.account.balance -= 4;
        r = alice.account.balance;
    }

    function storageFieldDeepWriteRead() public returns (uint r) {
        alice.account.balance = 34;
        r = alice.account.balance;
    }

    function storageFieldDelete() public returns (uint r) {
        alice.age = 30;
        delete alice.age;
        r = alice.age;
    }

    function storageFieldDivAssign() public returns (uint r) {
        alice.age = 30;
        alice.age /= 5;
        r = alice.age;
    }

    function storageFieldModAssign() public returns (uint r) {
        alice.age = 30;
        alice.age %= 7;
        r = alice.age;
    }

    function storageFieldMulAssign() public returns (uint r) {
        alice.age = 7;
        alice.age *= 4;
        r = alice.age;
    }

    function storageFieldPostdecrementAssign() public returns (uint r) {
        alice.age = 30;
        r = alice.age--;
    }

    function storageFieldPostdecrement() public returns (uint r) {
        alice.age = 30;
        alice.age--;
        r = alice.age;
    }

    function storageFieldPostincrementAssign() public returns (uint r) {
        alice.age = 30;
        r = alice.age++;
    }

    function storageFieldPostincrement() public returns (uint r) {
        alice.age = 30;
        alice.age++;
        r = alice.age;
    }

    function storageFieldPredecrementAssign() public returns (uint r) {
        alice.age = 30;
        r = --alice.age;
    }

    function storageFieldPredecrement() public returns (uint r) {
        alice.age = 30;
        --alice.age;
        r = alice.age;
    }

    function storageFieldPreincrementAssign() public returns (uint r) {
        alice.age = 30;
        r = ++alice.age;
    }

    function storageFieldPreincrement() public returns (uint r) {
        alice.age = 30;
        ++alice.age;
        r = alice.age;
    }

    function storageFieldReadBindLocal() public returns (uint r) {
        Account storage acc = alice.account;
        bob.account.balance = 42;
        acc = bob.account;
        r = acc.balance;
    }

    function storageFieldReadStoreRoot() public returns (uint r) {
        alice.age = 17;
        total = alice.age;
        r = total;
    }

    function storageFieldSubAssign() public returns (uint r) {
        alice.age = 30;
        alice.age -= 5;
        r = alice.age;
    }

    function storageFieldWriteCaptureSrc() public returns (uint r) {
        bob.account.balance = 11;
        alice.account = bob.account;
        r = alice.account.balance;
    }

    function storageFieldWriteRhsCapture(uint x, uint y, uint z) public returns (uint r) {
        alice.age = x + y*z;
        r = alice.age;
    }

    function storageIndexCopyValue(uint i, uint j) public returns (uint r) {
        values[1] = 8;
        balances[i] = values[j];
        r = balances[5];
    }

    function storageIndexCopysourceAfterPush() public {
        uint[][] storage m = matrix;
        m.push();
        m[0] = values;
    }

    function storageIndexDecomposeAfterPush() public returns (uint r) {
        matrix[0].push(100);
        matrix[0][0] = 7;
        r = matrix[0][0];
    }

    function storageIndexDeleteMappingBool() public returns (bool r) {
        flags[1] = true;
        delete flags[1];
        r = flags[1];
    }

    function storageIndexDeleteMappingStruct() public {
        delete people[1];
    }

    function storageIndexDeleteNseIndex(uint k) public returns (uint r) {
        balances[3] = 7;
        delete balances[k+1];
        r = balances[3];
    }

    function storageIndexDelete() public returns (uint r) {
        values[1] = 7;
        delete values[1];
        r = values[1];
    }

    function storageIndexDivAssign() public returns (uint r) {
        values[1] = 40;
        values[1] /= 8;
        r = values[1];
    }

    function storageIndexModAssign() public returns (uint r) {
        values[1] = 40;
        values[1] %= 6;
        r = values[1];
    }

    function storageIndexMulAssign() public returns (uint r) {
        values[1] = 5;
        values[1] *= 6;
        r = values[1];
    }

    function storageIndexMultipleWrites() public returns (uint r) {
        values[3] = 5;
        values[3] = 8;
        r = values[3];
    }

    function storageIndexPostdecrementAssign() public returns (uint r) {
        values[1] = 40;
        r = values[1]--;
    }

    function storageIndexPostdecrement() public returns (uint r) {
        values[1] = 40;
        values[1]--;
        r = values[1];
    }

    function storageIndexPostincrementAssign() public returns (uint r) {
        values[1] = 40;
        r = values[1]++;
    }

    function storageIndexPostincrement() public returns (uint r) {
        values[1] = 40;
        values[1]++;
        r = values[1];
    }

    function storageIndexPredecrementAssign() public returns (uint r) {
        values[1] = 40;
        r = --values[1];
    }

    function storageIndexPredecrement() public returns (uint r) {
        values[1] = 40;
        --values[1];
        r = values[1];
    }

    function storageIndexPreincrementAssign() public returns (uint r) {
        values[1] = 40;
        r = ++values[1];
    }

    function storageIndexPreincrement() public returns (uint r) {
        values[1] = 40;
        ++values[1];
        r = values[1];
    }

    function storageIndexReadMappingStoreRoot(uint k) public returns (uint r) {
        balances[1] = 42;
        total = balances[k];
        r = total;
    }

    function storageIndexReadNseIndex(uint i) public returns (uint r) {
        values[2] = 9;
        r = values[i+1];
    }

    function storageIndexSubAssign() public returns (uint r) {
        values[1] = 40;
        values[1] -= 8;
        r = values[1];
    }

    function storageIndexWriteNseChain(uint i, uint x, uint y) public returns (uint r) {
        balances[i+1] = x*y + 3;
        r = balances[3];
    }

    function storageLocalDeclSkip() public returns (uint r) {
        Person storage p;
        age = 7;
        r = age;
    }

    function storageMatrixNseIndex(uint i, uint j, uint x, uint y) public {
        matrix[i+1][j+1] = x + y;
    }

    function storageMatrixWriteRead() public returns (uint r) {
        matrix[2][3] = 99;
        r = matrix[2][3];
    }

    function storagePopAfterPush() public {
        values.push();
        values.pop();
    }

    function storagePopEmptyBox() public {
        values.pop();
    }

    function storagePopNonempty() public {
        values.pop();
    }

    function storagePushEmpty() public {
        values.push();
    }

    function storagePushLocalBind() public {
        Person storage p;
        p = persons.push();
    }

    function storagePushNonsimpleArg(uint x, uint y) public {
        values.push(x + y);
    }

    function storagePushReturnAssign() public {
        values.push() = 42;
    }

    function storageRootAddAssign() public returns (uint r) {
        age = 10;
        age += 5;
        r = age;
    }

    function storageRootCopySource() public returns (uint r) {
        age = 34;
        balance = age;
        r = balance;
    }

    function storageRootCopyStruct() public returns (uint r) {
        bob.age = 7;
        alice = bob;
        r = alice.age;
    }

    function storageRootDeleteStruct() public returns (uint r) {
        alice.age = 30;
        delete alice;
        r = alice.age;
    }

    function storageRootDelete() public returns (uint r) {
        age = 10;
        delete age;
        r = age;
    }

    function storageRootDisjoint() public returns (uint r) {
        age = 7;
        balance = 9;
        r = age;
    }

    function storageRootDivAssign() public returns (uint r) {
        age = 20;
        age /= 4;
        r = age;
    }

    function storageRootModAssign() public returns (uint r) {
        age = 17;
        age %= 5;
        r = age;
    }

    function storageRootMulAssign() public returns (uint r) {
        age = 6;
        age *= 3;
        r = age;
    }

    function storageRootMultipleWrites() public returns (uint r) {
        age = 1;
        age = 2;
        r = age;
    }

    function storageRootPostdecrementAssign() public returns (uint r) {
        age = 10;
        r = age--;
    }

    function storageRootPostincrementAssign() public returns (uint r) {
        age = 10;
        r = age++;
    }

    function storageRootPostincrement() public returns (uint r) {
        age = 10;
        age++;
        r = age;
    }

    function storageRootPredecrement() public returns (uint r) {
        age = 10;
        --age;
        r = age;
    }

    function storageRootPreincrementAssign() public returns (uint r) {
        age = 10;
        r = ++age;
    }

    function storageRootPreincrement() public returns (uint r) {
        age = 10;
        ++age;
        r = age;
    }

    function storageRootSubAssign() public returns (uint r) {
        age = 10;
        age -= 4;
        r = age;
    }

    function storageRootWriteRhsCapture(uint x, uint y, uint z) public returns (uint r) {
        total = x + y*z;
        r = total;
    }

    function storageToMemory() public returns (uint r) {
        alice.age = 27;
        Person memory carol = alice;
        alice.age = 30;
        r = carol.age;
    }

    function subtractionSimple() public returns (uint r) {
        r = 7 - 2;
    }

    function subtractionStorageRead() public returns (uint r) {
        alice.age = 10;
        r = alice.age - 3;
    }

    function unaryMinusSimple(int x) public returns (int r) {
        r = -x;
    }

    function testDeepPopDoesNotResetMappingMember() public {
        ledgerUses.push();
        ledgerUses[0].ledger.balances[1] = 10;
        ledgerUses.pop();
        ledgerUses.push();
        assert(ledgerUses[0].ledger.balances[1] == 10);
    }

    function testMemoryAliasing() public {
        Person memory carol;
        Account memory carolAcc = carol.account;
        carolAcc.balance = 100;
        assert(carol.account.balance == 100);
    }

    function testMemoryDeleteAlias() public {
        Person memory carol;
        Account memory carolAcc = carol.account;
        carolAcc.balance = 100;
        delete carolAcc;
        uint b = carolAcc.balance;
        assert(b == 0);
    }

    function testMemoryDeleteIdentityFieldFreshensSlot() public {
        Person memory carol;
        Account memory carolAcc = carol.account;
        carolAcc.balance = 100;
        delete carol.account;
        assert(carol.account.balance == 0);
        assert(carolAcc.balance == 100);
    }

    function testMemoryDeletePrimitiveField() public {
        Person memory carol;
        carol.age = 20;
        delete carol.age;
        assert(carol.age == 0);
    }

    function testMemoryFieldShallowCopy() public {
        Person memory carol;
        Person memory david;
        david.account.balance = 50;
        carol.account = david.account;
        carol.account.balance = 60;
        assert(david.account.balance == 60);
        assert(carol.account.balance == 60);
    }

    function testMemoryRootAlias() public {
        Person memory carol;
        Person memory david;
        david.age = 40;
        carol = david;
        carol.age = 41;
        assert(david.age == 41);
        assert(carol.age == 41);
    }

    function testMemoryRootDeleteRebindsOnlyLocal() public {
        Person memory carol;
        Person memory carolAlias = carol;
        carol.age = 33;
        delete carol;
        assert(carol.age == 0);
        assert(carolAlias.age == 33);
    }

    function testMemoryToStorageCopyComplexSource() public {
        Person memory carol;
        carol.account.balance = 50;
        alice.account = carol.account;
        carol.account.balance = 51;
        assert(alice.account.balance == 50);
    }

    function testMemoryToStorageCopyComplexTarget() public {
        Token memory carolToken;
        carolToken.value = 99;
        alice.account.token = carolToken;
        carolToken.value = 100;
        assert(alice.account.token.value == 99);
    }

    function testMemoryToStorageCopyField() public {
        Account memory carolAcc;
        carolAcc.balance = 50;
        alice.account = carolAcc;
        carolAcc.balance = 51;
        assert(alice.account.balance == 50);
    }

    function testMemoryToStorageCopyRoot() public {
        Person memory carol;
        carol.age = 42;
        alice = carol;
        carol.age = 43;
        assert(alice.age == 42);
    }

    function testMemoryTokenArrayAuxiliaryCases() public {
        Token[] memory carolTokens = new Token[](3);
        Token[] memory davidTokens = new Token[](3);
        uint i = 0;
        uint j = 0;
        Token memory tmp;
        tmp.value = 7;
        davidTokens[j] = tmp;
        carolTokens[++i] = davidTokens[j];
        Token memory tok = carolTokens[i];
        tok.value = 9;
        delete carolTokens[i];
        assert(tok.value == 9);
        assert(carolTokens[1].value == 0);
    }

    function testMemoryUintArrayAuxiliaryCases() public {
        uint[] memory carolValues = new uint[](3);
        uint i = 0;
        carolValues[++i] = 77;
        uint v = carolValues[i];
        delete carolValues[i];
        assert(v == 77);
        assert(carolValues[1] == 0);
    }

    function testMemoryUintArrayPostdecrement() public {
        uint[] memory carolValues = new uint[](3);
        uint i = 1;
        carolValues[i--] = 77;
        uint v = carolValues[1];
        delete carolValues[1];
        assert(i == 0);
        assert(v == 77);
        assert(carolValues[1] == 0);
    }

    function testMemoryUintArrayPostincrement() public {
        uint[] memory carolValues = new uint[](3);
        uint i = 0;
        carolValues[i++] = 77;
        uint v = carolValues[0];
        delete carolValues[0];
        assert(i == 1);
        assert(v == 77);
        assert(carolValues[0] == 0);
    }

    function testMemoryUintArrayPredecrement() public {
        uint[] memory carolValues = new uint[](3);
        uint i = 1;
        carolValues[--i] = 77;
        uint v = carolValues[i];
        delete carolValues[i];
        assert(i == 0);
        assert(v == 77);
        assert(carolValues[0] == 0);
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

    function testStorageArrayReadWrite() public {
        tokens.push();
        tokens[0].value = 100;
        uint v = tokens[0].value;
        assert(v == 100);
    }

    function testStorageComplexReceiverPushFieldLvalue() public {
        bucket.tokens.push().value = 5;
        Token[] storage bt = bucket.tokens;
        assert(bt.length == 1);
        assert(bt[0].value == 5);
    }

    function testStorageComplexReceiverPushLvalueCopy() public {
        alice.account.token.value = 7;
        Token storage tokRef = alice.account.token;
        bucket.tokens.push() = tokRef;
        Token[] storage bt = bucket.tokens;
        assert(bt.length == 1);
        assert(bt[0].value == 7);
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

    function testStorageEvaluationOrder() public {
        uint i = 0;
        a.push(100);
        a.push(100);
        a.push(100);
        a[++i] = ++i;
        assert(a[2] == 1);
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

    function testStorageNestedPushReturnAlias() public {
        Account storage acc = persons.push().account;
        acc.balance = 50;
        assert(persons.length == 1);
        assert(acc.balance == 50);
    }

    function testStoragePushFieldLvalue() public {
        tokens.push().value = 11;
        assert(tokens.length == 1);
        assert(tokens[0].value == 11);
    }

    function testStoragePushLvalueCopiesStorageSource() public {
        alice.account.token.value = 9;
        Token storage storageRef = alice.account.token;
        tokens.push() = storageRef;
        assert(tokens.length == 1);
        assert(tokens[0].value == 9);
    }

    function testStoragePushLvaluePrimitive() public {
        values.push() = 77;
        assert(values.length == 1);
        assert(values[0] == 77);
    }

    function testStoragePushReturnAlias() public {
        Token storage t = tokens.push();
        t.value = 99;
        assert(tokens.length == 1);
        assert(tokens[0].value == 99);
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

    function testStorageToMemoryCopyComplexPath() public {
        alice.account.token.value = 17;
        Token memory t = alice.account.token;
        alice.account.token.value = 18;
        assert(t.value == 17);
    }

    function testStorageToMemoryCopyField() public {
        alice.account.balance = 10;
        Account memory acc = alice.account;
        alice.account.balance = 20;
        uint v = acc.balance;
        assert(v == 10);
    }

    function testStorageToMemoryCopyRoot() public {
        alice.age = 25;
        Person memory carol = alice;
        alice.age = 30;
        uint v = carol.age;
        assert(v == 25);
    }

    function testStorageWriteAndRead() public {
        uint x = 20;
        uint y = 1;
        alice.age = x + y;
        uint v = alice.age;
        assert(v == 21);
    }

    // ── split: one function per modality ──

    function storageFieldDecompositionWrite() public {
        alice.account.balance = 34;
    }

    function storageFieldDecompositionRead() public returns (uint r) {
        r = alice.account.balance;
    }

    function storageFieldDeepValueWrite() public {
        alice.account.token.value = 7;
    }

    function storageFieldDeepValueRead() public returns (uint r) {
        r = alice.account.token.value;
    }

    function storageIndexDecompositionWrite() public {
        matrix[2][3] = 34;
    }

    function storageIndexDecompositionRead() public returns (uint r) {
        r = matrix[2][3];
    }

    function storageIndexRootArrayWrite() public {
        values[1] = 42;
    }

    function storageIndexRootArrayRead() public returns (uint r) {
        r = values[1];
    }

    // ── shared: both conjuncts run the same program ──

    function storageFieldDisjointFields() public {
        alice.account.balance = 1;
        alice.account.token.value = 2;
    }

    function storageFieldDisjointRoots() public {
        alice.age = 1;
        bob.age = 2;
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

    function memoryDelete() public {
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
