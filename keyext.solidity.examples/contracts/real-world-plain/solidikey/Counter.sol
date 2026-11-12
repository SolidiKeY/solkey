// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// The contracts/solidikey/Counter.sol example with its specification written as require and assert.
contract Counter {
    uint public limit = 5;
    uint public count;

    /// @custom:key box
    constructor(uint start) {
        count = start;

        assert(count == start);
        assert(limit == 5);
    }

    /// @custom:key box
    function increment() public {
        require(limit == 5);
        uint oldCount = count;

        count = count + 1;

        assert(count == oldCount + 1);
        assert(limit == 5);
    }
}
