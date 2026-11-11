// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.0;

// Source: https://etherscan.io/address/0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2#code
// The real-world/WETH9.sol port with its @custom:key specification removed.
contract WETH9 {
    uint8 public decimals = 18;

    mapping(address => uint) public balanceOf;
    mapping(address => mapping(address => uint)) public allowance;

    function deposit() public payable {
        balanceOf[msg.sender] += msg.value;
    }

    receive() external payable {
        deposit();
    }

    function withdraw(uint wad) public {
        require(balanceOf[msg.sender] >= wad);
        balanceOf[msg.sender] -= wad;
        payable(msg.sender).transfer(wad);
    }

    function approve(address guy, uint wad) public returns (bool ok) {
        allowance[msg.sender][guy] = wad;
        return true;
    }

    // 115792089237316195423570985008687907853269984665640564039457584007913129639935 is
    // 2^256 - 1, the largest uint256: the original's uint(-1), type(uint).max in 0.8, which is
    // not supported yet. An allowance of that value is unlimited and never decreases.
    function transferFrom(address src, address dst, uint wad) public returns (bool ok) {
        require(balanceOf[src] >= wad);
        if (src != msg.sender && allowance[src][msg.sender] != 115792089237316195423570985008687907853269984665640564039457584007913129639935) {
            require(allowance[src][msg.sender] >= wad);
            allowance[src][msg.sender] -= wad;
        }
        balanceOf[src] -= wad;
        balanceOf[dst] += wad;
        return true;
    }

    function transfer(address dst, uint wad) public returns (bool) {
        return transferFrom(msg.sender, dst, wad);
    }
}
