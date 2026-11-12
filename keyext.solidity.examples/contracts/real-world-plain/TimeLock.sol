// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/hacks/overflow/Overflow.sol
// The real-world/TimeLock.sol port with its specification written as require and assert. withdraw
// is in open/key-spec/TimeLock.sol.
contract TimeLock {
    mapping(address => uint256) public balances;
    mapping(address => uint256) public lockTime;
    uint256 timeNow;

    /// @custom:key box
    function deposit(address a) external payable {
        require(msg.value >= 0);
        require(timeNow >= 0);
        require(balances[a] >= 0 && lockTime[a] >= 0);
        uint256 oldBalance = balances[msg.sender];

        balances[msg.sender] += msg.value;
        lockTime[msg.sender] = timeNow + 1 weeks;

        assert(balances[msg.sender] == oldBalance + msg.value);
        assert(lockTime[msg.sender] == timeNow + 1 weeks);
        assert(timeNow >= 0);
        assert(balances[a] >= 0 && lockTime[a] >= 0);
    }

    /// @custom:key box
    function increaseLockTime(uint256 _secondsToIncrease, address a) public {
        require(timeNow >= 0);
        require(balances[a] >= 0 && lockTime[a] >= 0);
        require(_secondsToIncrease >= 0);
        uint256 oldLockTime = lockTime[msg.sender];
        uint256 oldBalance = balances[msg.sender];

        lockTime[msg.sender] += _secondsToIncrease;

        assert(lockTime[msg.sender] == oldLockTime + _secondsToIncrease);
        assert(balances[msg.sender] == oldBalance);
        assert(timeNow >= 0);
        assert(balances[a] >= 0 && lockTime[a] >= 0);
    }
}
