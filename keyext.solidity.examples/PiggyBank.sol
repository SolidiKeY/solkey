// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

/// PiggyBank course example (SolidityCalculus/maltaCourseSol/PiggyBank), adapted to the
/// supported fragment: the state enum Unused/InUse/Broken is a `uint` (0/1/2), participants
/// are `uint` ids (`address` equality has no discharging rule yet), `msg.sender` is the
/// `caller` state variable, `msg.value` is a parameter or literal, `block.timestamp` is
/// `timeNow`, "365 days" is `lockup`, and `owner.transfer(balance)` is the outbound
/// counter `ownerReceived`. Each function is a self-contained scenario: box-tagged requires
/// set up the pre-state, the transition statements run inline, asserts state the
/// postcondition. Modifier guards appear either as requires (happy path) or as an
/// `if (guard) { body }` wrapper asserting the state unchanged (the no-op form — "this
/// always reverts" has no assert form).
contract PiggyBank {
    uint owner;
    uint caller;
    uint state; // 0 Unused, 1 InUse, 2 Broken
    uint balance;
    uint timeOfFirstDeposit;
    uint timeNow;
    uint lockup;
    uint ownerReceived;

    // ── addMoney ──

    /// @custom:key box
    function depositFromUnusedActivates() public {
        uint ownerId = owner;
        require(caller == ownerId);
        require(state == 0);
        require(timeNow == 7);
        balance += 5;
        if (state == 0) {
            state = 1;
            timeOfFirstDeposit = timeNow;
        }
        uint s = state;
        uint t = timeOfFirstDeposit;
        assert(s == 1);
        assert(t == 7);
    }

    /// @custom:key box
    function depositFromUnusedAmount(uint v) public {
        require(v <= 1000);
        uint ownerId = owner;
        require(caller == ownerId);
        require(state == 0);
        require(balance == 0);
        balance += v;
        if (state == 0) {
            state = 1;
            timeOfFirstDeposit = timeNow;
        }
        uint r = balance;
        assert(r == v);
    }

    /// @custom:key box
    function depositInUseKeepsState() public {
        uint ownerId = owner;
        require(caller == ownerId);
        require(state == 1);
        require(timeOfFirstDeposit == 5);
        balance += 3;
        if (state == 0) {
            state = 1;
            timeOfFirstDeposit = timeNow;
        }
        uint s = state;
        uint t = timeOfFirstDeposit;
        assert(s == 1);
        assert(t == 5);
    }

    /// @custom:key box
    function depositAccumulates() public {
        uint ownerId = owner;
        require(caller == ownerId);
        require(state == 0);
        require(balance == 0);
        balance += 3;
        if (state == 0) {
            state = 1;
            timeOfFirstDeposit = timeNow;
        }
        balance += 4;
        if (state == 0) {
            state = 1;
            timeOfFirstDeposit = timeNow;
        }
        uint r = balance;
        uint s = state;
        assert(r == 7);
        assert(s == 1);
    }

    /// @custom:key box
    function depositPreservesSum(uint b0, uint v) public {
        require(b0 <= 1000 && v <= 1000);
        uint ownerId = owner;
        require(caller == ownerId);
        require(state == 1);
        balance = b0;
        balance += v;
        if (state == 0) {
            state = 1;
            timeOfFirstDeposit = timeNow;
        }
        uint r = balance;
        uint expected = b0 + v;
        assert(r == expected);
    }

    // ── breakPiggyBank ──

    /// @custom:key box
    function breakHappyPath() public {
        uint ownerId = owner;
        require(caller == ownerId);
        require(state == 1);
        require(timeOfFirstDeposit == 2 && lockup == 10 && timeNow == 20);
        require(timeNow >= timeOfFirstDeposit + lockup);
        require(balance == 8);
        require(ownerReceived == 0);
        state = 2;
        uint payout = balance;
        ownerReceived += payout;
        balance = 0;
        uint s = state;
        uint b = balance;
        uint o = ownerReceived;
        assert(s == 2);
        assert(b == 0);
        assert(o == 8);
    }

    /// @custom:key box
    function breakConservation(uint b0) public {
        require(b0 <= 1000);
        uint ownerId = owner;
        require(caller == ownerId);
        require(state == 1);
        require(balance == b0);
        require(ownerReceived == 0);
        state = 2;
        uint payout = balance;
        ownerReceived += payout;
        balance = 0;
        uint b = balance;
        uint o = ownerReceived;
        assert(o == b0);
        assert(b == 0);
    }

    // ── guards ──

    /// @custom:key box
    function depositGuardNoOpWhenBroken() public {
        uint ownerId = owner;
        require(caller == ownerId);
        require(state == 2);
        require(balance == 4);
        if (state != 2) {
            balance += 5;
            if (state == 0) {
                state = 1;
                timeOfFirstDeposit = timeNow;
            }
        }
        uint r = balance;
        uint s = state;
        assert(r == 4);
        assert(s == 2);
    }

    // ── lifecycle ──

    /// @custom:key box
    function fullLifecycle() public {
        uint ownerId = owner;
        require(caller == ownerId);
        require(state == 0);
        require(balance == 0);
        require(ownerReceived == 0);
        balance += 3;
        if (state == 0) {
            state = 1;
            timeOfFirstDeposit = timeNow;
        }
        balance += 4;
        if (state == 0) {
            state = 1;
            timeOfFirstDeposit = timeNow;
        }
        state = 2;
        uint payout = balance;
        ownerReceived += payout;
        balance = 0;
        uint o = ownerReceived;
        uint b = balance;
        uint s = state;
        assert(o == 7);
        assert(b == 0);
        assert(s == 2);
    }
}
