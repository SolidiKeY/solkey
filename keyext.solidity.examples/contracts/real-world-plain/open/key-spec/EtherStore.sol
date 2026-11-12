// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/hacks/re-entrancy/ReEntrancy.sol
// The real-world/EtherStore.sol port with its specification written as require and assert. Each
// clause with no Solidity form stays a @custom:key clause.
/// @custom:key invariant \forall address a; balances[a] == net(a)
contract EtherStore {
    mapping(address => uint256) public balances;

    /// @custom:key box
    function deposit(address a) public payable {
        require(msg.value >= 0);
        require(balances[a] >= 0);
        uint256 oldBalance = balances[msg.sender];

        balances[msg.sender] += msg.value;

        assert(balances[msg.sender] == oldBalance + msg.value);
        assert(balances[a] >= 0);
    }

    /// @custom:key ensures net(msg.sender) == \old(net(msg.sender)) - \old(balances[msg.sender])
    /// @custom:key box
    function withdraw(address a) public {
        require(balances[a] >= 0);
        uint256 oldBalance = balances[msg.sender];

        uint256 bal = balances[msg.sender];
        require(bal > 0);

        (bool sent,) = msg.sender.call{value: bal}("");
        require(sent);

        balances[msg.sender] = 0;

        assert(oldBalance > 0 && balances[msg.sender] == 0);
        assert(balances[a] >= 0);
    }
}
