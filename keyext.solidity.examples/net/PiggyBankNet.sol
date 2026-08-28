// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// The `net` ledger companion contract (docs/net.md): real Solidity `msg.sender`,
// `msg.value` and `.transfer`, verified function-by-function from the `.key` problems in
// this directory, each calling `f(args)@PiggyBankNet` in its modality. It is loaded only
// via their `\programSource` - the synthesized `.sol` obligations cannot carry the
// `insertCInv` rules block or the transferSemantics option these proofs need.
contract PiggyBankNet {
    address payable owner;
    address paidBy;
    uint paidValue;
    uint balance;

    function readMsg() public payable {
        paidValue = msg.value;
        paidBy = msg.sender;
    }

    function payTo(address payable a) public {
        a.transfer(5);
    }

    function payToPlus(address payable a, uint x) public {
        a.transfer(x + 2);
    }

    function payOwner() public {
        owner.transfer(5);
    }

    function addMoney() public payable {
        balance = balance + msg.value;
    }

    function breakPiggyBank() public {
        uint b = balance;
        balance = 0;
        owner.transfer(b);
    }
}
