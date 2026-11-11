// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// The contracts/Counter.sol example with its @custom:key specification removed.
contract Counter {
    uint public limit = 5;
    uint public count;

    constructor(uint start) {
        count = start;
    }

    function increment() public {
        count = count + 1;
    }
}
