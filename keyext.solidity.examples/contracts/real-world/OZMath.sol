// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v5.0.0/contracts/utils/math/Math.sol
// Changes: the library becomes a contract and the ported functions become public with named
// returns; comments dropped. mulDiv, sqrt, log* and the Rounding variants are not ported
// (assembly, bitwise operators). Checked arithmetic is not modelled, so uint256 parameters
// get requires >= 0, and an overflowing tryAdd/tryMul is only specified as "success implies the
// exact result".
contract OZMath {
    /// @custom:key requires a >= 0 && b >= 0
    /// @custom:key ensures success -> r == a + b
    /// @custom:key ensures !success -> r == 0
    function tryAdd(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        unchecked {
            uint256 c = a + b;
            if (c < a) return (false, 0);
            return (true, c);
        }
    }

    /// @custom:key requires a >= 0 && b >= 0
    /// @custom:key ensures success <-> b <= a
    /// @custom:key ensures success -> r == a - b
    /// @custom:key ensures !success -> r == 0
    function trySub(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        unchecked {
            if (b > a) return (false, 0);
            return (true, a - b);
        }
    }

    /// @custom:key requires a >= 0 && b >= 0
    /// @custom:key ensures success -> r == a * b
    /// @custom:key ensures !success -> r == 0
    function tryMul(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        unchecked {
            if (a == 0) return (true, 0);
            uint256 c = a * b;
            if (c / a != b) return (false, 0);
            return (true, c);
        }
    }

    /// @custom:key ensures (r == a || r == b) && r >= a && r >= b
    function max(uint256 a, uint256 b) public pure returns (uint256 r) {
        return a > b ? a : b;
    }

    /// @custom:key ensures (r == a || r == b) && r <= a && r <= b
    function min(uint256 a, uint256 b) public pure returns (uint256 r) {
        return a < b ? a : b;
    }

    /// @custom:key requires a >= 0 && b >= 0
    /// @custom:key ensures success <-> b != 0
    /// @custom:key ensures success -> r == a / b
    /// @custom:key ensures !success -> r == 0
    function tryDiv(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        unchecked {
            if (b == 0) return (false, 0);
            return (true, a / b);
        }
    }

    /// @custom:key requires a >= 0 && b >= 0
    /// @custom:key ensures success <-> b != 0
    /// @custom:key ensures success -> r == a % b
    /// @custom:key ensures !success -> r == 0
    function tryMod(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        unchecked {
            if (b == 0) return (false, 0);
            return (true, a % b);
        }
    }

    /// @custom:key requires a >= 0 && b >= 0
    /// @custom:key ensures r == (a + b) / 2
    function average(uint256 a, uint256 b) public pure returns (uint256 r) {
        return (a & b) + (a ^ b) / 2;
    }

    /// @custom:key requires a >= 0 && b > 0
    /// @custom:key ensures a == 0 -> r == 0
    /// @custom:key ensures a > 0 -> r * b >= a && (r - 1) * b < a
    function ceilDiv(uint256 a, uint256 b) public pure returns (uint256 r) {
        if (b == 0) {
            return a / b;
        }
        return a == 0 ? 0 : (a - 1) / b + 1;
    }
}
