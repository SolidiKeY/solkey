// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// From semantics/SolcArithmetic.sol. Each claim holds on a chain whose storage beyond every array length
// is zero, and stays open in SolKey, which assumes no such clean tail (README.md).
contract SolcArithmeticTail {
    uint[] pushed;

    /// solc: smtCheckerTests/operators/unary_add_array_push_1.sol
    /// @custom:key box
    function unaryAddOnPush() public {
        require(pushed.length == 0);
        ++pushed.push();
        uint len = pushed.length;
        uint r = pushed[0];
        assert(len == 1);
        assert(r == 1);
    }
}
