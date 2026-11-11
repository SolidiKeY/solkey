// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// The contracts/PiggyBank.sol example with its @custom:key specification removed.
contract PiggyBank {
    enum PiggyBankState { Unused, InUse, Broken }

    address payable public owner;
    PiggyBankState public state;
    uint public timeOfFirstDeposit;
    uint public balance;
    uint timeNow;

    constructor(address payable _owner) {
        owner = _owner;
        state = PiggyBankState.Unused;
        balance = 0;
    }

    function addMoney() public payable {
        address own = owner;
        require(msg.sender == own);
        require(state != PiggyBankState.Broken);
        uint b = balance;
        uint tn = timeNow;
        balance = b + msg.value;
        if (state == PiggyBankState.Unused) {
            state = PiggyBankState.InUse;
            timeOfFirstDeposit = tn;
        }
    }

    function breakPiggyBank() public {
        address own = owner;
        require(msg.sender == own);
        require(state == PiggyBankState.InUse);
        uint dl = timeOfFirstDeposit + 31536000;
        require(timeNow >= dl);
        uint b = balance;
        state = PiggyBankState.Broken;
        owner.transfer(b);
    }
}
