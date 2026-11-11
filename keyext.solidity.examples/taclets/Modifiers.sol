// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Taclet examples: modifiers and their placeholder continuations.
// Conventions: README.md, "Writing an example".
contract Modifiers {
    uint total;
    uint localOut;

    function setTotalToOne() internal {
        total = 1;
    }

    modifier callsInternal() {
        setTotalToOne();
        _;
    }

    function modifierCallsInternalFunction() public callsInternal {
        assert(total == 1);
    }

    modifier keepsLocal(uint y) {
        uint c = y;
        _;
        localOut = c;
    }

    function twiceKeepsLocal() internal keepsLocal(2) keepsLocal(5) {}

    function modifierLocalsArePerApplication() public {
        twiceKeepsLocal();
        uint r = localOut;
        assert(r == 2);
    }

    modifier twiceContinuation() {
        _;
        _;
    }

    function repeatedContinuationBody() internal twiceContinuation returns (uint r) {
        uint v;
        v += 1;
        assert(v == 1);
        localOut += v;
        r += 1;
        if (r == 1) return r;
        r += 10;
    }

    function modifierRepeatedReturnAndLocals() public {
        localOut = 0;
        uint r = repeatedContinuationBody();
        assert(localOut == 2);
        assert(r == 12);
    }

    modifier continuationArgument(uint v) {
        assert(v == localOut);
        _;
    }

    function nextContinuationArgument() internal returns (uint) {
        localOut += 1;
        return localOut;
    }

    function reenteredArgumentBody() internal twiceContinuation
        continuationArgument(nextContinuationArgument()) returns (uint r)
    {
        uint value = localOut;
        r += value;
    }

    function modifierReentersArguments() public {
        localOut = 0;
        uint r = reenteredArgumentBody();
        assert(localOut == 2);
        assert(r == 3);
    }

    modifier conditionalContinuation(bool run) {
        if (run) {
            _;
        } else {
            return;
        }
        localOut += 10;
    }

    function conditionalContinuationBody(bool run) internal
        conditionalContinuation(run) returns (uint r)
    {
        r = 1;
        return r;
    }

    function modifierSkippedPlaceholder() public {
        localOut = 0;
        uint r = conditionalContinuationBody(false);
        assert(r == 0);
        assert(localOut == 0);
    }

    function modifierTakenPlaceholder() public {
        localOut = 0;
        uint r = conditionalContinuationBody(true);
        assert(r == 1);
        assert(localOut == 10);
    }

    modifier enclosingContinuation() {
        _;
        localOut += 1000;
    }

    modifier returningContinuation() {
        _;
        localOut += 10;
        if (localOut == 11) return;
        localOut = 100;
    }

    function nestedReturningContinuation() internal
        enclosingContinuation returningContinuation returns (uint r)
    {
        localOut = 1;
        return 7;
    }

    function modifierReturnResumesOuterSuffix() public {
        localOut = 0;
        uint r = nestedReturningContinuation();
        assert(r == 7);
        assert(localOut == 1011);
    }

    modifier loopContinuation() {
        for (uint i = 0; i < 3; ++i) {
            _;
            localOut += 10;
        }
    }

    function loopContinuationBody() internal loopContinuation returns (uint r) {
        r += 1;
        return r;
    }

    function modifierLoopBodyReturnStaysInFrame() public {
        localOut = 0;
        uint r = loopContinuationBody();
        assert(r == 3);
        assert(localOut == 30);
    }

    modifier whileContinuation() {
        uint i;
        while (i < 4) {
            ++i;
            if (i == 2) continue;
            _;
            if (i == 3) break;
        }
    }

    function whileContinuationBody() internal pure whileContinuation returns (uint r) {
        r += 1;
        return r;
    }

    function modifierWhileJumps() public pure {
        uint r = whileContinuationBody();
        assert(r == 2);
    }

    modifier doWhileContinuation() {
        uint i;
        do {
            ++i;
            if (i == 2) continue;
            _;
            if (i == 3) break;
        } while (i < 4);
    }

    function doWhileContinuationBody() internal pure doWhileContinuation returns (uint r) {
        r += 1;
        return r;
    }

    function modifierDoWhileJumps() public pure {
        uint r = doWhileContinuationBody();
        assert(r == 2);
    }

    // ── Modifiers ──

    modifier nonZero(uint v) {
        require(v > 0);
        _;
    }

    function modifierGuarded(uint v) internal pure nonZero(v) returns (uint) {
        return v;
    }

    function modifierArgument() public pure {
        uint r = modifierGuarded(3);
        assert(r == 3);
    }

    modifier countAround() {
        total = total + 1;
        _;
        total = total + 10;
    }

    function modifierAroundBody() internal countAround returns (uint r) {
        r = total;
        return r;
    }

    function modifierResumesAfterReturn() public {
        total = 0;
        uint r = modifierAroundBody();
        assert(r == 1 && total == 11);
    }

    modifier doubleAfter() {
        _;
        total = total * 2;
    }

    modifier addThreeAfter() {
        _;
        total = total + 3;
    }

    function modifierOrdered() internal doubleAfter addThreeAfter {
        total = 1;
    }

    function modifierFirstListedOutermost() public {
        modifierOrdered();
        assert(total == 8);
    }

    modifier setsFive() {
        total = 5;
        _;
    }

    function modifierOnObligation() public setsFive {
        assert(total == 5);
    }
}
