// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// The contracts/Escrow.sol example with its @custom:key specification removed.
contract Escrow {
    enum State { AwaitingDeposit, DepositPlaced, Withdrawn }

    address public sender;
    address payable public receiver;
    uint public delayUntilRelease;
    uint public releaseTime;
    uint public amountInEscrow;
    bool public releasedBySender;
    bool public releasedByReceiver;
    State public state;
    uint timeNow;

    constructor(address _sender, address payable _receiver, uint _delayUntilRelease) {
        require(_sender != _receiver);
        sender = _sender;
        receiver = _receiver;
        delayUntilRelease = _delayUntilRelease;
        releasedBySender = false;
        releasedByReceiver = false;
        state = State.AwaitingDeposit;
    }

    function placeInEscrow() public payable {
        require(state == State.AwaitingDeposit);
        address snd = sender;
        require(msg.sender == snd);
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
        require(releasedByReceiver == true);
        require(releasedBySender == true);
        state = State.Withdrawn;
        receiver.transfer(amountInEscrow);
        amountInEscrow = 0;
    }
}
