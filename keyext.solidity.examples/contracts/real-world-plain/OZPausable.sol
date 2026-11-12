// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v5.0.0/contracts/utils/Pausable.sol
// The real-world/OZPausable.sol port with its specification written as require and assert. The
// modifiers are inlined.
contract OZPausable {
    bool private _paused;
    bool public drasticMeasureTaken;
    uint256 public count;

    /// @custom:key box
    constructor() {
        _paused = false;
        drasticMeasureTaken = false;
        count = 0;

        assert(_paused == false && drasticMeasureTaken == false && count == 0);
    }

    /// @custom:key box
    function paused() public view virtual returns (bool p) {
        p = _paused;

        assert(p == _paused);
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

    function _pause() internal virtual {
        if (_paused) {
            revert();
        }
        _paused = true;
    }

    function _unpause() internal virtual {
        if (!_paused) {
            revert();
        }
        _paused = false;
    }

    /// @custom:key box
    function normalProcess() external {
        bool oldPaused = _paused;
        uint256 oldCount = count;

        if (_paused) {
            revert();
        }
        count++;

        assert(oldPaused == false && count == oldCount + 1 && _paused == false);
    }

    /// @custom:key box
    function drasticMeasure() external {
        bool oldPaused = _paused;

        if (!_paused) {
            revert();
        }
        drasticMeasureTaken = true;

        assert(oldPaused == true && drasticMeasureTaken == true && _paused == true);
    }

    /// @custom:key box
    function pause() external {
        bool oldPaused = _paused;

        _pause();

        assert(oldPaused == false && _paused == true);
    }

    /// @custom:key box
    function unpause() external {
        bool oldPaused = _paused;

        _unpause();

        assert(oldPaused == true && _paused == false);
    }
}
