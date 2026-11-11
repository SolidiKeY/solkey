// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Taclet examples: loops, unwound or with an invariant.
// Conventions: README.md, "Writing an example".
contract Loops {
    uint total;

    uint[] values;

    // ── Loops: unwound by whileUnwind after LoopLowering ──

    function whileCountsUp() public pure {
        uint i = 0;
        while (i < 3) {
            i = i + 1;
        }
        assert(i == 3);
    }

    function whileBodyDeclaresLocal() public pure {
        uint i = 0;
        uint s = 0;
        while (i < 3) {
            uint t = i * 2;
            s = s + t;
            i = i + 1;
        }
        assert(s == 6);
    }

    function whileBreakLeavesLoop() public pure {
        uint i = 0;
        while (i < 10) {
            if (i == 4) break;
            i = i + 1;
        }
        assert(i == 4);
    }

    function whileContinueSkipsRest() public pure {
        uint i = 0;
        uint s = 0;
        while (i < 5) {
            i = i + 1;
            if (i == 2) continue;
            s = s + i;
        }
        assert(s == 13);
    }

    function forContinueStillUpdates() public pure {
        uint s = 0;
        for (uint i = 0; i < 5; i++) {
            if (i == 1) {
                continue;
            }
            s = s + i;
        }
        assert(s == 9);
    }

    function forBreakKeepsCounter() public pure {
        uint i;
        for (i = 0; i < 9; i++) {
            if (i == 3) break;
        }
        assert(i == 3);
    }

    function doWhileRunsBodyFirst() public pure {
        uint i = 7;
        do {
            i = i + 1;
        } while (i < 3);
        assert(i == 8);
    }

    /// @custom:key box
    function forWritesStorageArray() public {
        require(values.length == 3);
        for (uint i = 0; i < values.length; i++) {
            values[i] = i * i;
        }
        assert(values[2] == 4);
    }

    function loopNestedReturn(uint k) internal pure returns (uint r) {
        for (uint i = 0; i < 3; i++) {
            for (uint j = 0; j < 3; j++) {
                if (i * 3 + j == k) {
                    return i * 10 + j;
                }
            }
        }
        r = 99;
    }

    function returnLeavesNestedLoops() public pure {
        uint r = loopNestedReturn(5);
        assert(r == 12);
        r = loopNestedReturn(20);
        assert(r == 99);
    }

    // ── Loop invariants: whileInvariantBox / whileInvariantDiamond ──

    /// @custom:key box
    function invariantCountsToBound(uint n) public pure {
        require(n >= 0);
        uint i = 0;
        /// @custom:key invariant i <= n
        while (i < n) {
            i = i + 1;
        }
        assert(i == n);
    }

    function invariantForCountsArrayLength() public view {
        uint c = 0;
        /// @custom:key invariant 0 <= i && i <= values.length && c == i
        /// @custom:key decreases values.length - i
        for (uint i = 0; i < values.length; i++) {
            c = c + 1;
        }
        assert(c == values.length);
    }

    function invariantClosedFormSum() public view {
        uint s = 0;
        uint i = 0;
        /// @custom:key invariant 0 <= i && i <= values.length && s == 2 * i
        /// @custom:key decreases values.length - i
        while (i < values.length) {
            s = s + 2;
            i = i + 1;
        }
        assert(s == 2 * values.length);
    }

    /// @custom:key box
    function invariantBreakLeavesInvariantRange(uint n) public pure {
        require(n > 0);
        uint i = 0;
        /// @custom:key invariant i == 0
        while (i < n) {
            i = n + 1;
            break;
        }
        assert(i == n + 1);
    }

    /// @custom:key box
    function invariantForContinueRunsUpdate(uint n) public pure {
        uint count = 0;
        /// @custom:key invariant 0 <= i && i <= n && count == i
        for (uint i = 0; i < n; ++i) {
            ++count;
            continue;
        }
        assert(count == n);
    }

    /// @custom:key box
    function invariantForBreakSkipsUpdate(uint n) public pure {
        require(n > 0);
        uint i = 0;
        /// @custom:key invariant i == 0
        for (; i < n; ++i) {
            if (i == 0) {
                i = n + 1;
                break;
            }
        }
        assert(i == n + 1);
    }

    /// @custom:key box
    function unprovableInvariantBreakPostcondition(uint n) public pure {
        require(n > 0);
        uint i = 0;
        /// @custom:key invariant i == 0
        while (i < n) {
            i = n + 1;
            break;
        }
        assert(i <= n);
    }

    /// @custom:key box
    function unprovableInvariantForContinueUpdate(uint n) public pure {
        require(n > 0);
        uint count = 0;
        /// @custom:key invariant 0 <= i && i <= n && count == i
        for (uint i = 0; i < n; ++i) {
            ++count;
            continue;
        }
        assert(count == n + 1);
    }

    /// @custom:key box
    function invariantConditionSideEffect() public pure {
        uint i = 0;
        /// @custom:key invariant i <= 3
        while (i++ < 3) {
        }
        assert(i == 4);
    }

    /// @custom:key box
    function invariantConditionStorageSideEffect() public {
        total = 0;
        /// @custom:key invariant total <= 2
        while (++total < 3) {
        }
        assert(total == 3);
    }

    /// @custom:key box
    function unprovableInvariantConditionSideEffect() public pure {
        uint i = 0;
        /// @custom:key invariant true
        while (i++ < 3) {
        }
        assert(i == 0);
    }

    /// @custom:key box
    function unprovableInvariantConditionStorageSideEffect() public {
        total = 0;
        /// @custom:key invariant true
        while (++total < 3) {
        }
        assert(total == 0);
    }

    function invariantVariantSkipsBreakIteration(uint n) public pure {
        uint i = 0;
        /// @custom:key invariant 0 <= i && i <= 10
        /// @custom:key decreases 10 - i
        while (i < 10) {
            if (i == n) break;
            i = i + 1;
        }
        assert(i <= 10);
    }

    function invariantVariantConditionSideEffect() public pure {
        uint i = 0;
        /// @custom:key invariant i <= 3
        /// @custom:key decreases 3 - i
        while (i++ < 3) {
        }
        assert(i == 4);
    }

    function invariantVariantNegativeAtExit() public pure {
        int i = 5;
        /// @custom:key invariant -1 <= i && i <= 5
        /// @custom:key decreases i
        while (i > 0) {
            i = i - 2;
        }
        assert(i <= 0);
    }

    // The condition lowers the measure and the body restores it, so the loop never ends.
    function unprovableVariantConditionLowersBodyRestores() public pure {
        uint i = 1;
        /// @custom:key invariant i == 1
        /// @custom:key decreases i
        while (--i == 0) {
            i = i + 1;
        }
        assert(false);
    }

    function invariantDiamondBreakOutsideInvariant(uint n) public pure {
        uint i = 0;
        /// @custom:key invariant i <= n && (n == 0 || i < n)
        /// @custom:key decreases n - i
        while (i < n) {
            if (i + 1 == n) {
                i = n + 1;
                break;
            }
            i = i + 1;
        }
        assert(n == 0 || i == n + 1);
    }

    function invariantDiamondForBreakOutsideInvariant(uint n) public pure {
        uint i = 0;
        /// @custom:key invariant i <= n && (n == 0 || i < n)
        /// @custom:key decreases n - i
        for (; i < n; ++i) {
            if (i + 1 == n) {
                i = n + 1;
                break;
            }
        }
        assert(n == 0 || i == n + 1);
    }

    function unprovableInvariantDiamondBreakPostcondition(uint n) public pure {
        uint i = 0;
        /// @custom:key invariant i <= n && (n == 0 || i < n)
        /// @custom:key decreases n - i
        while (i < n) {
            if (i + 1 == n) {
                i = n + 1;
                break;
            }
            i = i + 1;
        }
        assert(i <= n);
    }

    // The break is unreachable, so the loop never ends.
    function unprovableVariantUnreachableBreak(uint n) public pure {
        uint i = 0;
        /// @custom:key invariant i == 0
        /// @custom:key decreases 1
        while (i == 0) {
            if (n != n) break;
        }
        assert(false);
    }

    /// @custom:key box
    function invariantOverStorageWrites(uint n) public {
        require(n >= 0);
        total = 0;
        /// @custom:key invariant total == i && i <= n
        for (uint i = 0; i < n; i++) {
            total = total + 1;
        }
        assert(total == n);
    }

    /// @custom:key box
    function invariantQuantifiedOverPrefix() public {
        /// @custom:key invariant 0 <= i && i <= values.length
        /// @custom:key invariant \forall uint q; 0 <= q && q < i -> values[q] == 0
        for (uint i = 0; i < values.length; i++) {
            values[i] = 0;
        }
        assert(values.length == 0 || values[0] == 0);
    }
}
