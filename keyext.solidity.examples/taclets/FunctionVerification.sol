// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract FunctionVerification {
    uint total;

    function deposit(uint amount) public {
        require(amount > 0);
        total = amount;
        assert(total > 0);
    }
}
