// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

/// Escrow course example (SolidityCalculus/maltaCourseSol/Escrow, v2 with the two-party
/// release flags; v1 is the subset without them), adapted to the supported fragment:
/// the state enum AwaitingDeposit/DepositPlaced/Withdrawn is a `uint` (0/1/2),
/// participants are `uint` ids, `msg.sender` is `caller`, `block.timestamp` is `timeNow`,
/// and the payment ledger is the pair of counters `senderPaid` (funds in from the sender)
/// and `receiverReceived` (funds out to the receiver) — the course invariant
/// `amountInEscrow == net(sender) + net(receiver)` becomes
/// `amountInEscrow == senderPaid - receiverReceived`.
///
/// Calculus conventions (learned on PiggyBank): a comparison may read storage on one side
/// only — bind the other side to a local first; the right side of a compound assignment is
/// bound to a local; an assert compares two bound locals, never an arithmetic expression.
contract Escrow {
    uint sender;
    uint receiver;
    uint caller;
    uint state; // 0 AwaitingDeposit, 1 DepositPlaced, 2 Withdrawn
    uint amountInEscrow;
    bool releasedBySender;
    bool releasedByReceiver;
    uint timeNow;
    uint releaseTime;
    uint delayUntilRelease;
    uint senderPaid;
    uint receiverReceived;

    // ── placeInEscrow ──

    /// @custom:key box
    function placeInEscrowHappy() public {
        uint snd = sender;
        require(caller == snd);
        require(state == 0);
        require(timeNow == 3 && delayUntilRelease == 10);
        require(senderPaid == 0);
        amountInEscrow = 5;
        senderPaid += 5;
        uint rt = timeNow + delayUntilRelease;
        releaseTime = rt;
        state = 1;
        uint s = state;
        uint a = amountInEscrow;
        uint r = releaseTime;
        assert(s == 1);
        assert(a == 5);
        assert(r == 13);
    }

    /// @custom:key box
    function placeInEscrowConservation(uint v) public {
        require(v > 0 && v <= 1000);
        uint snd = sender;
        require(caller == snd);
        require(state == 0);
        require(senderPaid == 0);
        require(receiverReceived == 0);
        amountInEscrow = v;
        senderPaid += v;
        state = 1;
        uint a = amountInEscrow;
        uint sp = senderPaid;
        uint rr = receiverReceived;
        uint expected = sp - rr;
        assert(a == expected);
    }

    // ── releaseEscrow ──

    /// @custom:key box
    function releaseBySenderSetsFlag() public {
        uint snd = sender;
        uint rcv = receiver;
        require(caller == snd);
        require(caller != rcv);
        require(!releasedBySender);
        require(!releasedByReceiver);
        if (caller == snd) {
            releasedBySender = true;
        } else if (caller == rcv) {
            releasedByReceiver = true;
        }
        bool bs = releasedBySender;
        bool br = releasedByReceiver;
        assert(bs);
        assert(!br);
    }

    /// @custom:key box
    function releaseByReceiverSetsFlag() public {
        uint snd = sender;
        uint rcv = receiver;
        require(caller == rcv);
        require(caller != snd);
        require(!releasedBySender);
        require(!releasedByReceiver);
        if (caller == snd) {
            releasedBySender = true;
        } else if (caller == rcv) {
            releasedByReceiver = true;
        }
        bool bs = releasedBySender;
        bool br = releasedByReceiver;
        assert(br);
        assert(!bs);
    }

    /// @custom:key box
    function releaseByThirdPartyNoOp() public {
        uint snd = sender;
        uint rcv = receiver;
        require(caller != snd);
        require(caller != rcv);
        require(!releasedBySender);
        require(!releasedByReceiver);
        if (caller == snd) {
            releasedBySender = true;
        } else if (caller == rcv) {
            releasedByReceiver = true;
        }
        bool bs = releasedBySender;
        bool br = releasedByReceiver;
        assert(!bs);
        assert(!br);
    }

    /// @custom:key box
    function releaseIdempotent() public {
        uint snd = sender;
        require(caller == snd);
        require(!releasedBySender);
        require(!releasedByReceiver);
        if (caller == snd) {
            releasedBySender = true;
        }
        if (caller == snd) {
            releasedBySender = true;
        }
        bool bs = releasedBySender;
        bool br = releasedByReceiver;
        assert(bs);
        assert(!br);
    }

    // ── withdrawFromEscrow ──

    /// @custom:key box
    function withdrawHappy() public {
        uint rcv = receiver;
        require(caller == rcv);
        require(state == 1);
        require(releasedBySender);
        require(releasedByReceiver);
        require(releaseTime == 10 && timeNow == 12);
        require(timeNow >= releaseTime);
        require(amountInEscrow == 5);
        require(receiverReceived == 0);
        state = 2;
        uint amt = amountInEscrow;
        receiverReceived += amt;
        amountInEscrow = 0;
        uint s = state;
        uint a = amountInEscrow;
        uint r = receiverReceived;
        assert(s == 2);
        assert(a == 0);
        assert(r == 5);
    }

    /// @custom:key box
    function withdrawConservation(uint v) public {
        require(v <= 1000);
        uint rcv = receiver;
        require(caller == rcv);
        require(state == 1);
        require(releasedBySender);
        require(releasedByReceiver);
        require(senderPaid == v);
        require(amountInEscrow == v);
        require(receiverReceived == 0);
        state = 2;
        uint amt = amountInEscrow;
        receiverReceived += amt;
        amountInEscrow = 0;
        uint sp = senderPaid;
        uint rr = receiverReceived;
        uint a = amountInEscrow;
        assert(rr == sp);
        assert(a == 0);
    }

    /// @custom:key box
    function withdrawGuardNoOpWithoutBothFlags() public {
        uint rcv = receiver;
        require(caller == rcv);
        require(state == 1);
        require(releasedBySender);
        require(!releasedByReceiver);
        require(amountInEscrow == 5);
        bool bs = releasedBySender;
        bool br = releasedByReceiver;
        if (bs && br) {
            state = 2;
            uint amt = amountInEscrow;
            receiverReceived += amt;
            amountInEscrow = 0;
        }
        uint s = state;
        uint a = amountInEscrow;
        assert(s == 1);
        assert(a == 5);
    }

    // ── lifecycle ──

    /// @custom:key box
    function fullLifecycle() public {
        uint snd = sender;
        uint rcv = receiver;
        require(snd != rcv);
        require(state == 0);
        require(senderPaid == 0);
        require(receiverReceived == 0);
        require(!releasedBySender);
        require(!releasedByReceiver);
        require(timeNow == 3 && delayUntilRelease == 10);
        amountInEscrow = 5;
        senderPaid += 5;
        uint rt = timeNow + delayUntilRelease;
        releaseTime = rt;
        state = 1;
        releasedBySender = true;
        releasedByReceiver = true;
        state = 2;
        uint amt = amountInEscrow;
        receiverReceived += amt;
        amountInEscrow = 0;
        uint sp = senderPaid;
        uint rr = receiverReceived;
        uint s = state;
        uint a = amountInEscrow;
        assert(sp == 5);
        assert(rr == 5);
        assert(s == 2);
        assert(a == 0);
    }
}
