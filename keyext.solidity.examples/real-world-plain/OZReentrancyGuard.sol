// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v5.0.0/contracts/utils/ReentrancyGuard.sol
// The real-world/OZReentrancyGuard.sol port with its @custom:key specification removed.
contract OZReentrancyGuard {
    uint256 private constant NOT_ENTERED = 1;
    uint256 private constant ENTERED = 2;

    uint256 private _status;
    uint256 public counter;

    constructor() {
        _status = NOT_ENTERED;
        counter = 0;
    }

    modifier nonReentrant() {
        if (_status == ENTERED) {
            revert();
        }
        _status = ENTERED;
        _;
        _status = NOT_ENTERED;
    }

    function _reentrancyGuardEntered() internal view returns (bool) {
        return _status == ENTERED;
    }

    function callback() external nonReentrant {
        _count();
    }

    function countLocalRecursive(uint256 n) public nonReentrant {
        if (n > 0) {
            _count();
            countLocalRecursive(n - 1);
        }
    }

    function _count() private {
        counter += 1;
    }

    function guardedCheckEntered() public nonReentrant {
        require(_reentrancyGuardEntered());
    }

    function unguardedCheckNotEntered() public view {
        require(!_reentrancyGuardEntered());
    }
}
