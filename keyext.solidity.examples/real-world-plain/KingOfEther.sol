// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/hacks/denial-of-service/PreventDenialOfService.sol
// The real-world/KingOfEther.sol port with its @custom:key specification removed.
contract KingOfEther {
    address public king;
    uint256 public balance;
    mapping(address => uint256) public balances;

    function claimThrone() external payable {
        require(msg.value > balance);

        uint256 b = balance;
        address k = king;
        balances[k] += b;

        balance = msg.value;
        king = msg.sender;
    }

    function withdraw() public {
        require(msg.sender != king);

        uint256 amount = balances[msg.sender];
        balances[msg.sender] = 0;

        (bool sent,) = msg.sender.call{value: amount}("");
        require(sent);
    }
}
