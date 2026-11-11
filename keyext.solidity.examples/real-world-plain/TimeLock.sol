// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/hacks/overflow/Overflow.sol
// The real-world/TimeLock.sol port with its @custom:key specification removed.
contract TimeLock {
    mapping(address => uint256) public balances;
    mapping(address => uint256) public lockTime;
    uint256 timeNow;

    function deposit() external payable {
        balances[msg.sender] += msg.value;
        lockTime[msg.sender] = timeNow + 1 weeks;
    }

    function increaseLockTime(uint256 _secondsToIncrease) public {
        lockTime[msg.sender] += _secondsToIncrease;
    }

    function withdraw() public {
        require(balances[msg.sender] > 0);
        require(timeNow > lockTime[msg.sender]);

        uint256 amount = balances[msg.sender];
        balances[msg.sender] = 0;

        (bool sent,) = msg.sender.call{value: amount}("");
        require(sent);
    }
}
