// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/function-modifier/FunctionModifier.sol
// The real-world/FunctionModifier.sol port with its specification written as require and assert.
// The modifiers are inlined.
contract FunctionModifier {
    address public owner;
    uint256 public x = 10;
    bool public locked;

    /// @custom:key box
    constructor() {
        owner = msg.sender;

        assert(owner == msg.sender && x == 10 && !locked);
    }

    /// @custom:key box
    function changeOwner(address _newOwner) public {
        address oldOwner = owner;
        uint256 oldX = x;
        bool oldLocked = locked;

        require(msg.sender == owner);
        require(_newOwner != address(0));
        owner = _newOwner;

        assert(oldOwner == msg.sender && _newOwner != address(0));
        assert(owner == _newOwner && x == oldX && locked == oldLocked);
    }

    /// @custom:key box
    function decrement(uint256 i) public {
        require(i >= 0 && i <= x);
        bool oldLocked = locked;
        uint256 oldX = x;
        address oldOwner = owner;

        require(!locked);

        locked = true;
        x -= i;

        if (i > 1) {
            decrement(i - 1);
        }
        locked = false;

        assert(!oldLocked && i <= 1 && x == oldX - i && !locked && owner == oldOwner);
    }
}
