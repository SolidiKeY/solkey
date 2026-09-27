// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract Simple {
    uint8 counter;

    function incrementHolds() public {
        counter = 1;
        counter = counter + 1;
        assert(counter == 2);
    }

    function arithmeticHolds() public pure {
        uint8 a = 3;
        uint8 b = 4;
        assert(a + b == 7);
    }

    function wrongAssertFails() public pure {
        uint8 x = 1;
        assert(x == 2);
    }
}
