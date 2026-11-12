// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://solidity-by-example.org/app/ether-wallet/
// The real-world/EtherWallet.sol port with its specification written as require and assert. Each
// clause with no Solidity form stays a @custom:key clause.
contract EtherWallet {
    address payable public owner;

    constructor() {
        owner = payable(msg.sender);
    }

    receive() external payable {}

    /// @custom:key ensures net(owner) == \old(net(owner)) - _amount
    /// @custom:key box
    function withdraw(uint256 _amount) external {
        require(_amount >= 0);
        address oldOwner = owner;

        address o = owner;
        require(msg.sender == o);
        payable(msg.sender).transfer(_amount);

        assert(msg.sender == owner && owner == oldOwner);
    }
}
