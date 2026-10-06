// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract SolcStructsMappingsOpen {
    struct V { uint value; }
    struct PushZero { uint x; uint y; uint z; uint[3] a1; uint[] a2; }
    struct TA { uint y; uint[] a; }
    struct SA { uint x; TA t; uint[] a; TA[] ts; }

    uint pushB;
    PushZero[] pushZero;
    uint pushA;
    V[][] va;
    V[] vb;
    SA saStorage;
    SA[] saArray;

    /// solc: semanticTests/structs/struct_storage_push_zero_value.sol
    // open: push() on a storage array of structs does not zero the appended element; every field of pushZero[0] stays symbolic
    /// @custom:key box
    function pushedStructIsZero() public {
        require(pushZero.length == 0);
        pushB = 23;
        pushA = 17;
        pushZero.push();
        assert(pushZero[0].x == 0);
        assert(pushZero[0].y == 0);
        assert(pushZero[0].z == 0);
        assert(pushZero[0].a1[0] == 0);
        assert(pushZero[0].a1[1] == 0);
        assert(pushZero[0].a1[2] == 0);
        assert(pushZero[0].a2.length == 0);
        assert(pushB == 23);
        assert(pushA == 17);
    }

    /// solc: semanticTests/structs/copy_struct_array_from_storage.sol
    // open: va.push() and va[0].push() leave the appended inner array and struct symbolic (push does not zero non-primitive elements), so va[0].length == 3 is unprovable
    /// @custom:key box
    function copyStructArrayIntoNestedArray() public {
        require(va.length == 0);
        require(vb.length == 0);
        va.push();
        va[0].push();
        va[0][0].value = 1;
        va[0].push();
        va[0][1].value = 2;
        va[0].push();
        va[0][2].value = 3;
        vb.push();
        vb[0].value = 4;
        vb.push();
        vb[1].value = 5;
        vb.push();
        vb[2].value = 6;
        vb.push();
        vb[3].value = 7;
        va.push();
        va[1] = vb;
        assert(va.length == 2);
        assert(va[0].length == 3);
        assert(va[1].length == 4);
        assert(va[1][0].value == 4);
        assert(va[1][1].value == 5);
        assert(va[1][2].value == 6);
        assert(va[1][3].value == 7);
    }

    /// solc: semanticTests/structs/copy_struct_array_from_storage.sol
    // open: temp[0] = va[0] (storage array copied into an element of a memory array of arrays) has no rule: symbolic execution stops on the statement
    /// @custom:key box
    function copyStructArraysIntoMemoryRows() public {
        require(va.length == 1);
        require(va[0].length == 3);
        require(vb.length == 4);
        vb[0].value = 4;
        vb[1].value = 5;
        vb[2].value = 6;
        vb[3].value = 7;
        V[][] memory temp = new V[][](2);
        temp[0] = va[0];
        temp[1] = vb;
        assert(temp.length == 2);
        assert(temp[0].length == 3);
        assert(temp[1].length == 4);
        assert(temp[1][0].value == 4);
        assert(temp[1][3].value == 7);
    }

    /// solc: smtCheckerTests/types/struct/struct_array_struct_array_storage_safe.sol
    // open: closes only with -m 300000 (~130 s); exceeds the suite budget of 50000 steps / 30 s
    /// @custom:key box
    function storageNestedStructArrays() public {
        require(saStorage.a.length == 0);
        require(saStorage.ts.length == 0);
        saStorage.x = 2;
        assert(saStorage.x == 2);
        saStorage.t.y = 3;
        assert(saStorage.t.y == 3);
        saStorage.a.push();
        saStorage.a.push();
        saStorage.a.push();
        saStorage.a[2] = 4;
        assert(saStorage.a[2] == 4);
        saStorage.ts.push();
        saStorage.ts.push();
        saStorage.ts.push();
        saStorage.ts.push();
        saStorage.ts.push();
        saStorage.ts[3].y = 5;
        assert(saStorage.ts[3].y == 5);
        saStorage.ts[4].a.push();
        saStorage.ts[4].a.push();
        saStorage.ts[4].a.push();
        saStorage.ts[4].a.push();
        saStorage.ts[4].a.push();
        saStorage.ts[4].a.push();
        saStorage.ts[4].a[5] = 6;
        assert(saStorage.ts[4].a[5] == 6);
    }

    /// solc: smtCheckerTests/types/struct/array_struct_array_struct_storage_safe.sol
    // open: closes only with -m 300000 (~340 s); exceeds the suite budget of 50000 steps / 30 s
    /// @custom:key box
    function storageArrayOfNestedStructArrays() public {
        require(saArray.length == 0);
        saArray.push();
        saArray.push();
        saArray.push();
        saArray[0].x = 2;
        assert(saArray[0].x == 2);
        saArray[1].t.y = 3;
        assert(saArray[1].t.y == 3);
        saArray[2].a.push();
        saArray[2].a.push();
        saArray[2].a.push();
        saArray[2].a[2] = 4;
        assert(saArray[2].a[2] == 4);
        saArray[0].ts.push();
        saArray[0].ts.push();
        saArray[0].ts.push();
        saArray[0].ts.push();
        saArray[0].ts.push();
        saArray[0].ts[3].y = 5;
        assert(saArray[0].ts[3].y == 5);
        saArray[1].ts.push();
        saArray[1].ts.push();
        saArray[1].ts.push();
        saArray[1].ts.push();
        saArray[1].ts.push();
        saArray[1].ts[4].a.push();
        saArray[1].ts[4].a.push();
        saArray[1].ts[4].a.push();
        saArray[1].ts[4].a.push();
        saArray[1].ts[4].a.push();
        saArray[1].ts[4].a.push();
        saArray[1].ts[4].a[5] = 6;
        assert(saArray[1].ts[4].a[5] == 6);
        saArray.pop();
        saArray.pop();
        saArray.pop();
    }
}
