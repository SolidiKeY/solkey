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

    function conditionalAssigns(bool cond, uint v) internal pure returns (uint a, uint b) {
        cond ? a = v : b = v;
    }

    // open: missing-rule: no rule executes a conditional expression statement whose branches
    // are assignments, `cond ? a = v : b = v;`
    /// solc: semanticTests/expressions/conditional_expression_with_return_values.sol — upstream
    /// `f(true, 20) -> 20, 0` and `f(false, 20) -> 0, 20`: `cond ? a = v : b = v;` assigns
    /// only in the chosen branch.
    /// @custom:key box
    function conditionalExpressionAssigns(uint p) public pure {
        require(p <= 1);
        (uint a, uint b) = conditionalAssigns(p == 1, 20);
        if (p == 1) {
            assert(a == 20 && b == 0);
        } else {
            assert(a == 0 && b == 20);
        }
    }
}
