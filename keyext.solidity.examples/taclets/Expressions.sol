// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// Taclet examples: operators, literals, conditionals and local variables.
// Conventions: README.md, "Writing an example".
contract Expressions {
    struct Token { uint value; }
    struct Account { uint balance; Token token; }
    struct Person { Account account; uint age; }
    uint age;
    bool flag;
    Person alice;
    Person bob;

    function additionBothStorage() public {
        alice.age = 10;
        bob.age = 5;
        uint r = alice.age + bob.age;
        assert(r == 15);
    }

    function additionSimple() public pure {
        uint r = 1 + 2;
        assert(r == 3);
    }

    function additionStorageRead() public {
        alice.age = 10;
        uint r = alice.age + 1;
        assert(r == 11);
    }

    function divisionSimple() public pure {
        uint r = 8 / 2;
        assert(r == 4);
    }

    function greaterEqualSimple() public pure {
        bool r = 5 >= 6;
        assert(!r);
    }

    function greaterThanSimple() public pure {
        bool r = 5 > 3;
        assert(r);
    }

    function lessEqualSimple() public pure {
        bool r = 5 <= 5;
        assert(r);
    }

    function lessThanSimple() public pure {
        bool r = 3 < 5;
        assert(r);
    }

    function logicalAndSimple() public pure {
        bool r = true && false;
        assert(!r);
    }

    function logicalAndShortCircuitRhs() public pure {
        uint x = 5;
        bool r = x == 5 && x + 1 == 6;
        assert(r);
    }

    function logicalAndStorageAssignment(bool left) public {
        flag = false;
        bool r = left && (flag = true);
        assert(r == left);
        assert(flag == left);
    }

    function logicalOrStorageAssignment(bool left) public {
        flag = true;
        bool r = left || (flag = false);
        assert(r == left);
        assert(flag == left);
    }

    function unprovableStorageShortCircuitMutation() public {
        flag = false;
        bool r = false && (flag = true);
        assert(!r);
        assert(flag);
    }

    function logicalNotSimple() public pure {
        bool r = !false;
        assert(r);
    }

    function logicalOrSimple() public pure {
        bool r = true || false;
        assert(r);
    }

    function logicalOrShortCircuitRhs() public pure {
        uint x = 5;
        bool r = x == 4 || x + 1 == 6;
        assert(r);
    }

    function ternaryCaptureCond() public pure {
        uint x = 5;
        uint r = x == 5 ? x + 1 : 0;
        assert(r == 6);
    }

    function ternaryToIf() public pure {
        bool b = true;
        uint r = b ? 1 : 2;
        assert(r == 1);
    }

    function ifUnfold() public pure {
        uint x = 5;
        uint r = 0;
        if (x == 4) r = 1;
        assert(r == 0);
    }

    function ifElseUnfold() public pure {
        uint x = 5;
        uint r = 0;
        if (x == 5) r = 1; else r = 2;
        assert(r == 1);
    }

    function ifSplit() public pure {
        bool b = true;
        uint r = 0;
        if (b) r = 1;
        assert(r == 1);
    }

    function ifElseSplit() public pure {
        bool b = false;
        uint r = 0;
        if (b) {
            r = 1;
        } else {
            r = 2;
        }
        assert(r == 2);
    }

    function ifTrue() public pure {
        uint r = 0;
        if (true) r = 1;
        assert(r == 1);
    }

    function ifFalse() public pure {
        uint r = 0;
        if (false) r = 1;
        assert(r == 0);
    }

    function ifElseTrue() public pure {
        uint r = 0;
        if (true) r = 1; else r = 2;
        assert(r == 1);
    }

    function ifElseFalse() public pure {
        uint r = 0;
        if (false) r = 1; else r = 2;
        assert(r == 2);
    }

    function ifElseNegated() public pure {
        bool b = false;
        uint r = 0;
        if (!b) r = 1; else r = 2;
        assert(r == 1);
    }

    function moduloSimple() public pure {
        uint r = 7 % 3;
        assert(r == 1);
    }

    function multiplicationSimple() public pure {
        uint r = 3 * 4;
        assert(r == 12);
    }

    function notEqualSimple() public pure {
        bool r = 3 != 4;
        assert(r);
    }

    function powerSimple() public pure {
        uint r = 2 ** 3;
        assert(r == 8);
    }

    function subtractionSimple() public pure {
        uint r = 7 - 2;
        assert(r == 5);
    }

    function subtractionStorageRead() public {
        alice.age = 10;
        uint r = alice.age - 3;
        assert(r == 7);
    }

    /// @custom:key box
    function unaryMinusSimple(int x) public pure {
        require(x == 5);
        int r = -x;
        // a negative literal directly inside the assert condition does not discharge
        int expected = -5;
        assert(r == expected);
    }

    function uninitialisedLocalsAreDefault() public pure {
        uint x;
        bool b;
        address addr;
        assert(x == 0);
        assert(!b);
        assert(addr == address(0));
    }

    function arithmeticAssignmentValues() public pure {
        uint left = 28;
        left += left += left;
        assert(left == 112);
        uint result = (left = 24);
        assert(result == 24 && left == 24);
        result = (left -= 4);
        assert(result == 20 && left == 20);
        result = (left *= 3);
        assert(result == 60 && left == 60);
        result = (left /= 4);
        assert(result == 15 && left == 15);
        result = (left %= 4);
        assert(result == 3 && left == 3);
    }

    // import
    function arithmeticDiscardKeepsSideEffects() public pure {
        uint ix = 0;
        ix++ + ix++;
        assert(ix == 2);
        ix++ - ix++;
        assert(ix == 4);
        ix++ * ix++;
        assert(ix == 6);
        ix++ / ix++;
        assert(ix == 8);
        ix++ % ix++;
        assert(ix == 10);
        ix++ ** 2;
        assert(ix == 11);
    }

    // import
    /// @custom:key box
    function arithmeticDiscardKeepsZeroDivisorRevert() public pure {
        uint numerator = 1;
        uint denominator = 0;
        numerator / denominator;
        assert(false);
    }

    /// @custom:key box
    function arithmeticRemainderBoundAllSigns(int numerator, int denominator) public pure {
        require(denominator > 0);
        int result = numerator % denominator;
        assert(result < denominator);
    }

    function testUnsignedParameterNonnegative(uint x) public pure {
        assert(x >= 0);
    }

    // ── Arithmetic ──

    /// @custom:key box
    function additionStorageWrite(uint x, uint y) public {
        require(x == 5 && y == 7);
        alice.age = x + y;
        uint r = alice.age;
        assert(r == 12);
    }

    // ── arithmetic on pinned ranges ──

    /// @custom:key box
    function localArithmeticInRange(uint x) public pure {
        require(x >= 1 && x <= 100);
        uint r;
        r = x + 1;
        r -= 1;
        ++r;
        assert(r == x + 1);
    }

    /// @custom:key box
    function signedUnaryMinusInRange(int8 x) public pure {
        require(x == 5);
        int8 r;
        int8 expected;
        r = -x;
        expected = -5;
        assert(r == expected);
    }

    // ── Number literals ──

    function numberLiteralForms() public pure {
        uint h = 0xff;
        uint u = 1_000;
        uint e = 2e3;
        uint g = 1 gwei;
        uint d = 2 days;
        assert(h == 255);
        assert(u == 1000);
        assert(e == 2000);
        assert(g == 1000000000);
        assert(d == 172800);
    }

    // ── Taclet coverage: one function per taclet no other example applies ──

    function localAddAssign() public pure {
        uint x = 40;
        x += 2;
        assert(x == 42);
    }

    function localMulAssign() public pure {
        uint x = 7;
        x *= 6;
        assert(x == 42);
    }

    function localDivAssign() public pure {
        uint x = 40;
        x /= 8;
        assert(x == 5);
    }

    function localModAssign() public pure {
        uint x = 40;
        x %= 7;
        assert(x == 5);
    }

    function localPostincrement() public pure {
        uint x = 40;
        x++;
        assert(x == 41);
    }

    function localPreincrementAssign() public pure {
        uint x = 40;
        uint r = 0;
        r = ++x;
        assert(r == 41);
        assert(x == 41);
    }

    function localPostdecrement() public pure {
        uint x = 40;
        x--;
        assert(x == 39);
    }

    function localPredecrement() public pure {
        uint x = 40;
        --x;
        assert(x == 39);
    }

    function localDeclPostdecrement() public pure {
        uint x = 40;
        uint r = x--;
        assert(r == 40);
        assert(x == 39);
    }

    function localDeclPredecrement() public pure {
        uint x = 40;
        uint r = --x;
        assert(r == 39);
        assert(x == 39);
    }

    function subAssignValueRhsCapture() public pure {
        uint x = 40;
        uint y = 1;
        x -= y + 1;
        assert(x == 38);
    }

    function mulAssignValueRhsCapture() public pure {
        uint x = 7;
        uint y = 5;
        x *= y + 1;
        assert(x == 42);
    }

    function divAssignValueRhsCapture() public pure {
        uint x = 40;
        uint y = 7;
        x /= y + 1;
        assert(x == 5);
    }

    function modAssignValueRhsCapture() public pure {
        uint x = 40;
        uint y = 6;
        x %= y + 1;
        assert(x == 5);
    }

    function boolInequalityCaptureLhs() public pure {
        bool b = true;
        bool r = !b != b;
        assert(r);
    }

    function boolInequalityCaptureRhs() public pure {
        bool b = true;
        bool r = b != !b;
        assert(r);
    }

    function greaterThanCaptureRhs() public pure {
        uint x = 3;
        bool r = x > x - 1;
        assert(r);
    }

    function greaterEqualCaptureLhs() public pure {
        uint x = 3;
        bool r = x + 1 >= x;
        assert(r);
    }

    function greaterEqualCaptureRhs() public pure {
        uint x = 3;
        bool r = x >= x - 1;
        assert(r);
    }

    function lessEqualCaptureLhs() public pure {
        uint x = 3;
        bool r = x - 1 <= x;
        assert(r);
    }

    function lessEqualCaptureRhs() public pure {
        uint x = 3;
        bool r = x <= x + 1;
        assert(r);
    }

    function logicalNotCapture() public pure {
        uint x = 3;
        bool r = !(x == 4);
        assert(r);
    }

    function unaryMinusCapture() public pure {
        int x = 3;
        int r = -(x + 1);
        assert(r == -4);
    }

    function multiplicationUnfoldLeft() public pure {
        uint x = 5;
        uint y = 7;
        uint r = (x + 1) * y;
        assert(r == 42);
    }

    function multiplicationUnfoldRight() public pure {
        uint x = 7;
        uint y = 5;
        uint r = x * (y + 1);
        assert(r == 42);
    }

    function divisionUnfoldLeft() public pure {
        uint x = 39;
        uint y = 8;
        uint r = (x + 1) / y;
        assert(r == 5);
    }

    function divisionUnfoldRight() public pure {
        uint x = 40;
        uint y = 7;
        uint r = x / (y + 1);
        assert(r == 5);
    }

    function moduloUnfoldLeft() public pure {
        uint x = 39;
        uint y = 7;
        uint r = (x + 1) % y;
        assert(r == 5);
    }

    function moduloUnfoldRight() public pure {
        uint x = 40;
        uint y = 6;
        uint r = x % (y + 1);
        assert(r == 5);
    }

    function signedDivisionTruncates() public pure {
        int n = -7;
        int b = -2;
        int q1 = n / 2;
        int q2 = 7 / b;
        int q3 = n / b;
        int minusThree = -3;
        assert(q1 == minusThree);
        assert(q2 == minusThree);
        assert(q3 == 3);
    }

    function signedModuloTakesDividendSign() public pure {
        int n = -7;
        int b = -2;
        int r1 = n % 2;
        int r2 = 7 % b;
        int r3 = n % b;
        int minusOne = -1;
        assert(r1 == minusOne);
        assert(r2 == 1);
        assert(r3 == minusOne);
    }

    function powerUnfoldLeft() public pure {
        uint x = 1;
        uint y = 3;
        uint r = (x + 1) ** y;
        assert(r == 8);
    }

    function powerUnfoldRight() public pure {
        uint x = 2;
        uint y = 2;
        uint r = x ** (y + 1);
        assert(r == 8);
    }

    function powerUnfoldLeftNarrowExponent() public pure {
        uint256 x = 1;
        uint8 y = 3;
        uint256 r = (x + 1) ** y;
        assert(r == 8);
    }

    function powerUnfoldRightNarrowExponent() public pure {
        uint256 x = 2;
        uint8 y = 2;
        uint256 r = x ** (y + 1);
        assert(r == 8);
    }

    function subtractionUnfoldRight() public pure {
        uint x = 40;
        uint y = 1;
        uint r = x - (y + 1);
        assert(r == 38);
    }

    function parenthesizedRightOperand() public pure {
        uint x = 10;
        uint y = 5;
        uint z = 3;
        uint r = x - (y - z);
        assert(r == 8);
    }

    function parenthesizedLeftOperand() public pure {
        uint x = 2;
        uint y = 3;
        uint z = 4;
        uint r = (x + y) * z;
        assert(r == 20);
    }

    function parenthesizedCondition() public pure {
        bool p = true;
        bool b = false;
        bool c = true;
        bool r = p && (b || c);
        assert(r);
    }

    function ternaryToIfStorage() public {
        bool b = true;
        age = b ? 1 : 2;
        uint r = age;
        assert(r == 1);
    }
}
