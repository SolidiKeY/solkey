// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract Ledger {
    uint256 balance;
    uint256 archived;

    // copies one contract field into another (assign to a field, value read from a field)
    function archive() public {
        archived = balance;
    }
}
