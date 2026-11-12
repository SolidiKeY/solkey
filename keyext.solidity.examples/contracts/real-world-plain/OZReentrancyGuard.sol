// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v5.0.0/contracts/utils/ReentrancyGuard.sol
// The real-world/OZReentrancyGuard.sol port with its specification written as require and assert.
// The modifier is inlined.
contract OZReentrancyGuard {
    uint256 private constant NOT_ENTERED = 1;
    uint256 private constant ENTERED = 2;

    uint256 private _status;
    uint256 public counter;

    /// @custom:key box
    constructor() {
        _status = NOT_ENTERED;
        counter = 0;

        assert(NOT_ENTERED == 1 && ENTERED == 2);
        assert(_status == 1 && counter >= 0);
    }

    function _reentrancyGuardEntered() internal view returns (bool) {
        return _status == ENTERED;
    }

    /// @custom:key box
    function callback() external {
        require(NOT_ENTERED == 1 && ENTERED == 2);
        require(_status == 1 && counter >= 0);
        uint256 oldCounter = counter;

        if (_status == ENTERED) {
            revert();
        }
        _status = ENTERED;
        _count();
        _status = NOT_ENTERED;

        assert(counter == oldCounter + 1);
        assert(NOT_ENTERED == 1 && ENTERED == 2);
        assert(_status == 1 && counter >= 0);
    }

    /// @custom:key box
    function countLocalRecursive(uint256 n) public {
        require(NOT_ENTERED == 1 && ENTERED == 2);
        require(_status == 1 && counter >= 0);
        require(n >= 0);
        uint256 oldCounter = counter;

        if (_status == ENTERED) {
            revert();
        }
        _status = ENTERED;
        if (n > 0) {
            _count();
            countLocalRecursive(n - 1);
        }
        _status = NOT_ENTERED;

        assert(n == 0 && counter == oldCounter);
        assert(NOT_ENTERED == 1 && ENTERED == 2);
        assert(_status == 1 && counter >= 0);
    }

    function _count() private {
        counter += 1;
    }

    /// @custom:key box
    function guardedCheckEntered() public {
        require(NOT_ENTERED == 1 && ENTERED == 2);
        require(_status == 1 && counter >= 0);
        uint256 oldCounter = counter;

        if (_status == ENTERED) {
            revert();
        }
        _status = ENTERED;
        require(_reentrancyGuardEntered());
        _status = NOT_ENTERED;

        assert(counter == oldCounter);
        assert(NOT_ENTERED == 1 && ENTERED == 2);
        assert(_status == 1 && counter >= 0);
    }

    /// @custom:key box
    function unguardedCheckNotEntered() public view {
        require(NOT_ENTERED == 1 && ENTERED == 2);
        require(_status == 1 && counter >= 0);
        uint256 oldCounter = counter;

        require(!_reentrancyGuardEntered());

        assert(counter == oldCounter);
        assert(NOT_ENTERED == 1 && ENTERED == 2);
        assert(_status == 1 && counter >= 0);
    }
}
