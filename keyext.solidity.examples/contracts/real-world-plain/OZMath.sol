// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v5.0.0/contracts/utils/math/Math.sol
// The real-world/OZMath.sol port with its specification written as require and assert. unchecked is
// removed, and each early return becomes an assignment to the named returns, so the asserts run
// before the function ends.
contract OZMath {
    /// @custom:key box
    function tryAdd(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        require(a >= 0 && b >= 0);

        uint256 c = a + b;
        if (c < a) {
            success = false;
            r = 0;
        } else {
            success = true;
            r = c;
        }

        assert(!success || r == a + b);
        assert(success || r == 0);
    }

    /// @custom:key box
    function trySub(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        require(a >= 0 && b >= 0);

        if (b > a) {
            success = false;
            r = 0;
        } else {
            success = true;
            r = a - b;
        }

        assert(success == (b <= a));
        assert(!success || r == a - b);
        assert(success || r == 0);
    }

    /// @custom:key box
    function tryMul(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        require(a >= 0 && b >= 0);

        if (a == 0) {
            success = true;
            r = 0;
        } else {
            uint256 c = a * b;
            if (c / a != b) {
                success = false;
                r = 0;
            } else {
                success = true;
                r = c;
            }
        }

        assert(!success || r == a * b);
        assert(success || r == 0);
    }

    /// @custom:key box
    function max(uint256 a, uint256 b) public pure returns (uint256 r) {
        r = a > b ? a : b;

        assert((r == a || r == b) && r >= a && r >= b);
    }

    /// @custom:key box
    function min(uint256 a, uint256 b) public pure returns (uint256 r) {
        r = a < b ? a : b;

        assert((r == a || r == b) && r <= a && r <= b);
    }

    /// @custom:key box
    function tryDiv(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        require(a >= 0 && b >= 0);

        if (b == 0) {
            success = false;
            r = 0;
        } else {
            success = true;
            r = a / b;
        }

        assert(success == (b != 0));
        assert(!success || r == a / b);
        assert(success || r == 0);
    }

    /// @custom:key box
    function tryMod(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        require(a >= 0 && b >= 0);

        if (b == 0) {
            success = false;
            r = 0;
        } else {
            success = true;
            r = a % b;
        }

        assert(success == (b != 0));
        assert(!success || r == a % b);
        assert(success || r == 0);
    }

    /// @custom:key box
    function average(uint256 a, uint256 b) public pure returns (uint256 r) {
        require(a >= 0 && b >= 0);

        r = (a & b) + (a ^ b) / 2;

        assert(r == (a + b) / 2);
    }

    /// @custom:key box
    function ceilDiv(uint256 a, uint256 b) public pure returns (uint256 r) {
        require(a >= 0 && b > 0);

        if (b == 0) {
            r = a / b;
        } else {
            r = a == 0 ? 0 : (a - 1) / b + 1;
        }

        assert(a != 0 || r == 0);
        assert(a == 0 || (r * b >= a && (r - 1) * b < a));
    }
}
