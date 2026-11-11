// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Taclet examples: memory values and copies between memory and storage.
// Conventions: README.md, "Writing an example".
contract Memory {
    struct Token { uint value; }
    struct Account { uint balance; Token token; }
    struct Person { Account account; uint age; }
    struct Basket { uint[] items; }
    struct Triple { uint[3] items; uint tag; }

    uint[] values;
    uint[][] matrix;
    uint[3] fixedValues;
    mapping(uint => Person) people;

    Person[] persons;
    Person alice;
    Basket basketA;

    function memoryDeepField() public pure {
        Person memory carol;
        carol.account.balance = 10;
        uint r = carol.account.balance;
        assert(r == 10);
    }

    function memoryFieldAddAssign() public pure {
        Person memory carol;
        carol.age = 30;
        carol.age += 4;
        uint r = carol.age;
        assert(r == 34);
    }

    function memoryFieldSubAssign() public pure {
        Person memory carol;
        carol.age = 30;
        carol.age -= 4;
        uint r = carol.age;
        assert(r == 26);
    }

    function memoryFieldMulAssign() public pure {
        Person memory carol;
        carol.age = 7;
        carol.age *= 4;
        uint r = carol.age;
        assert(r == 28);
    }

    function memoryFieldDivAssign() public pure {
        Person memory carol;
        carol.age = 30;
        carol.age /= 5;
        uint r = carol.age;
        assert(r == 6);
    }

    function memoryFieldModAssign() public pure {
        Person memory carol;
        carol.age = 30;
        carol.age %= 7;
        uint r = carol.age;
        assert(r == 2);
    }

    function memoryFieldAddAssignUnfold() public pure {
        Person memory carol;
        carol.account.balance = 20;
        carol.account.balance += 4;
        uint r = carol.account.balance;
        assert(r == 24);
    }

    function memoryFieldAddAssignNse() public pure {
        Person memory carol;
        carol.age = 30;
        carol.age += 2 * 3;
        uint r = carol.age;
        assert(r == 36);
    }

    function memoryFieldPreincrement() public pure {
        Person memory carol;
        carol.age = 30;
        ++carol.age;
        uint r = carol.age;
        assert(r == 31);
    }

    function memoryFieldPostincrement() public pure {
        Person memory carol;
        carol.age = 30;
        carol.age++;
        uint r = carol.age;
        assert(r == 31);
    }

    function memoryFieldPredecrement() public pure {
        Person memory carol;
        carol.age = 30;
        --carol.age;
        uint r = carol.age;
        assert(r == 29);
    }

    function memoryFieldPostdecrement() public pure {
        Person memory carol;
        carol.age = 30;
        carol.age--;
        uint r = carol.age;
        assert(r == 29);
    }

    function memoryFieldPreincrementAssignment() public pure {
        Person memory carol;
        carol.age = 30;
        uint r = ++carol.age;
        assert(r == 31);
        assert(carol.age == 31);
    }

    function memoryFieldPostincrementAssignment() public pure {
        Person memory carol;
        carol.age = 30;
        uint r = carol.age++;
        assert(r == 30);
        assert(carol.age == 31);
    }

    function memoryFieldPredecrementAssignment() public pure {
        Person memory carol;
        carol.age = 30;
        uint r = --carol.age;
        assert(r == 29);
        assert(carol.age == 29);
    }

    function memoryFieldPostdecrementAssignment() public pure {
        Person memory carol;
        carol.age = 30;
        uint r = carol.age--;
        assert(r == 30);
        assert(carol.age == 29);
    }

    function memoryFieldPreincrementUnfold() public pure {
        Person memory carol;
        carol.account.balance = 20;
        ++carol.account.balance;
        uint r = carol.account.balance;
        assert(r == 21);
    }

    function memoryFieldAlias() public pure {
        Person memory carol;
        Account memory acc = carol.account;
        acc.balance = 100;
        uint r = carol.account.balance;
        assert(r == 100);
    }

    function memoryFieldReferenceAssign() public pure {
        Person memory carol;
        Person memory david;
        Account memory pv = david.account;
        carol.account = pv;
        carol.account.balance = 60;
        uint r = david.account.balance;
        assert(r == 60);
    }

    function memoryIndexArrayAddAssign() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        xs[1] += 2;
        uint r = xs[1];
        assert(r == 42);
    }

    function memoryIndexArraySubAssign() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        xs[1] -= 2;
        uint r = xs[1];
        assert(r == 38);
    }

    function memoryIndexArrayMulAssign() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 7;
        xs[1] *= 6;
        uint r = xs[1];
        assert(r == 42);
    }

    function memoryIndexArrayDivAssign() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        xs[1] /= 8;
        uint r = xs[1];
        assert(r == 5);
    }

    function memoryIndexArrayModAssign() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        xs[1] %= 7;
        uint r = xs[1];
        assert(r == 5);
    }

    function memoryIndexArrayAddAssignUnfold() public pure {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 40;
        basket.items[1] += 2;
        uint r = basket.items[1];
        assert(r == 42);
    }

    function memoryIndexArrayPreincrementUnfold() public pure {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 40;
        ++basket.items[1];
        uint r = basket.items[1];
        assert(r == 41);
    }

    function memoryIndexArrayPreincrement() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        ++xs[1];
        uint r = xs[1];
        assert(r == 41);
    }

    function memoryIndexArrayPostdecrement() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        xs[1]--;
        uint r = xs[1];
        assert(r == 39);
    }

    function memoryIndexArrayPostincrementAssignment() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        uint r = xs[1]++;
        assert(r == 40);
        assert(xs[1] == 41);
    }

    function memoryIndexArrayPredecrementAssignment() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        uint r = --xs[1];
        assert(r == 39);
        assert(xs[1] == 39);
    }

    /// @custom:key box
    function memoryIndexWriteNse(uint i, uint lhs, uint rhs) public pure {
        require(i == 1 && lhs == 4 && rhs == 5);
        uint[] memory xs = new uint[](4);
        xs[i+1] = lhs + rhs;
        uint r = xs[i+1];
        assert(r == 9);
    }

    function memoryRootAlias() public pure {
        Person memory carol;
        Person memory david;
        david.age = 40;
        carol = david;
        carol.age = 41;
        uint r = david.age;
        assert(r == 41);
    }

    function memoryRootDeleteFresh() public pure {
        Person memory carol;
        delete carol;
    }

    function memoryStructArrayIndex() public pure {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 33;
        uint r = basket.items[1];
        assert(r == 33);
    }

    function memoryToStorage() public {
        Person memory carol;
        carol.age = 44;
        alice = carol;
        uint r = alice.age;
        assert(r == 44);
    }

    function memoryToStorageIndexMappingCopyRootExample() public {
        Person memory carol;
        carol.age = 6;
        people[1] = carol;
        uint r = people[1].age;
        assert(r == 6);
    }

    /// @custom:key box
    function memoryToStorageIndexArrayCopyRootExample() public {
        require(0 < persons.length);
        Person memory carol;
        carol.age = 6;
        persons[0] = carol;
        uint r = persons[0].age;
        assert(r == 6);
    }

    /// @custom:key box
    function memoryToStorageIndexArrayCopyRootOutOfBoundsReverts() public {
        require(persons.length == 0);
        Person memory carol;
        persons[0] = carol;
        assert(false);
    }

    function storageToMemory() public {
        alice.age = 27;
        Person memory carol = alice;
        alice.age = 30;
        uint r = carol.age;
        assert(r == 27);
    }

    function testMemoryAliasing() public pure {
        Person memory carol;
        Account memory carolAcc = carol.account;
        carolAcc.balance = 100;
        assert(carol.account.balance == 100);
    }

    // import
    function testMemoryDeleteAlias() public pure {
        Person memory carol;
        Account memory carolAcc = carol.account;
        carolAcc.balance = 100;
        delete carolAcc;
        uint b = carolAcc.balance;
        assert(b == 0);
    }

    // import
    function testMemoryDeleteIdentityFieldFreshensSlot() public pure {
        Person memory carol;
        Account memory carolAcc = carol.account;
        carolAcc.balance = 100;
        delete carol.account;
        assert(carol.account.balance == 0);
        assert(carolAcc.balance == 100);
    }

    function testMemoryDeletePrimitiveField() public pure {
        Person memory carol;
        carol.age = 20;
        delete carol.age;
        assert(carol.age == 0);
    }

    function testMemoryFieldShallowCopy() public pure {
        Person memory carol;
        Person memory david;
        david.account.balance = 50;
        carol.account = david.account;
        carol.account.balance = 60;
        assert(david.account.balance == 60);
        assert(carol.account.balance == 60);
    }

    function testMemoryRootAlias() public pure {
        Person memory carol;
        Person memory david;
        david.age = 40;
        carol = david;
        carol.age = 41;
        assert(david.age == 41);
        assert(carol.age == 41);
    }

    // import
    function testMemoryRootDeleteRebindsOnlyLocal() public pure {
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

    function testMemoryTokenArrayAuxiliaryCases() public pure {
        Token[] memory carolTokens = new Token[](3);
        Token[] memory davidTokens = new Token[](3);
        uint i = 0;
        uint j = 0;
        Token memory tmp;
        tmp.value = 7;
        davidTokens[j] = tmp;
        carolTokens[++i] = davidTokens[j];
        Token memory carolTok = carolTokens[i];
        carolTok.value = 9;
        delete carolTokens[i];
        assert(carolTok.value == 9);
        assert(carolTokens[1].value == 0);
    }

    function testMemoryUintArrayAuxiliaryCases() public pure {
        uint[] memory carolValues = new uint[](3);
        uint i = 0;
        carolValues[++i] = 77;
        uint v = carolValues[i];
        delete carolValues[i];
        assert(v == 77);
        assert(carolValues[1] == 0);
    }

    function testMemoryUintArrayPostdecrement() public pure {
        uint[] memory carolValues = new uint[](3);
        uint i = 1;
        carolValues[i--] = 77;
        uint v = carolValues[1];
        delete carolValues[1];
        assert(i == 0);
        assert(v == 77);
        assert(carolValues[1] == 0);
    }

    function testMemoryUintArrayPostincrement() public pure {
        uint[] memory carolValues = new uint[](3);
        uint i = 0;
        carolValues[i++] = 77;
        uint v = carolValues[0];
        delete carolValues[0];
        assert(i == 1);
        assert(v == 77);
        assert(carolValues[0] == 0);
    }

    function testMemoryUintArrayPredecrement() public pure {
        uint[] memory carolValues = new uint[](3);
        uint i = 1;
        carolValues[--i] = 77;
        uint v = carolValues[i];
        delete carolValues[i];
        assert(i == 0);
        assert(v == 77);
        assert(carolValues[0] == 0);
    }

    function testMemoryStructFixedMemberLength() public pure {
        Triple memory t;
        assert(t.items.length == 3);
        t.items[1] = 5;
        assert(t.items[1] == 5);
        assert(t.items[0] == 0);
    }

    function testMemoryFixedArrayLength() public pure {
        uint[3] memory x;
        assert(x.length == 3);
        x[1] = 5;
        assert(x[1] == 5);
        assert(x[0] == 0);
    }

    function testMemoryNestedFixedArrayLength() public pure {
        uint[2][3] memory y;
        assert(y.length == 3);
        assert(y[0].length == 2);
    }

    function testMemoryDynamicArrayDefaultLength() public pure {
        uint[] memory d;
        assert(d.length == 0);
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

    function fixedArrayCopyFromMemory() public {
        fixedValues[0] = 1;
        fixedValues[1] = 2;
        fixedValues[2] = 3;
        uint[3] memory src;
        src[1] = 11;
        fixedValues = src;
        assert(fixedValues[0] == 0 && fixedValues[1] == 11 && fixedValues[2] == 0);
    }

    function storageArrayCopyShorterFromMemory() public {
        delete values;
        values.push(1);
        values.push(2);
        values.push(3);
        uint[] memory src = new uint[](1);
        src[0] = 11;
        values = src;
        assert(values.length == 1);
        assert(values[0] == 11);
        values.push();
        values.push();
        assert(values[1] == 0 && values[2] == 0);
    }

    // ── Memory ──

    function memoryDeclDefault() public pure {
        Person memory carol;
        uint r = carol.age;
        assert(r == 0);
    }

    function memoryArrayIndex() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 33;
        uint r = xs[1];
        assert(r == 33);
    }

    function memoryNewIntoStructField() public pure {
        Basket memory basket;
        basket.items = new uint[](2);
        assert(basket.items.length == 2);
    }

    function memoryNewIntoNestedField() public pure {
        Basket[] memory bs = new Basket[](1);
        bs[0].items = new uint[](3);
        assert(bs[0].items.length == 3);
    }

    function memoryNewIntoIndex() public pure {
        uint[][] memory xs = new uint[][](1);
        xs[0] = new uint[](3);
        assert(xs[0].length == 3);
    }

    function storageNewIntoField() public {
        basketA.items = new uint[](2);
        assert(basketA.items.length == 2);
    }

    // ── Taclet coverage: one function per taclet no other example applies ──

    function memoryIndexWriteStorageRefRhsCapture() public {
        values = new uint[](1);
        values[0] = 7;
        uint[][] memory xs = new uint[][](2);
        xs[0] = values;
        xs[1] = values;
        values[0] = 9;
        xs[0][0] = 11;
        assert(xs[0].length == 1);
        assert(xs[1].length == 1);
        assert(xs[0][0] == 11);
        assert(xs[1][0] == 7);
        assert(values[0] == 9);
        uint[] memory fresh = new uint[](1);
        assert(fresh[0] == 0);
        assert(xs[1][0] == 7);
    }

    function memoryIndexWriteStorageStructCopy() public {
        persons.push();
        persons[0].account.balance = 7;
        Person[] memory xs = new Person[](2);
        xs[0] = persons[0];
        xs[1] = persons[0];
        persons[0].account.balance = 9;
        xs[0].account.balance = 11;
        assert(xs[0].account.balance == 11);
        assert(xs[1].account.balance == 7);
        assert(persons[0].account.balance == 9);
    }

    /// @custom:key box
    function memoryIndexWriteStorageRefOutOfBoundsReverts() public view {
        uint[][] memory xs = new uint[][](0);
        xs[0] = values;
        assert(false);
    }

    /// @custom:key box
    function memoryIndexWriteStorageRefSourceOutOfBoundsReverts() public view {
        require(matrix.length == 0);
        uint[][] memory xs = new uint[][](1);
        xs[0] = matrix[0];
        assert(false);
    }

    function memoryIndexWriteMemRefRhsCapture() public pure {
        Token[] memory ts = new Token[](2);
        Person memory carol;
        carol.account.token.value = 7;
        ts[0] = carol.account.token;
        uint r = ts[0].value;
        assert(r == 7);
    }

    function memoryIndexDeleteNonSimpleIndexCapture() public pure {
        uint[] memory xs = new uint[](3);
        uint i = 0;
        xs[1] = 5;
        delete xs[i + 1];
        uint r = xs[1];
        assert(r == 0);
    }

    function memoryFieldSubAssignUnfold() public pure {
        Person memory carol;
        carol.account.balance = 20;
        carol.account.balance -= 4;
        uint r = carol.account.balance;
        assert(r == 16);
    }

    function memoryFieldMulAssignUnfold() public pure {
        Person memory carol;
        carol.account.balance = 7;
        carol.account.balance *= 6;
        uint r = carol.account.balance;
        assert(r == 42);
    }

    function memoryFieldDivAssignUnfold() public pure {
        Person memory carol;
        carol.account.balance = 40;
        carol.account.balance /= 8;
        uint r = carol.account.balance;
        assert(r == 5);
    }

    function memoryFieldModAssignUnfold() public pure {
        Person memory carol;
        carol.account.balance = 40;
        carol.account.balance %= 7;
        uint r = carol.account.balance;
        assert(r == 5);
    }

    function memoryFieldPostincrementUnfold() public pure {
        Person memory carol;
        carol.account.balance = 20;
        carol.account.balance++;
        uint r = carol.account.balance;
        assert(r == 21);
    }

    function memoryFieldPredecrementUnfold() public pure {
        Person memory carol;
        carol.account.balance = 20;
        --carol.account.balance;
        uint r = carol.account.balance;
        assert(r == 19);
    }

    function memoryFieldPostdecrementUnfold() public pure {
        Person memory carol;
        carol.account.balance = 20;
        carol.account.balance--;
        uint r = carol.account.balance;
        assert(r == 19);
    }

    function memoryFieldDeleteUnfold() public pure {
        Person memory carol;
        carol.account.balance = 20;
        delete carol.account.balance;
        uint r = carol.account.balance;
        assert(r == 0);
    }

    function memoryFieldReadUnfoldResult() public pure {
        Person memory carol;
        Person memory david;
        david.age = 40;
        carol.account.balance = david.age;
        uint r = carol.account.balance;
        assert(r == 40);
    }

    function memoryIndexReadUnfoldResult() public pure {
        Person memory carol;
        uint[] memory xs = new uint[](2);
        xs[1] = 40;
        carol.account.balance = xs[1];
        uint r = carol.account.balance;
        assert(r == 40);
    }

    function memoryIndexArraySubAssignUnfold() public pure {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 40;
        basket.items[1] -= 2;
        uint r = basket.items[1];
        assert(r == 38);
    }

    function memoryIndexArrayMulAssignUnfold() public pure {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 7;
        basket.items[1] *= 6;
        uint r = basket.items[1];
        assert(r == 42);
    }

    function memoryIndexArrayDivAssignUnfold() public pure {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 40;
        basket.items[1] /= 8;
        uint r = basket.items[1];
        assert(r == 5);
    }

    function memoryIndexArrayModAssignUnfold() public pure {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 40;
        basket.items[1] %= 7;
        uint r = basket.items[1];
        assert(r == 5);
    }

    function memoryIndexArrayPostincrementUnfold() public pure {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 40;
        basket.items[1]++;
        uint r = basket.items[1];
        assert(r == 41);
    }

    function memoryIndexArrayPredecrementUnfold() public pure {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 40;
        --basket.items[1];
        uint r = basket.items[1];
        assert(r == 39);
    }

    function memoryIndexArrayPostdecrementUnfold() public pure {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 40;
        basket.items[1]--;
        uint r = basket.items[1];
        assert(r == 39);
    }

    function memoryIndexArrayDeleteUnfold() public pure {
        Basket memory basket;
        uint[] memory xs = new uint[](4);
        basket.items = xs;
        basket.items[1] = 40;
        delete basket.items[1];
        uint r = basket.items[1];
        assert(r == 0);
    }

    function memoryIndexArrayPostincrement() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        xs[1]++;
        uint r = xs[1];
        assert(r == 41);
    }

    function memoryIndexArrayPredecrement() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        --xs[1];
        uint r = xs[1];
        assert(r == 39);
    }

    function memoryIndexArrayPreincrementAssignment() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        uint r = ++xs[1];
        assert(r == 41);
        assert(xs[1] == 41);
    }

    function memoryIndexArrayPostdecrementAssignment() public pure {
        uint[] memory xs = new uint[](4);
        xs[1] = 40;
        uint r = xs[1]--;
        assert(r == 40);
        assert(xs[1] == 39);
    }
}
