// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

// Source: https://github.com/Cyfrin/solidity-by-example.github.io/blob/5bcdca0239409d7336a07b66a6fca8d0bcc710e6/contracts/src/error/Account.sol
// The real-world/Account.sol port with its specification written as require and assert.
contract Account {
    uint256 public balance;
    uint256 public constant MAX_UINT = 2 ** 256 - 1;

    /// @custom:key box
    function deposit(uint256 _amount) public {
        require(balance >= 0);
        require(_amount >= 0);
        uint256 oldBalanceAtEntry = balance;

        uint256 oldBalance = balance;
        uint256 newBalance = balance + _amount;

        require(newBalance >= oldBalance);

        balance = newBalance;

        assert(balance >= oldBalance);

        assert(balance == oldBalanceAtEntry + _amount);
        assert(balance >= 0);
    }

    /// @custom:key box
    function withdraw(uint256 _amount) public {
        require(balance >= 0);
        require(_amount >= 0);
        uint256 oldBalanceAtEntry = balance;

        uint256 oldBalance = balance;

        require(balance >= _amount);

        if (balance < _amount) {
            revert();
        }

        balance -= _amount;

        assert(balance <= oldBalance);

        assert(oldBalanceAtEntry >= _amount && balance == oldBalanceAtEntry - _amount);
        assert(balance >= 0);
    }
}
