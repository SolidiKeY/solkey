// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v5.0.0/contracts/utils/math/Math.sol
// The real-world/OZMath.sol port with its @custom:key specification removed.
contract OZMath {
    function tryAdd(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        unchecked {
            uint256 c = a + b;
            if (c < a) return (false, 0);
            return (true, c);
        }
    }

    function trySub(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        unchecked {
            if (b > a) return (false, 0);
            return (true, a - b);
        }
    }

    function tryMul(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        unchecked {
            if (a == 0) return (true, 0);
            uint256 c = a * b;
            if (c / a != b) return (false, 0);
            return (true, c);
        }
    }

    function max(uint256 a, uint256 b) public pure returns (uint256 r) {
        return a > b ? a : b;
    }

    function min(uint256 a, uint256 b) public pure returns (uint256 r) {
        return a < b ? a : b;
    }

    function tryDiv(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        unchecked {
            if (b == 0) return (false, 0);
            return (true, a / b);
        }
    }

    function tryMod(uint256 a, uint256 b) public pure returns (bool success, uint256 r) {
        unchecked {
            if (b == 0) return (false, 0);
            return (true, a % b);
        }
    }

    function average(uint256 a, uint256 b) public pure returns (uint256 r) {
        return (a & b) + (a ^ b) / 2;
    }

    function ceilDiv(uint256 a, uint256 b) public pure returns (uint256 r) {
        if (b == 0) {
            return a / b;
        }
        return a == 0 ? 0 : (a - 1) / b + 1;
    }
}
