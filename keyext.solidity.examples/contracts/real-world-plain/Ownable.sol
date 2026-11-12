// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/access/Ownable.sol
// The real-world/Ownable.sol port with its specification written as require and assert.
contract Ownable {
    address private _owner;

    constructor(address initialOwner) {
        if (initialOwner == address(0)) revert();
        _owner = initialOwner;
    }

    function owner() public view returns (address) {
        return _owner;
    }

    /// @custom:key box
    function renounceOwnership() public {
        address oldOwner = _owner;

        address o = _owner;
        if (o != msg.sender) revert();
        _owner = address(0);

        assert(oldOwner == msg.sender && _owner == address(0));
    }

    /// @custom:key box
    function transferOwnership(address newOwner) public {
        address oldOwner = _owner;

        address o = _owner;
        if (o != msg.sender) revert();
        if (newOwner == address(0)) revert();
        _owner = newOwner;

        assert(oldOwner == msg.sender && _owner == newOwner && newOwner != address(0));
    }
}
