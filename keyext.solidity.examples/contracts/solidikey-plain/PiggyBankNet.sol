// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Course PiggyBank1 (SolidityCalculus/maltaCourseKey/piggybank) in the supported
// fragment: modifiers are inlined requires, `now` is the timeNow state variable, `365 days` is 31536000.
// Verified per function by the piggybank-*.key problems of obligations/ against the
// course invariant
//   state != Broken -> balance == net(owner),
//   state == Broken -> net(owner) == 0.
// breakPiggyBank keeps the course order (state = Broken written before the payout
// leaves), so the invariant also holds at the transfer point
// (piggybank-breakPiggyBank-withcallback.key).
// The readMsg/payTo/payToPlus/sendTo/callTo/payOwner helpers host the net-* machinery
// starters; the invariant does not mention their paidBy/paidValue/sent fields.
contract PiggyBankNet {
    enum PiggyBankState { Unused, InUse, Broken }

    address payable owner;
    address paidBy;
    uint paidValue;
    PiggyBankState state;
    uint timeOfFirstDeposit;
    uint balance;
    uint timeNow;
    bool sent;

    function readMsg() public payable {
        paidValue = msg.value;
        paidBy = msg.sender;
    }

    /// @custom:key box
    function payTo(address payable a) public {
        a.transfer(5);
    }

    /// @custom:key box
    function payToPlus(address payable a, uint x) public {
        a.transfer(x + 2);
    }

    function sendTo(address payable a) public {
        bool ok = a.send(5);
        sent = ok;
    }

    function callTo(address payable a) public {
        (bool ok, ) = a.call{value: 5}("");
        sent = ok;
    }

    /// @custom:key box
    function payOwner() public {
        owner.transfer(5);
    }

    function addMoney() public payable {
        address own = owner;
        require(msg.sender == own);
        PiggyBankState st = state;
        require(st != PiggyBankState.Broken);
        uint b = balance;
        uint tn = timeNow;
        balance = b + msg.value;
        if (st == PiggyBankState.Unused) {
            state = PiggyBankState.InUse;
            timeOfFirstDeposit = tn;
        }
    }

    function breakPiggyBank() public {
        address own = owner;
        require(msg.sender == own);
        PiggyBankState st = state;
        require(st == PiggyBankState.InUse);
        uint dl = timeOfFirstDeposit + 31536000;
        require(timeNow >= dl);
        uint b = balance;
        state = PiggyBankState.Broken;
        owner.transfer(b);
    }
}
