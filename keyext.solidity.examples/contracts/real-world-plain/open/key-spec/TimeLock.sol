// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/hacks/overflow/Overflow.sol
// The real-world/TimeLock.sol port with its specification written as require and assert. Each
// clause with no Solidity form stays a @custom:key clause.
contract TimeLock {
    mapping(address => uint256) public balances;
    mapping(address => uint256) public lockTime;
    uint256 timeNow;

    /// @custom:key ensures net(msg.sender) == \old(net(msg.sender)) - \old(balances[msg.sender])
    /// @custom:key box
    function withdraw(address a) public {
        require(timeNow >= 0);
        require(balances[a] >= 0 && lockTime[a] >= 0);
        uint256 oldBalance = balances[msg.sender];
        uint256 oldLockTime = lockTime[msg.sender];

        require(balances[msg.sender] > 0);
        require(timeNow > lockTime[msg.sender]);

        uint256 amount = balances[msg.sender];
        balances[msg.sender] = 0;

        (bool sent,) = msg.sender.call{value: amount}("");
        require(sent);

        assert(oldBalance > 0 && timeNow > oldLockTime && balances[msg.sender] == 0);
        assert(lockTime[msg.sender] == oldLockTime);
        assert(timeNow >= 0);
        assert(balances[a] >= 0 && lockTime[a] >= 0);
    }
}
