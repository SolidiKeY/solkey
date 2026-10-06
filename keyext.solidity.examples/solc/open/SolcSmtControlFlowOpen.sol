// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract SolcSmtControlFlowOpen {
    uint counter;
    bool flag;

    modifier check() {
        require(counter == 0);
        _;
        assert(counter == 0);
    }

    modifier incM() {
        if (counter == 0) {
            return;
        }
        counter = counter + 1;
        _;
    }

    modifier postinc() {
        if (counter == 0) {
            return;
        }
        _;
        counter = counter + 1;
    }

    function resetIfOverflow() internal postinc {
        if (counter < 10)
            return;
        counter = 0;
    }

    function gAddr() internal pure returns (address) {
        address a;
        a = address(0);
        return a;
    }

    function hAddr() internal pure returns (address) {
        address a;
        return a;
    }

    function readValue() internal view returns (uint) {
        return msg.value;
    }

    // open: `address a;` in hAddr starts unconstrained instead of address(0)
    /// solc: smtCheckerTests/control_flow/function_call_inside_branch_4.sol
    function functionCallInsideBranch4() public pure {
        if (true) {
            address a = gAddr();
            assert(a == address(0));
        }
        if (true) {
            address a = hAddr();
            assert(a == address(0));
        }
    }

    // open: an uninitialised `uint x;` starts unconstrained instead of 0
    /// solc: smtCheckerTests/control_flow/side_effects_inside_if_1.sol
    function sideEffectsInsideIf1() public pure {
        uint x;
        if (++x < 3) {}
        assert(x == 1);
    }

    // open: an uninitialised `uint x;` starts unconstrained instead of 0
    /// solc: smtCheckerTests/control_flow/side_effects_inside_ternary_1.sol
    function sideEffectsInsideTernary1() public pure {
        uint x;
        uint y = (++x < 3) ? ++x : --x;
        assert(y == x);
        assert(x == 2);
    }

    // open: an uninitialised `uint y;` starts unconstrained instead of 0, so the invariant fails on entry
    /// solc: smtCheckerTests/invariants/loop_basic.sol
    /// @custom:key box
    function loopBasic(uint x) public pure {
        require(x > 0);
        uint y;
        /// @custom:key invariant 0 <= y && y <= x
        while (y < x) ++y;
        assert(y == x);
    }

    // open: an uninitialised `uint y;` starts unconstrained instead of 0, so the trip count is unknown
    /// solc: smtCheckerTests/invariants/loop_nested.sol
    function loopNested() public pure {
        uint x = 10;
        uint y;
        while (y < x) {
            ++y;
            x = 0;
            while (x < 10) ++x;
            assert(x == 10);
        }
    }

    // open: an uninitialised `uint y;` starts unconstrained instead of 0, so the trip count is unknown
    /// solc: smtCheckerTests/invariants/loop_nested_for.sol
    function loopNestedFor() public pure {
        uint x;
        uint y;
        for (x = 10; y < x; ++y) {
            for (x = 0; x < 10; ++x) {}
            assert(x == 10);
        }
    }

    // open: an assignment used as an operand, `b = (flag = false)`, is stuck on the program text
    /// solc: smtCheckerTests/control_flow/short_circuit_and_touched.sol
    function shortCircuitAndTouched() public {
        bool c1 = (flag = false) && (flag == true);
        assert(!c1);
        bool c2 = (flag == false) && (flag = true);
        assert(c2);
        bool c3 = (flag = false) && (flag = true);
        assert(!c3);
        bool c4 = (flag == false) && (flag == true);
        assert(!c4);
        bool c5 = (flag = true) && flag;
        assert(c5);
    }

    // open: an assignment used as an operand, `b = (flag = true)`, is stuck on the program text
    /// solc: smtCheckerTests/control_flow/short_circuit_or_touched.sol
    function shortCircuitOrTouched() public {
        bool c1 = (flag = true) || (flag == false);
        assert(c1);
        bool c2 = (flag == true) || (flag = false);
        assert(c2);
        bool c3 = (flag = true) || (flag = false);
        assert(c3);
        bool c4 = (flag == true) || (flag == false);
        assert(c4);
        bool c5 = (flag = false) || flag;
        assert(!c5);
    }

    // open: a uint parameter is not known to be non-negative, so the diamond leaves leq(x, -1) open
    /// solc: smtCheckerTests/bmc_coverage/assert.sol
    function bmcRequireAlwaysTrue(uint x) public pure {
        require(x >= 0);
    }

    // open: a non-payable function does not fix msg.value to 0
    /// solc: smtCheckerTests/bmc_coverage/msg_value_4.sol
    function msgValueZeroWhenNonPayable() public view {
        uint v = readValue();
        assert(v == 0);
    }

    // open: msg.value is not known to be non-negative
    /// solc: smtCheckerTests/bmc_coverage/range_check.sol
    function msgValueNonNegative() public payable {
        uint v = msg.value;
        assert(v >= 0);
    }

    // open: a modifier containing `return` is not inlined
    /// solc: smtCheckerTests/control_flow/branches_with_return/branches_in_modifiers.sol
    /// @custom:key box
    function branchesInModifiers() public check incM {
    }

    // open: a modifier containing `return` is not inlined
    /// solc: smtCheckerTests/control_flow/branches_with_return/branches_in_modifiers_2.sol
    function branchesInModifiers2() public {
        if (counter == 0) {
            resetIfOverflow();
            assert(counter == 0);
            return;
        }
        if (counter < 10) {
            resetIfOverflow();
            return;
        }
        resetIfOverflow();
        assert(counter == 1);
    }
}
