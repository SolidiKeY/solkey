// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// From semantics/SolcArrayMembers.sol and semantics/open/SolcArrayMembers.sol. Each claim holds
// on a chain whose storage beyond every array length is zero, and stays open in SolKey, which
// assumes no such clean tail (README.md).
contract SolcArrayMembersTail {
    uint[][] lhs2d;
    uint[][][] lhs3d;
    uint[][] lenArr;
    uint[][] lenArr2;
    uint[] noArgs1d;
    uint[] lhsRhs1d;
    int[] compound;
    uint[] zero1d;
    uint[][] nestedP;
    uint[][] zero2d;
    uint[][] nestedToMemory;
    uint[][] delIdx2d;
    uint[][] lhsRhs2d;
    uint[][] lhsRhs2dAny;

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

    /// solc: smtCheckerTests/array_members/push_as_lhs_2d.sol
    function pushLhs2dLast() public {
        lhs2d.push().push() = 2;
        assert(lhs2d.length > 0);
        assert(lhs2d[lhs2d.length - 1].length == 1);
        assert(lhs2d[lhs2d.length - 1][lhs2d[lhs2d.length - 1].length - 1] == 2);
    }

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

    /// solc: smtCheckerTests/array_members/push_as_lhs_and_rhs_1d.sol
    function pushLhsAndRhs1d() public {
        lhsRhs1d.push() = lhsRhs1d.push();
        uint length = lhsRhs1d.length;
        assert(length >= 2);
        assert(lhsRhs1d[length - 1] == 0);
        assert(lhsRhs1d[length - 1] == lhsRhs1d[length - 2]);
    }

    /// solc: smtCheckerTests/array_members/push_as_lhs_compound_assignment.sol
    /// @custom:key box
    function pushLhsCompound() public {
        require(compound.length == 0);
        compound.push() -= 1;
        assert(compound[0] < 0);
    }

    /// solc: semanticTests/array/push/array_push_nested.sol
    /// @custom:key box
    function pushNested() public {
        require(nestedP.length == 0);
        nestedP.push();
        assert(nestedP.length == 1);
        assert(nestedP[0].length == 0);
        nestedP[0].push();
        assert(nestedP[0].length == 1);
        assert(nestedP[0][0] == 0);
    }

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

    /// solc: semanticTests/array/push/push_no_args_1d.sol
    /// @custom:key box
    function pushNoArgs1dLvalue() public {
        require(noArgs1d.length == 0);
        noArgs1d.push() = 42;
        assert(noArgs1d.length == 1);
        assert(noArgs1d[0] == 42);
        noArgs1d.push();
        assert(noArgs1d.length == 2);
        assert(noArgs1d[1] == 0);
        noArgs1d.push() = 111;
        assert(noArgs1d.length == 3);
        assert(noArgs1d[2] == 111);
    }

    /// solc: smtCheckerTests/array_members/push_zero_2d_safe.sol
    function pushZero2d() public {
        zero2d.push();
        zero2d[zero2d.length - 1].push();
        uint r = zero2d[zero2d.length - 1][0];
        assert(r == 0);
    }

    /// solc: smtCheckerTests/array_members/push_zero_safe.sol
    function pushZeroReadsZero() public {
        zero1d.push();
        uint r = zero1d[zero1d.length - 1];
        assert(r == 0);
    }

    function delIdx2dInit() internal {
        delIdx2d.push();
        delIdx2d.push();
        delIdx2d[0].push();
        delIdx2d[1].push();
    }

    /// solc: smtCheckerTests/operators/delete_array_index_2d.sol
    /// @custom:key box
    function deleteArrayIndex2d(uint bp) public {
        require(bp <= 1);
        bool b = bp == 1;
        require(delIdx2d.length == 0);
        delIdx2dInit();
        if (b)
            delete delIdx2d;
        else
            delete delIdx2d[1];
        delIdx2dInit();
        assert(delIdx2d[1][0] == 0);
        assert(delIdx2d[0][0] == 0);
    }

    /// solc: smtCheckerTests/array_members/push_as_lhs_and_rhs_2d_1.sol
    /// @custom:key box
    function pushLhsRhs2d() public {
        require(lhsRhs2d.length == 0);
        lhsRhs2d.push().push() = lhsRhs2d.push().push();
        assert(lhsRhs2d.length == 2);
        assert(lhsRhs2d[0].length == 1);
        assert(lhsRhs2d[0].length == 1);
        assert(lhsRhs2d[0][0] == 0);
        assert(lhsRhs2d[1][0] == 0);
        assert(lhsRhs2d[0][0] == lhsRhs2d[1][0]);
    }

    /// solc: smtCheckerTests/array_members/push_as_lhs_and_rhs_2d_2.sol
    function pushLhsRhs2dAnyState() public {
        lhsRhs2dAny.push().push() = lhsRhs2dAny.push().push();
        uint length = lhsRhs2dAny.length;
        assert(length >= 2);
        uint length1 = lhsRhs2dAny[length - 1].length;
        uint length2 = lhsRhs2dAny[length - 2].length;
        assert(length1 == 1);
        assert(length2 == 1);
        assert(lhsRhs2dAny[length - 1][length1 - 1] == 0);
    }
}
