// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v4.9.0/contracts/utils/Counters.sol
// The real-world/OZCounters.sol port with its specification written as require and assert.
// unchecked is removed.
contract OZCounters {
    struct Counter {
        uint256 _value;
    }

    Counter private _counter;

    /// @custom:key box
    function current() public view returns (uint256 c) {
        require(_counter._value >= 0);
        uint256 oldValue = _counter._value;

        c = _counter._value;

        assert(c == _counter._value && _counter._value == oldValue);
        assert(_counter._value >= 0);
    }

    /// @custom:key box
    function increment() public {
        require(_counter._value >= 0);
        require(_counter._value < 115792089237316195423570985008687907853269984665640564039457584007913129639935);
        uint256 oldValue = _counter._value;

        _counter._value += 1;

        assert(_counter._value == oldValue + 1);
        assert(_counter._value >= 0);
    }

    /// @custom:key box
    function decrement() public {
        require(_counter._value >= 0);
        uint256 oldValue = _counter._value;

        uint256 value = _counter._value;
        require(value > 0);
        _counter._value = value - 1;

        assert(oldValue > 0 && _counter._value == oldValue - 1);
        assert(_counter._value >= 0);
    }

    /// @custom:key box
    function reset() public {
        require(_counter._value >= 0);

        _counter._value = 0;

        assert(_counter._value == 0);
        assert(_counter._value >= 0);
    }
}
