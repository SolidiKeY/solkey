// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.0;

// Source: https://etherscan.io/address/0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2#code
// The real-world/WETH9.sol port with its specification written as require and assert. Each clause
// with no Solidity form stays a @custom:key clause.
contract WETH9 {
    uint8 public decimals = 18;

    mapping(address => uint) public balanceOf;
    mapping(address => mapping(address => uint)) public allowance;

    /// @custom:key ensures net(msg.sender) == \old(net(msg.sender)) + msg.value
    /// @custom:key box
    function deposit() public payable {
        uint oldBalance = balanceOf[msg.sender];

        balanceOf[msg.sender] += msg.value;

        assert(balanceOf[msg.sender] == oldBalance + msg.value);
    }

    receive() external payable {
        deposit();
    }

    /// @custom:key ensures net(msg.sender) == \old(net(msg.sender)) - wad
    /// @custom:key box
    function withdraw(uint wad) public {
        require(wad >= 0);
        uint oldBalance = balanceOf[msg.sender];

        require(balanceOf[msg.sender] >= wad);
        balanceOf[msg.sender] -= wad;
        payable(msg.sender).transfer(wad);

        assert(balanceOf[msg.sender] == oldBalance - wad);
    }
}
