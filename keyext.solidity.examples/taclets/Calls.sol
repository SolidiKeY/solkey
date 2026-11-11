// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

uint constant FILE_LEVEL_LIMIT = 42;

enum FileLevelColor { Red, Green, Blue }

struct FileLevelPair { uint left; uint right; }

type Wei is uint;

function fileLevelTwice(uint v) pure returns (uint) {
    return 2 * v;
}

library PairLib {
    uint constant UNIT = 10;

    function bump(uint v) internal pure returns (uint) {
        return v + UNIT;
    }

    function swap(FileLevelPair storage p) internal {
        uint l = p.left;
        p.left = p.right;
        p.right = l;
    }
}

// Taclet examples: internal calls, returns, tuples, parameters and file-level declarations.
// Conventions: README.md, "Writing an example".
contract Calls {
    using PairLib for uint;

    struct Token { uint value; }

    uint total;
    address owner;
    mapping(uint => uint) valuesMap;
    Token tok;
    FileLevelPair pair;

    uint constant LIMIT = 7;
    enum Phase { Open, Closed }

    function firstOf(uint[] memory xs) internal pure returns (uint) {
        return xs[0];
    }

    function indexParameter() public pure {
        uint[] memory xs = new uint[](1);
        xs[0] = 9;
        uint r = firstOf(xs);
        assert(r == 9);
    }

    function setTokenValue(Token storage t, uint v) internal {
        t.value = v;
    }

    function readTokenValue(Token storage t) internal view returns (uint) {
        return t.value;
    }

    function storageStructParameter() public {
        setTokenValue(tok, 4);
        uint r = readTokenValue(tok);
        assert(r == 4);
    }

    function credit(mapping(uint => uint) storage m, uint k) internal {
        m[k] = 1;
    }

    function storageMappingParameter() public {
        credit(valuesMap, 3);
        uint r = valuesMap[3];
        assert(r == 1);
    }

    /// @custom:key box
    function pinWithInequality(uint x, uint y) public pure {
        require(y != 0 && x == 42);
        assert(x == 42);
    }

    /// @custom:key box
    function payableAddressParameter(address payable p) public pure {
        require(p == 0x5B38Da6a701c568545dCfcB03FcB875f56beddC4);
        assert(p != address(0));
    }

    function digits(uint p, uint q, uint s) internal pure returns (uint r) {
        r = p * 100 + q * 10 + s;
    }

    function namedArgumentsBindByName() public pure {
        uint r = digits({q: 2, s: 3, p: 1});
        assert(r == 123);
    }

    // ── Internal calls, return statements and tuples ──

    function returnOne() internal pure returns (uint) {
        return 1;
    }

    function bitAndOneValues() public pure {
        uint odd = 7;
        uint even = 8;
        int negative = -3;
        assert((odd & 1) == 1);
        assert((even & 1) == 0);
        assert((negative & 1) == 1);
        assert(((odd + even) & 1) == 1);
        assert((returnOne() & 1) == 1);
    }

    function bitAndGeneralValues() public pure {
        uint x = 13;
        uint y = 10;
        int negative = -3;
        int otherNegative = -6;
        assert((x & y) == 8);
        assert((x & 7) == 5);
        assert((negative & 6) == 4);
        assert((negative & otherNegative) == -8);
        assert(((x + 2) & (y - 1)) == 9);
        assert((returnOne() & returnOne()) == 1);
        uint256 high = 0x100000000000000000000000000000000;
        assert((high & (high + 7)) == high);
    }

    function bitAndSymbolic(int x) public pure {
        assert((x & 0) == 0);
        assert((0 & x) == 0);
        assert((x & x) == x);
        int parity = x & 1;
        int reversed = 1 & x;
        assert(parity == reversed);
        assert(parity >= 0 && parity < 2);
    }

    function bitAndSetTotal() internal returns (uint) {
        total = 7;
        return 7;
    }

    // import
    function bitAndOperandEvaluation() public {
        total = 12;
        uint r = bitAndSetTotal() & total;
        assert(r == 4);
        assert(total == 7);
        total = 12;
        r = total & bitAndSetTotal();
        assert(r == 7);
        assert(total == 7);
    }

    function bitXorGeneralValues() public pure {
        uint x = 13;
        uint y = 10;
        int negative = -3;
        assert((x ^ y) == 7);
        assert((x ^ 7) == 10);
        assert((negative ^ 6) == -5);
        assert(((x + 2) ^ (y - 1)) == 6);
        assert(((x + 2) ^ y) == 5);
        assert((returnOne() ^ returnOne()) == 0);
    }

    function bitXorSymbolic(int x) public pure {
        assert((x ^ 0) == x);
        assert((0 ^ x) == x);
        assert((x ^ x) == 0);
    }

    function bitXorAverage(uint x, uint y) public pure {
        assert((x & y) + (x ^ y) / 2 == (x + y) / 2);
    }

    // import
    function bitXorOperandEvaluation() public {
        total = 12;
        uint r = bitAndSetTotal() ^ total;
        assert(r == 11);
        assert(total == 7);
        total = 12;
        r = total ^ bitAndSetTotal();
        assert(r == 0);
        assert(total == 7);
    }

    function returnSign(int v) internal pure returns (int) {
        if (v < 0) return -1;
        if (v == 0) return 0;
        return 1;
    }

    function retConditionalTwo() internal pure returns (uint) {
        return 2;
    }

    function conditionalCallee(bool choose) public pure {
        int r = (choose ? returnSign : returnSign)(-3);
        assert(r < 0);
        uint v = (choose ? returnOne : retConditionalTwo)() + 1;
        assert(choose ? v == 2 : v == 3);
    }

    function conditionalSetOne() internal {
        total = 1;
    }

    function conditionalSetTwo() internal {
        total = 2;
    }

    function conditionalCalleeEffects() public {
        total = 0;
        (true ? conditionalSetOne : conditionalSetTwo)();
        assert(total == 1);
        (false ? conditionalSetOne : conditionalSetTwo)();
        assert(total == 2);
    }

    function internalCallDeclaration() public pure {
        uint r = returnOne();
        assert(r == 1);
    }

    function internalCallInExpression() public pure {
        uint r = returnOne() + returnOne();
        assert(r == 2);
    }

    function internalCallToStorage() public {
        total = returnOne();
        assert(total == 1);
    }

    function returnEarly() public pure {
        int negative = returnSign(-3);
        int zero = returnSign(0);
        int positive = returnSign(4);
        assert(negative < 0 && zero == 0 && positive > 0);
    }

    function returnOrdered(uint x, uint y) internal pure returns (uint lo, uint hi) {
        if (x < y) return (x, y);
        return (y, x);
    }

    function tupleReturnPair() public pure {
        (uint lo, uint hi) = returnOrdered(5, 2);
        assert(lo == 2 && hi == 5);
    }

    function returnSwapped() internal pure returns (uint x, uint y) {
        x = 1;
        y = 2;
        return (y, x);
    }

    function tupleReturnReadsReturnVariables() public pure {
        (uint x, uint y) = returnSwapped();
        assert(x == 2 && y == 1);
    }

    function returnStats(uint x, uint y, uint z)
        internal pure returns (uint lo, uint hi, uint sum, bool same)
    {
        lo = x < y ? x : y;
        lo = lo < z ? lo : z;
        hi = x > y ? x : y;
        hi = hi > z ? hi : z;
        return (lo, hi, x + y + z, x == y && y == z);
    }

    function tupleReturnDiscardsComponents() public pure {
        (uint lo, , uint sum, bool same) = returnStats(3, 1, 2);
        assert(lo == 1 && sum == 6 && !same);
    }

    function tupleReturnAssignsExisting() public {
        uint hi;
        (total, hi) = returnOrdered(9, 4);
        assert(total == 4 && hi == 9);
    }

    function tupleAssignmentRotates() public pure {
        uint first = 1;
        uint second = 2;
        uint third = 3;
        (first, second, third) = (second, third, first);
        assert(first == 2 && second == 3 && third == 1);
    }

    function tupleDeclaration() public pure {
        (uint first, , bool third) = (4, 5, true);
        assert(first == 4 && third);
    }

    function returnFromNestedBlock(uint x) internal pure returns (uint) {
        {
            {
                if (x > 0) {
                    return 1;
                }
            }
            x = 5;
        }
        return x;
    }

    function returnLeavesNestedBlocks() public pure {
        uint one = returnFromNestedBlock(3);
        uint five = returnFromNestedBlock(0);
        assert(one == 1 && five == 5);
    }

    function returnOrFallThrough(bool stop) internal pure returns (uint r) {
        r = 5;
        if (stop) return r;
        r = 6;
    }

    function returnOrFallThroughNamed() public pure {
        uint stopped = returnOrFallThrough(true);
        uint finished = returnOrFallThrough(false);
        assert(stopped == 5 && finished == 6);
    }

    function returnAddOne(uint x) internal pure returns (uint) {
        return x + 1;
    }

    function returnAddTwo(uint x) internal pure returns (uint) {
        uint y = returnAddOne(x);
        return returnAddOne(y);
    }

    function returnFromNestedCall() public pure {
        uint r = returnAddTwo(1);
        assert(r == 3);
    }

    function returnRevertsOnZero(uint x) internal pure returns (uint) {
        if (x == 0) revert();
        return x;
    }

    function returnAfterRevertGuard() public pure {
        uint r = returnRevertsOnZero(3);
        assert(r == 3);
    }

    /// @custom:key skip
    function tryCalleeGet(uint x) external pure returns (uint) {
        return x;
    }

    function returnInsideTry() internal view returns (uint) {
        try Calls(owner).tryCalleeGet(5) returns (uint) {
            return 7;
        } catch {
            return 8;
        }
    }

    /// @custom:key box
    function returnFromTryBranches() public view {
        uint r = returnInsideTry();
        assert(r == 7 || r == 8);
    }

    function returnVoidEarly(uint v) internal {
        if (total != 0) return;
        total = v;
    }

    function returnFromVoidFunction() public {
        total = 4;
        returnVoidEarly(9);
        assert(total == 4);
        total = 0;
        returnVoidEarly(9);
        assert(total == 9);
    }

    function returnDefaults() internal pure returns (uint r, bool b) {
    }

    function returnDefaultsAreZero() public pure {
        (uint r, bool b) = returnDefaults();
        assert(r == 0 && !b);
    }

    function returnDefaultAddress() internal pure returns (address who) {
    }

    function returnDefaultAddressIsZero() public pure {
        address who = returnDefaultAddress();
        assert(who == address(0));
    }

    function returnDefaultEnum() internal pure returns (Phase p) {
    }

    function returnDefaultEnumIsFirstMember() public pure {
        Phase p = returnDefaultEnum();
        assert(p == Phase.Open);
    }

    function returnDefaultMemoryArray() internal pure returns (uint[] memory xs) {
    }

    function returnDefaultMemoryArrayIsEmpty() public pure {
        uint[] memory xs = returnDefaultMemoryArray();
        assert(xs.length == 0);
    }

    function returnInThenBranch(bool b) internal pure returns (uint r, bool ok) {
        if (b) {
            return (5, true);
        } else {
        }
    }

    function returnInThenBranchElseIsDefault() public pure {
        (uint r, bool ok) = returnInThenBranch(true);
        assert(r == 5 && ok);
        (r, ok) = returnInThenBranch(false);
        assert(r == 0 && !ok);
    }

    function returnInElseBranch(bool b) internal view returns (address who) {
        if (b) {
        } else {
            return owner;
        }
    }

    function returnInElseBranchThenIsDefault() public view {
        address returned = returnInElseBranch(false);
        address fellThrough = returnInElseBranch(true);
        assert(returned == owner);
        assert(fellThrough == address(0));
    }

    function returnDefaultIsFreshPerCall() public pure {
        (uint first, ) = returnInThenBranch(true);
        (uint second, bool ok) = returnInThenBranch(false);
        assert(first == 5 && second == 0 && !ok);
    }

    function returnEnumInOneBranch(bool b) internal pure returns (Phase p) {
        if (b) return Phase.Closed;
    }

    function returnEnumOtherBranchIsDefault() public pure {
        Phase closed = returnEnumInOneBranch(true);
        Phase open = returnEnumInOneBranch(false);
        assert(closed == Phase.Closed && open == Phase.Open);
    }

    function returnAssignsOneOfTwo() internal pure returns (uint r, address who) {
        r = who == address(0) ? 3 : 4;
    }

    function returnUnassignedComponentIsDefault() public pure {
        (uint r, address who) = returnAssignsOneOfTwo();
        assert(r == 3 && who == address(0));
    }

    function returnReadsItsDefault() internal pure returns (uint r, bool wasZero) {
        wasZero = r == 0;
    }

    function returnDefaultIsVisibleInBody() public pure {
        (uint r, bool wasZero) = returnReadsItsDefault();
        assert(r == 0 && wasZero);
    }

    function overloadPick(uint x) internal pure returns (uint) {
        return x + 1;
    }

    function overloadPick(bool b) internal pure returns (uint) {
        return b ? 10 : 20;
    }

    function overloadedInternalCall() public pure {
        uint n = overloadPick(1);
        uint t = overloadPick(true);
        assert(n == 2 && t == 10);
    }

    // ── Constants, file-level declarations and libraries ──

    function constantInlined() public pure {
        uint r = LIMIT;
        assert(r == 7);
    }

    function contractQualifiedConstantAndEnum() public pure {
        uint r = Calls.LIMIT;
        Calls.Phase p = Calls.Phase.Closed;
        assert(r == 7);
        assert(p == Phase.Closed);
    }

    function fileLevelConstantEnumAndFunction() public pure {
        uint r = fileLevelTwice(FILE_LEVEL_LIMIT);
        FileLevelColor c = FileLevelColor.Blue;
        assert(r == 84);
        assert(uint(c) == 2);
    }

    function fileLevelStructThroughLibrary() public {
        pair.left = 1;
        pair.right = 2;
        PairLib.swap(pair);
        uint l = pair.left;
        assert(l == 2);
    }

    function libraryCallAndUsingFor() public pure {
        uint v = 5;
        assert(PairLib.bump(v) == 15);
        assert(v.bump() == 15);
        assert(PairLib.UNIT == 10);
    }

    function valueTypeWrapUnwrap() public pure {
        Wei w = Wei.wrap(3);
        assert(Wei.unwrap(w) == 3);
    }

    function typeBounds() public pure {
        uint8 m = type(uint8).max;
        int8 n = type(int8).min;
        int e = -128;
        assert(m == 255);
        assert(n == e);
        assert(uint(type(FileLevelColor).max) == 2);
    }

    function addmodMulmod() public pure {
        assert(addmod(10, 5, 7) == 1);
        assert(mulmod(3, 5, 7) == 1);
    }
}
