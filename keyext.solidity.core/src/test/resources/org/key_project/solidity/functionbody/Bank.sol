// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract Bank {
    uint256 balance;

    // a function with a (named) return parameter that reads contract storage,
    // exercising the result-value and storage connections of function-body expansion.
    function getBalance() public view returns (uint256 b) {
        b = balance;
    }
}
