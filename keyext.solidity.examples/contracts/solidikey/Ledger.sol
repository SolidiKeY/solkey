// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// The readMsg/payTo/payToPlus/sendTo/callTo/payOwner helpers of
// ../solidikey-plain/PiggyBankNet.sol, which the net-*.key starters call, with their effect on
// the net ledger carried in @custom:key tags. A transfer to address(this) leaves the ledger
// unchanged (net-transfer-self.key), any other one debits the receiver, and an untouched
// address keeps its net. The diamond and transferSemantics:withCallback starters have no
// @custom:key form: a specified function is proved in the box modality under the default
// semantics.
contract Ledger {
    address payable owner;
    address paidBy;
    uint paidValue;
    bool sent;

    /// @custom:key ensures paidValue == msg.value && paidBy == msg.sender
    function readMsg() public payable {
        paidValue = msg.value;
        paidBy = msg.sender;
    }

    /// @custom:key ensures a != address(this) -> net(a) == \old(net(a)) - 5
    /// @custom:key ensures a == address(this) -> net(a) == \old(net(a))
    /// @custom:key ensures \forall address o; o != a -> net(o) == \old(net(o))
    function payTo(address payable a) public {
        a.transfer(5);
    }

    /// @custom:key requires x >= 0
    /// @custom:key ensures a != address(this) -> net(a) == \old(net(a)) - (x + 2)
    function payToPlus(address payable a, uint x) public {
        a.transfer(x + 2);
    }

    /// @custom:key requires a != address(this)
    /// @custom:key ensures sent -> net(a) == \old(net(a)) - 5
    /// @custom:key ensures !sent -> net(a) == \old(net(a))
    function sendTo(address payable a) public {
        bool ok = a.send(5);
        sent = ok;
    }

    /// @custom:key requires a != address(this)
    /// @custom:key ensures sent -> net(a) == \old(net(a)) - 5
    /// @custom:key ensures !sent -> net(a) == \old(net(a))
    function callTo(address payable a) public {
        (bool ok, ) = a.call{value: 5}("");
        sent = ok;
    }

    /// @custom:key ensures owner != address(this) -> net(owner) == \old(net(owner)) - 5
    function payOwner() public {
        owner.transfer(5);
    }
}
