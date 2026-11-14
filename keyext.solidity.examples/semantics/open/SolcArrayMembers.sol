// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract SolcArrayMembers {
    uint[][] delIdx2d;
    uint[3][90][] copyLarge;
    uint[3][3][] copySmall;
    uint[][] lhsRhs2d;
    uint[][] lhsRhs2dAny;

    function delIdx2dInit() internal {
        delIdx2d.push();
        delIdx2d.push();
        delIdx2d[0].push();
        delIdx2d[1].push();
    }

    // open: no rule executes bitwise |; the empty pushes also leave small[2][2][0] unconstrained (closing twin: copyIncludingArrayCleared)
    /// solc: semanticTests/array/copying/array_copy_including_array.sol
    /// @custom:key box
    function copyIncludingArray() public {
        require(copyLarge.length == 0 && copySmall.length == 0);
        for (uint i = 0; i < 7; i++) {
            copyLarge.push();
            copySmall.push();
        }
        copyLarge[3][2][0] = 2;
        copyLarge[1] = copyLarge[3];
        copySmall[3][2][0] = 2;
        copySmall[1] = copySmall[2];
        uint r = ((
            copySmall[3][2][0] * 0x100 |
            copySmall[1][2][0]) * 0x100 |
            copyLarge[3][2][0]) * 0x100 |
            copyLarge[1][2][0];
        delete copySmall;
        delete copyLarge;
        assert(r == 0x02000202);
    }

    // open: design-limitation: arbitrary storage tail, the empty push a[0].push() keeps a stale a[0][0] that the else branch never clears
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

    // open: design-limitation: arbitrary storage tail, the empty pushes keep stale values in b[0][0] and b[1][0]
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

    // open: design-limitation: arbitrary storage tail, the empty pushes keep stale inner lengths and a stale last element
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
