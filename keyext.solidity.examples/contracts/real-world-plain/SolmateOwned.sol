// SPDX-License-Identifier: AGPL-3.0-only
pragma solidity >=0.8.0;

// Source: https://github.com/transmissions11/solmate/blob/v7/src/auth/Owned.sol
// The real-world/SolmateOwned.sol port with its specification written as require and assert. The
// modifier is inlined.
contract SolmateOwned {
    address public owner;

    /// @custom:key box
    constructor(address _owner) {
        owner = _owner;

        assert(owner == _owner);
    }

    /// @custom:key box
    function setOwner(address newOwner) public virtual {
        address oldOwner = owner;

        require(msg.sender == owner);

        owner = newOwner;

        assert(oldOwner == msg.sender && owner == newOwner);
    }
}
