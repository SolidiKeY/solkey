// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v5.0.0/contracts/access/Ownable2Step.sol
// The real-world/OZOwnable2Step.sol port with its @custom:key specification removed.
contract OZOwnable2Step {
    address private _owner;
    address private _pendingOwner;

    constructor(address initialOwner) {
        if (initialOwner == address(0)) {
            revert();
        }
        _transferOwnership(initialOwner);
    }

    modifier onlyOwner() {
        if (_owner != msg.sender) {
            revert();
        }
        _;
    }

    function owner() public view virtual returns (address o) {
        return _owner;
    }

    function _checkOwner() internal view virtual {
        if (owner() != msg.sender) {
            revert();
        }
    }

    function renounceOwnership() public virtual onlyOwner {
        _transferOwnership(address(0));
    }

    function pendingOwner() public view virtual returns (address p) {
        return _pendingOwner;
    }

    function transferOwnership(address newOwner) public virtual onlyOwner {
        _pendingOwner = newOwner;
    }

    function _transferOwnership(address newOwner) internal virtual {
        delete _pendingOwner;
        _owner = newOwner;
    }

    function acceptOwnership() public virtual {
        address sender = msg.sender;
        if (pendingOwner() != sender) {
            revert();
        }
        _transferOwnership(sender);
    }
}
