// SPDX-License-Identifier: AGPL-3.0-only
pragma solidity >=0.8.0;

// Source: https://github.com/transmissions11/solmate/blob/v7/src/auth/Owned.sol
// The real-world/SolmateOwned.sol port with its @custom:key specification removed.
contract SolmateOwned {
    address public owner;

    modifier onlyOwner() virtual {
        require(msg.sender == owner);

        _;
    }

    constructor(address _owner) {
        owner = _owner;
    }

    function setOwner(address newOwner) public virtual onlyOwner {
        owner = newOwner;
    }
}
