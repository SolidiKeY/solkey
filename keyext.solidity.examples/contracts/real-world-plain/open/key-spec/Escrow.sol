// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// The contracts/solidikey/Escrow.sol example with its specification written as require and assert.
// Each clause with no Solidity form stays a @custom:key clause.
/// @custom:key invariant amountInEscrow == net(sender) + net(receiver)
/// @custom:key invariant state != State.AwaitingDeposit || net(sender) == 0
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

    /// @custom:key box
    constructor(address _sender, address payable _receiver, uint _delayUntilRelease) {
        require(_sender != _receiver);
        sender = _sender;
        receiver = _receiver;
        delayUntilRelease = _delayUntilRelease;
        releasedBySender = false;
        releasedByReceiver = false;
        state = State.AwaitingDeposit;

        assert(sender != receiver);
        assert(state == State.DepositPlaced || amountInEscrow == 0);
    }

    /// @custom:key ensures net(sender) == msg.value
    /// @custom:key box
    function placeInEscrow() public payable {
        require(sender != receiver);
        require(state == State.DepositPlaced || amountInEscrow == 0);
        require(msg.sender == sender && state == State.AwaitingDeposit && msg.value > 0);

        require(state == State.AwaitingDeposit);
        address snd = sender;
        require(msg.sender == snd);
        require(msg.value > 0);
        amountInEscrow = msg.value;
        uint rt = timeNow + delayUntilRelease;
        releaseTime = rt;
        state = State.DepositPlaced;

        assert(state == State.DepositPlaced);
        assert(sender != receiver);
        assert(state == State.DepositPlaced || amountInEscrow == 0);
    }

    /// @custom:key box
    function releaseEscrow() public {
        require(sender != receiver);
        require(state == State.DepositPlaced || amountInEscrow == 0);
        require(state == State.DepositPlaced);

        require(state == State.DepositPlaced);
        address snd = sender;
        address rcv = receiver;
        if (msg.sender == snd) {
            releasedBySender = true;
        }
        if (msg.sender == rcv) {
            releasedByReceiver = true;
        }

        assert(msg.sender != sender || releasedBySender == true);
        assert(msg.sender != receiver || releasedByReceiver == true);
        assert(sender != receiver);
        assert(state == State.DepositPlaced || amountInEscrow == 0);
    }

    /// @custom:key ensures net(receiver) == -net(sender)
    /// @custom:key box
    function withdrawFromEscrow() public {
        require(sender != receiver);
        require(state == State.DepositPlaced || amountInEscrow == 0);
        require(releasedByReceiver && releasedBySender && msg.sender == receiver && state == State.DepositPlaced);

        address rcv = receiver;
        require(msg.sender == rcv);
        require(state == State.DepositPlaced);
        require(timeNow >= releaseTime);
        require(releasedByReceiver == true);
        require(releasedBySender == true);
        state = State.Withdrawn;
        receiver.transfer(amountInEscrow);
        amountInEscrow = 0;

        assert(state == State.Withdrawn);
        assert(sender != receiver);
        assert(state == State.DepositPlaced || amountInEscrow == 0);
    }
}
