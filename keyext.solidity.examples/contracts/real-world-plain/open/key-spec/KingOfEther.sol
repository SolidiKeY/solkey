// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/hacks/denial-of-service/PreventDenialOfService.sol
// The real-world/KingOfEther.sol port with its specification written as require and assert. Each
// clause with no Solidity form stays a @custom:key clause.
contract KingOfEther {
    address public king;
    uint256 public balance;
    mapping(address => uint256) public balances;

    /// @custom:key ensures net(msg.sender) == \old(net(msg.sender)) + msg.value
    /// @custom:key box
    function claimThrone(address a) external payable {
        require(balance >= 0);
        require(balances[a] >= 0 && balances[king] >= 0);
        uint256 oldBalance = balance;
        address oldKing = king;
        uint256 oldKingBalance = balances[king];
        uint256 oldA = balances[a];

        require(msg.value > balance);

        uint256 b = balance;
        address k = king;
        balances[k] += b;

        balance = msg.value;
        king = msg.sender;

        assert(msg.value > oldBalance && king == msg.sender && balance == msg.value);
        assert(balances[oldKing] == oldKingBalance + oldBalance);
        assert(a == oldKing || balances[a] == oldA);
        assert(balance >= 0);
        assert(balances[a] >= 0);
    }

    /// @custom:key ensures net(msg.sender) == \old(net(msg.sender)) - \old(balances[msg.sender])
    /// @custom:key box
    function withdraw(address a) public {
        require(balance >= 0);
        require(balances[a] >= 0);
        address oldKing = king;
        uint256 oldBalance = balance;

        require(msg.sender != king);

        uint256 amount = balances[msg.sender];
        balances[msg.sender] = 0;

        (bool sent,) = msg.sender.call{value: amount}("");
        require(sent);

        assert(msg.sender != king && king == oldKing && balance == oldBalance);
        assert(balances[msg.sender] == 0);
        assert(balance >= 0);
        assert(balances[a] >= 0);
    }
}
