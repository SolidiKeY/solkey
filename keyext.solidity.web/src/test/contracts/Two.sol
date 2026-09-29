// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract First {
    function one() public pure {
        uint8 x = 1;
        assert(x == 1);
    }
}

contract Second {
    function two() public pure {
        uint8 y = 2;
        assert(y == 2);
    }

    function three() public pure {
        uint8 z = 3;
        assert(z == 3);
    }
}
