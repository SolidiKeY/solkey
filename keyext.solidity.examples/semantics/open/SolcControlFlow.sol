// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract SolcControlFlow {
    function conditionalTuple(bool cond) internal pure returns (uint, uint) {
        return cond ? (1, 2) : (3, 4);
    }

    // open: missing-rule: a `return` of a conditional whose branches are tuples is refused as a
    // return of 1 value from a function with 2 return values
    /// solc: semanticTests/expressions/conditional_expression_tuples.sol — upstream
    /// `f(true) -> 1, 2` and `f(false) -> 3, 4`.
    /// @custom:key box
    function conditionalExpressionTuples(uint p) public pure {
        require(p <= 1);
        (uint u, uint v) = conditionalTuple(p == 1);
        if (p == 1) {
            assert(u == 1 && v == 2);
        } else {
            assert(u == 3 && v == 4);
        }
    }
}
