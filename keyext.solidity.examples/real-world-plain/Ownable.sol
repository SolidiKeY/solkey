// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

// Source: https://github.com/OpenZeppelin/openzeppelin-contracts/blob/master/contracts/access/Ownable.sol
// The real-world/Ownable.sol port with its @custom:key specification removed.
contract Ownable {
    address private _owner;

    constructor(address initialOwner) {
        if (initialOwner == address(0)) revert();
        _owner = initialOwner;
    }

    function owner() public view returns (address) {
        return _owner;
    }

    function renounceOwnership() public {
        address o = _owner;
        if (o != msg.sender) revert();
        _owner = address(0);
    }

    function transferOwnership(address newOwner) public {
        address o = _owner;
        if (o != msg.sender) revert();
        if (newOwner == address(0)) revert();
        _owner = newOwner;
    }
}
