// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://solidity-by-example.org/app/ether-wallet/
// The real-world/EtherWallet.sol port with its @custom:key specification removed.
contract EtherWallet {
    address payable public owner;

    constructor() {
        owner = payable(msg.sender);
    }

    receive() external payable {}

    function withdraw(uint256 _amount) external {
        address o = owner;
        require(msg.sender == o);
        payable(msg.sender).transfer(_amount);
    }
}
