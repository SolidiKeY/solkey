// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v5.0.0/contracts/utils/Pausable.sol
// The real-world/OZPausable.sol port with its @custom:key specification removed.
contract OZPausable {
    bool private _paused;
    bool public drasticMeasureTaken;
    uint256 public count;

    constructor() {
        _paused = false;
        drasticMeasureTaken = false;
        count = 0;
    }

    modifier whenNotPaused() {
        if (_paused) {
            revert();
        }
        _;
    }

    modifier whenPaused() {
        if (!_paused) {
            revert();
        }
        _;
    }

    function paused() public view virtual returns (bool p) {
        return _paused;
    }

    function _requireNotPaused() internal view virtual {
        if (paused()) {
            revert();
        }
    }

    function _requirePaused() internal view virtual {
        if (!paused()) {
            revert();
        }
    }

    function _pause() internal virtual whenNotPaused {
        _paused = true;
    }

    function _unpause() internal virtual whenPaused {
        _paused = false;
    }

    function normalProcess() external whenNotPaused {
        count++;
    }

    function drasticMeasure() external whenPaused {
        drasticMeasureTaken = true;
    }

    function pause() external {
        _pause();
    }

    function unpause() external {
        _unpause();
    }
}
