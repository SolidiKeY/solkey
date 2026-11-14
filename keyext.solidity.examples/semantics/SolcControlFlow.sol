// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

/// Ports of the control-flow tests of the solc compiler test suite:
/// `semanticTests/statements/`, `semanticTests/various/swap_in_storage_overwrite.sol` and
/// `semanticTests/expressions/conditional_expression_storage_memory_*.sol`; tuple assignment
/// and short-circuit tests of `semanticTests/viaYul/`, and the tuple and conditional tests of
/// `smtCheckerTests/types/` and `smtCheckerTests/operators/`.
///
/// A parameter must be integer-typed for an obligation to be generated, so upstream's
/// `f(bool cond)` becomes `f(uint cond)` with the flag derived in the body.
contract SolcControlFlow {
    struct Pair { uint a; uint b; }

    Pair sx;
    Pair sy;
    Pair target;
    uint[] values;
    uint ex;
    uint ey;
    uint lx;
    uint[] ta;
    uint ca;
    bool cb;

    /// solc: statements/do_while_loop_continue.sol — `do { … } while (false)` runs its body
    /// exactly once: the guard `i > 0` is false on the only iteration, so the body increments
    /// `i`, and the `continue` it skips would have jumped to the false condition.
    function doWhileFalseRunsBodyOnce() public pure {
        uint i = 0;
        uint r = 0;
        do {
            if (i > 0) continue;
            i = i + 1;
        } while (false);
        r = 42;
        assert(i == 1);
        assert(r == 42);
    }

    /// solc: array/array_storage_index_zeroed_test.sol — the upstream
    /// `for (uint i = 0; i < len; i++) storageArray[i] = i + 1;` at length 3.
    /// @custom:key box
    function forLoopOverArray() public {
        require(values.length == 3);
        uint i;
        for (i = 0; i < values.length; i++) values[i] = i + 1;
        assert(i == 3);
        assert(values[0] == 1);
        assert(values[1] == 2);
        assert(values[2] == 3);
    }

    /// solc: various/swap_in_storage_overwrite.sol — `(x, y) = (y, x)` does not swap. solc
    /// assigns the tuple components with no temporary, so the pair runs as `y = x; x = y;`
    /// and both structs end up holding x's original value. The tuple itself draws solc's
    /// "two copies to storage" warning, which `SolcWrapperTest` does not tolerate, so the port
    /// is that desugaring.
    function sequentialAssignmentDoesNotSwap() public {
        sx.a = 1;
        sx.b = 2;
        sy.a = 3;
        sy.b = 4;
        sy = sx;
        sx = sy;
        assert(sx.a == 1);
        assert(sx.b == 2);
        assert(sy.a == 1);
        assert(sy.b == 2);
    }

    /// solc: various/swap_in_storage_overwrite.sol — the corrected version, with the temporary
    /// the upstream test shows is necessary.
    function swapViaTemporary() public {
        sx.a = 1;
        sx.b = 2;
        sy.a = 3;
        sy.b = 4;
        Pair memory tmp = sx;
        sx = sy;
        sy = tmp;
        assert(sx.a == 3);
        assert(sx.b == 4);
        assert(sy.a == 1);
        assert(sy.b == 2);
    }

    /// solc: expressions/conditional_expression_storage_memory_1.sol — `data1 = cond ? x : y;`
    /// with two memory sources, `cond` true.
    /// @custom:key box
    function ternarySelectsFirstMemorySource(uint cond) public {
        require(cond == 1);
        bool c = cond == 1;
        Pair memory mx;
        mx.a = 11;
        Pair memory my;
        my.a = 22;
        target = c ? mx : my;
        assert(target.a == 11);
    }

    /// solc: expressions/conditional_expression_storage_memory_2.sol — the upstream
    /// `x = cond ? y : data1;`: the selection goes into a *memory* target, which closes.
    /// @custom:key box
    function ternaryIntoMemoryTarget(uint cond) public pure {
        require(cond == 1);
        bool c = cond == 1;
        Pair memory mx;
        mx.a = 11;
        Pair memory my;
        my.a = 22;
        Pair memory r = c ? mx : my;
        assert(r.a == 11);
    }

    /// solc: expressions/conditional_expression_storage_memory_1.sol — the same selection into
    /// a storage target, written as `if`/`else` instead of `?:`.
    /// @custom:key box
    function ifElseSelectsMemorySource(uint cond) public {
        require(cond == 0);
        bool c = cond == 1;
        Pair memory mx;
        mx.a = 11;
        Pair memory my;
        my.a = 22;
        if (c) {
            target = mx;
        } else {
            target = my;
        }
        assert(target.a == 22);
    }

    /// solc: expressions/conditional_expression_storage_memory_1.sol — the branch selection on
    /// its own, over plain values rather than references.
    /// @custom:key box
    function ternarySelectsValueByFlag(uint cond) public pure {
        require(cond == 0);
        bool c = cond == 1;
        uint r = c ? 11 : 22;
        assert(r == 22);
    }

    /// solc: expressions/conditional_expression_storage_memory_1.sol — the upstream result is
    /// read back through a chain of independent `if`s, the last matching one winning.
    function chainedIfsSelectLastMatch() public {
        target.a = 2;
        uint t = target.a;
        uint r = 0;
        if (t == 1) {
            r = 1;
        }
        if (t == 2) {
            r = 2;
        }
        assert(r == 2);
    }

    /// solc: expressions/conditional_expression_multiple.sol — the same branch structure as a
    /// nested `if`/`else` rather than a nested `?:`.
    /// @custom:key box
    function nestedIfElseSelectsBranch(uint x) public pure {
        require(x == 500);
        uint d = 0;
        if (x > 100) {
            if (x > 1000) {
                d = 1000;
            } else {
                d = 100;
            }
        } else {
            if (x > 50) {
                d = 50;
            } else {
                d = 10;
            }
        }
        assert(d == 100);
    }

    function setEx(uint v) internal returns (uint) {
        ex = v;
        return v;
    }

    /// solc: semanticTests/viaYul/tuple_evaluation_order.sol — `(y, y, y) = (set(1), set(2),
    /// set(3))`: the right-hand side runs left to right, so `x` ends at 3, and the components
    /// are stored right to left, so the last store into `y` is the first component, 1.
    function tupleEvaluationOrder() public {
        (ey, ey, ey) = (setEx(1), setEx(2), setEx(3));
        assert(ey == 1 && ex == 3);
    }

    /// solc: semanticTests/viaYul/local_tuple_assignment.sol — upstream `f(23, 42) -> 23, 42`
    /// and then `x() -> 42`: a tuple declaration, then a tuple assignment mixing locals and a
    /// state variable.
    /// @custom:key box
    function localTupleAssignmentMixed(uint a1, uint a2) public {
        require(a1 >= 0 && a1 < 100);
        require(a2 >= 0 && a2 < 100);
        uint r1;
        uint r2;
        (uint b1, uint b2) = (a1, a2);
        (r1, lx, r2) = (b1, b2, b2);
        assert(r1 == a1);
        assert(lx == a2);
        assert(r2 == a2);
    }

    /// solc: semanticTests/viaYul/local_tuple_assignment.sol — upstream `g() -> 3, 42, 1`, run
    /// after `f` left `x == 42`: memory array elements as tuple targets.
    function localTupleAssignmentMemoryTargets() public {
        lx = 42;
        uint256[3] memory m;
        (m[0], m[1], m[2]) = (1, lx, 3);
        assert(m[2] == 3);
        assert(m[1] == 42);
        assert(m[0] == 1);
    }

    /// solc: semanticTests/viaYul/local_tuple_assignment.sol — upstream `h() -> 4, 42, 1`: a
    /// skipped component, and `m[0]` assigned twice; the components are stored right to left,
    /// so the leftmost value, 1, is the one `m[0]` keeps.
    function localTupleAssignmentRepeatedTarget() public {
        lx = 42;
        uint256[3] memory m;
        (m[0], m[1], , m[2], m[0]) = (1, lx, 3, 4, 42);
        assert(m[2] == 4);
        assert(m[1] == 42);
        assert(m[0] == 1);
    }

    /// solc: semanticTests/viaYul/local_tuple_assignment.sol — upstream `i() -> 42, 23, 17, 13`:
    /// parenthesized single targets and values.
    function localTupleAssignmentParens() public pure {
        uint a;
        uint b;
        uint c;
        uint d;
        (a) = 42;
        (((((b))))) = 23;
        c = (((17)));
        (((d))) = (13);
        assert(a == 42);
        assert(b == 23);
        assert(c == 17);
        assert(d == 13);
    }

    /// solc: smtCheckerTests/types/tuple_assignment_array.sol — two distinct indices of a
    /// storage array as tuple targets; the array was grown by upstream's `p()`.
    /// @custom:key box
    function tupleAssignmentArray(uint x, uint y) public {
        require(x >= 0 && x < 2);
        require(y >= 0 && y < 2);
        require(ta.length == 2);
        require(x < ta.length);
        require(y < ta.length);
        require(x != y);
        (ta[x], ta[y]) = (2, 4);
        assert(ta[x] == 2);
        assert(ta[y] == 4);
    }

    /// solc: smtCheckerTests/types/tuple_assignment_array.sol — the same assignment with the
    /// two indices equal: the components are stored right to left, so `a[x] = 2` runs last and
    /// wins.
    /// @custom:key box
    function tupleAssignmentArrayAliased(uint x, uint y) public {
        require(x >= 0 && x < 2);
        require(y >= 0 && y < 2);
        require(ta.length == 2);
        require(x < ta.length);
        require(y == x);
        (ta[x], ta[y]) = (2, 4);
        assert(ta[x] == 2);
        assert(ta[y] == 2);
    }

    /// solc: semanticTests/viaYul/short_circuit.sol — upstream `or(0) -> true, 0` and
    /// `or(1) -> true, 8`: the assignment on the right of `||` runs only when the left is false.
    /// @custom:key box
    function shortCircuitOr(uint x) public pure {
        require(x >= 0 && x < 2);
        uint x0 = x;
        bool t = (x == 0 || ((x = 8) > 0));
        uint y = x;
        assert(t);
        if (x0 == 0) {
            assert(y == 0);
        } else {
            assert(y == 8);
        }
    }

    /// solc: semanticTests/viaYul/short_circuit.sol — upstream `and(0) -> true, 8` and
    /// `and(1) -> false, 1`: the assignment on the right of `&&` runs only when the left is true.
    /// @custom:key box
    function shortCircuitAnd(uint x) public pure {
        require(x >= 0 && x < 2);
        uint x0 = x;
        bool t = (x == 0 && ((x = 8) > 0));
        uint y = x;
        if (x0 == 0) {
            assert(t);
            assert(y == 8);
        } else {
            assert(!t);
            assert(y == 1);
        }
    }

    function decrementOne(uint x) internal pure returns (uint) {
        return x - 1;
    }

    function decrementTwo(uint x) internal pure returns (uint) {
        return x - 1;
    }

    function incrementOne(uint x) internal pure returns (uint) {
        return x + 1;
    }

    function incrementTwo(uint x) internal pure returns (uint) {
        return x + 1;
    }

    /// solc: smtCheckerTests/operators/ternary_operator_nested_2.sol — the nested `?:` reaches
    /// each decrement only with `x > 0`, so neither underflows (the checker's three safe
    /// targets); the overflow it reports in `increment2` needs `x == 2**256 - 1`, outside the pin.
    /// @custom:key box
    function ternaryNestedCalls(uint x) public pure {
        require(x >= 0 && x < 1000);
        uint r = x < 10 ? (x > 0 ? decrementOne(x) : incrementOne(x))
            : (x > 100 ? incrementTwo(x) : decrementTwo(x));
        if (x == 0) {
            assert(r == 1);
        } else if (x > 100) {
            assert(r == x + 1);
        } else {
            assert(r == x - 1);
        }
    }

    /// solc: smtCheckerTests/operators/conditional_assignment_statevar_1.sol — the constructor
    /// `b = _b` becomes an assignment at the top; `c = b ? a + 10 : ++a` is never below the
    /// updated `a`. The overflow the checker reports reverts, which the box obligation allows.
    /// @custom:key box
    function conditionalAssignmentStateVar(uint bf) public {
        require(bf >= 0 && bf <= 1);
        cb = bf == 1;
        uint c = cb ? ca + 10 : ++ca;
        assert(c >= ca);
    }

    function unreachableBranch() internal pure returns (uint) {
        assert(false);
        return 0;
    }

    /// solc: smtCheckerTests/operators/ternary_operator_nested_1.sol — `x <= 1 ? 0 : x < 2 ?
    /// unreachable() : 0`: the middle branch needs `x > 1 && x < 2`, so the `assert(false)` in
    /// it never runs.
    /// @custom:key box
    function ternaryNestedUnreachable(uint x) public pure {
        require(x >= 0 && x < 10);
        uint r = x <= 1 ? 0 : x < 2 ? unreachableBranch() : 0;
        assert(r == 0);
    }

    /// solc: smtCheckerTests/operators/conditional_assignment_nested_always_true.sol
    /// @custom:key box
    function conditionalNestedAlwaysTrue(uint p1, uint p2) public pure {
        require(p1 <= 1);
        require(p2 <= 1);
        bool b1 = p1 == 1;
        bool b2 = p2 == 1;
        require(b1 || b2);
        uint c = b1 ? 3 : (b2 ? 2 : 1);
        assert(c > 1);
    }

    /// solc: smtCheckerTests/operators/conditional_assignment_always_true.sol — the upstream
    /// `assert(c < 5)` is a reported violation (counterexample `c = 5`), so it becomes
    /// `c == 5`.
    /// @custom:key box
    function conditionalAlwaysTrue(uint p) public pure {
        require(p <= 1);
        bool b = p == 1;
        require(b);
        uint c = b ? 5 : 1;
        assert(c == 5);
    }
}
