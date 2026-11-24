// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Course escrow-v2 (SolidityCalculus/maltaCourseKey/escrow) in the supported fragment:
// modifiers are inlined requires, `now` is the timeNow state variable. Verified per
// function by the escrow-*.key problems of obligations/ against the course invariant
//   sender != receiver,
//   amountInEscrow == net(sender) + net(receiver),
//   state == AwaitingDeposit -> net(sender) == 0,
//   state != DepositPlaced   -> amountInEscrow == 0.
contract EscrowNet {
    enum State { AwaitingDeposit, DepositPlaced, Withdrawn }

    address sender;
    address payable receiver;
    uint delayUntilRelease;
    uint releaseTime;
    uint amountInEscrow;
    bool releasedBySender;
    bool releasedByReceiver;
    State state;
    uint timeNow;

    function placeInEscrow() public payable {
        address snd = sender;
        require(msg.sender == snd);
        require(state == State.AwaitingDeposit);
        require(msg.value > 0);
        amountInEscrow = msg.value;
        uint rt = timeNow + delayUntilRelease;
        releaseTime = rt;
        state = State.DepositPlaced;
    }

    function releaseEscrow() public {
        require(state == State.DepositPlaced);
        address snd = sender;
        address rcv = receiver;
        if (msg.sender == snd) {
            releasedBySender = true;
        }
        if (msg.sender == rcv) {
            releasedByReceiver = true;
        }
    }

    function withdrawFromEscrow() public {
        address rcv = receiver;
        require(msg.sender == rcv);
        require(state == State.DepositPlaced);
        require(timeNow >= releaseTime);
        require(releasedByReceiver);
        require(releasedBySender);
        state = State.Withdrawn;
        receiver.transfer(amountInEscrow);
        amountInEscrow = 0;
    }
}
