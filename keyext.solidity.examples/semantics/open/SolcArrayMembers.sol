// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract SolcArrayMembers {
    uint[3][90][] copyLarge;
    uint[3][3][] copySmall;

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
}
