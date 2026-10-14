// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract SolcArrayMembersOpen {
    struct B { int[] b; }
    struct T { B s; }
    struct PS { uint a; uint b; uint[3] c; uint[] d; }

    uint[][] nested;
    uint[][] nestedFromMemory;
    uint[] dyn1;
    uint[] dyn2;
    uint[][] nestedToMemory;
    uint[][] zero2d;
    uint[][] lhs2d;
    uint[][][] lhs3d;
    int[] compound;
    B sb;
    T tsb;
    uint[][] push2dArg;
    uint[] popIso;
    PS[] structData;
    int[][] pointer2d;
    uint[9] staticData1;
    uint[] staticData2;
    uint[40] big;
    uint[20] small;
    uint[][] lenArr;
    uint[][] lenArr2;

    function threeRows() internal returns (int[] storage) {
        pointer2d.push();
        pointer2d.push();
        pointer2d.push();
        return pointer2d[2];
    }

    // open: push(m) of a memory array onto a storage array of arrays is stuck on the program text
    /// solc: semanticTests/array/push/array_push_nested_from_memory.sol
    function pushNestedFromMemory() public {
        delete nestedFromMemory;
        uint[] memory m = new uint[](3);
        m[0] = 1;
        nestedFromMemory.push(m);
        assert(nestedFromMemory.length == 1);
        assert(nestedFromMemory[0].length == m.length);
        assert(nestedFromMemory[0].length > 0);
        uint r = nestedFromMemory[0][0];
        assert(r == 1);
    }

    // open: assigning new uint[](n) directly to a storage array is stuck on the program text
    /// solc: semanticTests/array/copying/array_copy_storage_storage_dyn_dyn.sol
    function copyStorageStorageDynDyn() public {
        dyn1 = new uint[](10);
        dyn1[5] = 4;
        dyn2 = dyn1;
        uint len = dyn2.length;
        uint val = dyn2[5];
        assert(len == 10);
        assert(val == 4);
    }

    // open: a compound assignment to push() (u.push() -= 1) is stuck on the program text
    /// solc: smtCheckerTests/array_members/push_as_lhs_compound_assignment.sol
    /// @custom:key box
    function pushLhsCompound() public {
        require(compound.length == 0);
        compound.push() -= 1;
        assert(compound[0] < 0);
    }

    // open: same as pushLhsAndRhs1d through struct members
    /// solc: smtCheckerTests/array_members/push_as_lhs_struct.sol
    function pushLhsStruct() public {
        sb.b.push() = tsb.s.b.push();
        int l = sb.b[sb.b.length - 1];
        int r = tsb.s.b[tsb.s.b.length - 1];
        assert(l == r);
    }

    // open: push(x) of a memory array onto a storage array of arrays is stuck on the program text
    /// solc: smtCheckerTests/array_members/push_2d_arg_1_safe.sol
    function pushMemoryRowThenValue() public {
        uint[] memory x = new uint[](2);
        uint y = 9;
        push2dArg.push(x);
        push2dArg[0].push(y);
        assert(push2dArg[0][push2dArg[0].length - 1] == y);
    }

    // open: push() on the storage reference an internal call returns (f().push()) is stuck on the program text
    /// solc: smtCheckerTests/array_members/storage_pointer_push_1.sol
    function storagePointerPush() public {
        threeRows().push();
        assert(pointer2d[2].length > 0);
    }

    // open: the bare member access popIso.pop; as a statement is stuck on the program text
    /// solc: semanticTests/array/pop/array_pop_isolated.sol
    function popIsolated() public view {
        uint x = 2;
        popIso.pop;
        x = 3;
        assert(x == 3);
    }

    // open: push(s) of a memory struct onto a storage array of structs is stuck on the program text
    /// solc: semanticTests/array/push/array_push_struct.sol
    /// @custom:key box
    function pushStructFromMemory() public {
        require(structData.length == 0);
        PS memory s;
        s.a = 2;
        s.b = 3;
        s.c[2] = 4;
        s.d = new uint[](4);
        s.d[2] = 5;
        structData.push(s);
        uint a = structData[0].a;
        uint b = structData[0].b;
        uint c = structData[0].c[2];
        uint d = structData[0].d[2];
        assert(a == 2);
        assert(b == 3);
        assert(c == 4);
        assert(d == 5);
    }

}
