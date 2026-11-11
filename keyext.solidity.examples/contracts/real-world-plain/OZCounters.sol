// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v4.9.0/contracts/utils/Counters.sol
// The real-world/OZCounters.sol port with its @custom:key specification removed.
contract OZCounters {
    struct Counter {
        uint256 _value;
    }

    Counter private _counter;

    function current() public view returns (uint256 c) {
        return _counter._value;
    }

    function increment() public {
        unchecked {
            _counter._value += 1;
        }
    }

    function decrement() public {
        uint256 value = _counter._value;
        require(value > 0);
        unchecked {
            _counter._value = value - 1;
        }
    }

    function reset() public {
        _counter._value = 0;
    }
}
