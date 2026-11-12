// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// The contracts/solidikey/PiggyBank.sol example with its specification written as require and
// assert. Each clause with no Solidity form stays a @custom:key clause.
/// @custom:key invariant state != PiggyBankState.Broken -> balance == net(owner)
/// @custom:key invariant state == PiggyBankState.Broken -> net(owner) == 0
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

    /// @custom:key ensures balance == net(owner)
    /// @custom:key box
    function addMoney() public payable {
        require(msg.sender == owner && state != PiggyBankState.Broken);
        require(state == PiggyBankState.Unused || state == PiggyBankState.InUse);

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

        assert(state == PiggyBankState.InUse);
    }

    /// @custom:key ensures net(owner) == 0
    /// @custom:key box
    function breakPiggyBank() public {
        require(msg.sender == owner && state == PiggyBankState.InUse);
        require(timeNow >= timeOfFirstDeposit + 31536000);

        address own = owner;
        require(msg.sender == own);
        require(state == PiggyBankState.InUse);
        uint dl = timeOfFirstDeposit + 31536000;
        require(timeNow >= dl);
        uint b = balance;
        state = PiggyBankState.Broken;
        owner.transfer(b);

        assert(state == PiggyBankState.Broken);
    }
}
