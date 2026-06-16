// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract C {
    uint256 f;

    // assigns the first parameter to field f and returns the constant 3
    function expFctBdy(uint256 x, uint256 y) public returns (uint256 r) {
        f = x;
        r = 3;
    }
}
