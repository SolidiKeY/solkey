// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract SolcArrayMembersOpen {
    struct B { int[] b; }
    struct T { B s; }
    struct PS { uint a; uint b; uint[3] c; uint[] d; }

    uint[] noArgs1d;
    uint[][] nested;
    uint[][] nestedFromMemory;
    uint[] dyn1;
    uint[] dyn2;
    uint[][] nestedToMemory;
    uint[][] zero2d;
    uint[][] lhs2d;
    uint[][][] lhs3d;
    uint[] lhsRhs1d;
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

    // open: push() used as an rvalue (uint y = arr.push();) is stuck on the program text
    /// solc: semanticTests/array/push/push_no_args_1d.sol
    /// @custom:key box
    function pushNoArgs1d() public {
        require(noArgs1d.length == 0);
        noArgs1d.push() = 42;
        uint y = noArgs1d.push();
        assert(y == 0);
        assert(noArgs1d.length == 2);
        assert(noArgs1d[1] == 0);
    }

    // open: push() onto an array of arrays leaves the new element's length unknown, so nested[0].length == 0 does not follow
    /// solc: semanticTests/array/push/array_push_nested.sol
    /// @custom:key box
    function pushNested() public {
        require(nested.length == 0);
        nested.push();
        assert(nested.length == 1);
        assert(nested[0].length == 0);
        nested[0].push();
        assert(nested[0].length == 1);
        assert(nested[0][0] == 0);
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

    // open: the pushed inner arrays start from an unknown length, so the pushed values land at unknown indices
    /// solc: semanticTests/array/copying/array_copy_storage_to_memory_nested.sol
    /// @custom:key box
    function copyNestedStorageToMemory() public {
        require(nestedToMemory.length == 0);
        nestedToMemory.push();
        nestedToMemory.push();
        nestedToMemory[0].push(0);
        nestedToMemory[0].push(1);
        nestedToMemory[1].push(2);
        nestedToMemory[1].push(3);
        uint[][] memory m = nestedToMemory;
        uint l = m.length;
        uint a = m[0][1];
        uint b = m[1][0];
        uint c = m[1][1];
        assert(l == 2);
        assert(a == 1);
        assert(b == 2);
        assert(c == 3);
    }

    // open: the pushed inner array starts from an unknown length, so index 0 is not the slot the second push cleared
    /// solc: smtCheckerTests/array_members/push_zero_2d_safe.sol
    function pushZero2d() public {
        zero2d.push();
        zero2d[zero2d.length - 1].push();
        uint r = zero2d[zero2d.length - 1][0];
        assert(r == 0);
    }

    // open: c.push().push() = 2 appends to an inner array of unknown length, so its length is not known to be 1
    /// solc: smtCheckerTests/array_members/push_as_lhs_2d.sol
    function pushLhs2dLast() public {
        lhs2d.push().push() = 2;
        assert(lhs2d.length > 0);
        assert(lhs2d[lhs2d.length - 1].length == 1);
        assert(lhs2d[lhs2d.length - 1][lhs2d[lhs2d.length - 1].length - 1] == 2);
    }

    // open: same as pushLhs2dLast one level deeper
    /// solc: smtCheckerTests/array_members/push_as_lhs_3d.sol
    function pushLhs3dLast() public {
        lhs3d.push().push().push() = 2;
        uint length1 = lhs3d.length;
        uint length2 = lhs3d[length1 - 1].length;
        uint length3 = lhs3d[length1 - 1][length2 - 1].length;
        assert(length1 > 0);
        assert(length2 == 1);
        assert(length3 == 1);
        assert(lhs3d[length1 - 1][length2 - 1][length3 - 1] == 2);
    }

    // open: a.push() = a.push() is desugared to a.push(a.push()), whose push() argument is stuck on the program text
    /// solc: smtCheckerTests/array_members/push_as_lhs_and_rhs_1d.sol
    function pushLhsAndRhs1d() public {
        lhsRhs1d.push() = lhsRhs1d.push();
        uint length = lhsRhs1d.length;
        assert(length >= 2);
        assert(lhsRhs1d[length - 1] == 0);
        assert(lhsRhs1d[length - 1] == lhsRhs1d[length - 2]);
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

    // open: copying a uint[9] storage array into a uint[] storage array leaves the copy's length unknown instead of 9
    /// solc: semanticTests/array/copying/array_copy_storage_storage_static_dynamic.sol
    function copyStaticToDynamic() public {
        staticData1[8] = 4;
        staticData2 = staticData1;
        uint x = staticData2.length;
        uint y = staticData2[8];
        assert(x == 9);
        assert(y == 4);
    }

    // open: copying a uint[20] into a uint[40] storage array does not clear the tail; big[30] reads small[30]
    /// solc: semanticTests/array/copying/array_copy_storage_storage_static_static.sol
    function copyStaticToLargerStatic() public {
        big[30] = 4;
        big[2] = 7;
        big[3] = 9;
        small[3] = 8;
        big = small;
        uint x = big[3];
        uint y = big[30];
        assert(x == 8);
        assert(y == 0);
    }

    // open: the pushed inner arrays start from an unknown length, so the two row lengths are unrelated
    /// solc: smtCheckerTests/array_members/length_1d_assignment_2d_storage_to_storage.sol
    /// @custom:key box
    function length2dStorageToStorage() public {
        require(lenArr.length == 0);
        require(lenArr2.length == 0);
        lenArr.push();
        lenArr.push();
        lenArr2.push();
        lenArr2.push();
        uint a = lenArr2[0].length;
        uint b = lenArr[0].length;
        assert(a == b);
        uint c = lenArr2.length;
        uint d = lenArr.length;
        assert(c == d);
    }

    // open: closes only with -m 100000 (about 25 s); the default step budget runs out before the last asserts
    /// solc: semanticTests/array/create_multiple_dynamic_arrays.sol
    function memoryMultipleDynamic() public pure {
        uint[][] memory x = new uint[][](42);
        assert(x[0].length == 0);
        x[0] = new uint[](1);
        x[0][0] = 1;
        assert(x[4].length == 0);
        x[4] = new uint[](1);
        x[4][0] = 2;
        assert(x[10].length == 0);
        x[10] = new uint[](1);
        x[10][0] = 44;
        uint[][] memory y = new uint[][](24);
        assert(y[0].length == 0);
        y[0] = new uint[](1);
        y[0][0] = 1;
        assert(y[4].length == 0);
        y[4] = new uint[](1);
        y[4][0] = 2;
        assert(y[10].length == 0);
        y[10] = new uint[](1);
        y[10][0] = 88;
        uint x0 = x[0][0];
        uint y0 = y[0][0];
        uint x4 = x[4][0];
        uint y4 = y[4][0];
        uint x10 = x[10][0];
        uint y10 = y[10][0];
        assert(x0 == y0);
        assert(x4 == y4);
        assert(x10 == 44);
        assert(y10 == 88);
    }
}
