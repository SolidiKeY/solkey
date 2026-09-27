// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// SolKey proves that every assert in a function holds, for all inputs and
// all storage states. Edit the code and press Verify (Ctrl+Enter).
contract Starter {
    uint8 counter;

    function setThenIncrement() public {
        counter = 1;
        counter = counter + 1;
        assert(counter == 2);
    }

    function localArithmetic() public pure {
        uint8 a = 3;
        uint8 b = 4;
        assert(a + b == 7);
    }

    function branches(bool flag) public pure {
        uint8 x = 0;
        if (flag) {
            x = 1;
        } else {
            x = 2;
        }
        assert(x > 0);
    }

    function wrongClaim() public pure {
        uint8 x = 1;
        assert(x == 2);
    }
}
