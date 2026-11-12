// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v5.0.0/contracts/access/Ownable2Step.sol
// The real-world/OZOwnable2Step.sol port with its specification written as require and assert. The
// modifier is inlined.
contract OZOwnable2Step {
    address private _owner;
    address private _pendingOwner;

    /// @custom:key box
    constructor(address initialOwner) {
        if (initialOwner == address(0)) {
            revert();
        }
        _transferOwnership(initialOwner);

        assert(initialOwner != address(0) && _owner == initialOwner && _pendingOwner == address(0));
    }

    /// @custom:key box
    function owner() public view virtual returns (address o) {
        address oldOwner = _owner;

        o = _owner;

        assert(o == _owner && _owner == oldOwner);
    }

    function _checkOwner() internal view virtual {
        if (owner() != msg.sender) {
            revert();
        }
    }

    /// @custom:key box
    function renounceOwnership() public virtual {
        address oldOwner = _owner;

        if (_owner != msg.sender) {
            revert();
        }
        _transferOwnership(address(0));

        assert(oldOwner == msg.sender && _owner == address(0) && _pendingOwner == address(0));
    }

    /// @custom:key box
    function pendingOwner() public view virtual returns (address p) {
        address oldPendingOwner = _pendingOwner;

        p = _pendingOwner;

        assert(p == _pendingOwner && _pendingOwner == oldPendingOwner);
    }

    /// @custom:key box
    function transferOwnership(address newOwner) public virtual {
        address oldOwner = _owner;

        if (_owner != msg.sender) {
            revert();
        }
        _pendingOwner = newOwner;

        assert(oldOwner == msg.sender && _owner == oldOwner && _pendingOwner == newOwner);
    }

    function _transferOwnership(address newOwner) internal virtual {
        delete _pendingOwner;
        _owner = newOwner;
    }

    /// @custom:key box
    function acceptOwnership() public virtual {
        address oldPendingOwner = _pendingOwner;

        address sender = msg.sender;
        if (pendingOwner() != sender) {
            revert();
        }
        _transferOwnership(sender);

        assert(oldPendingOwner == msg.sender && _owner == msg.sender && _pendingOwner == address(0));
    }
}
