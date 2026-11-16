// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// From taclets/StorageArrays.sol. Each claim holds on a chain whose storage beyond every array length
// is zero, and stays open in SolKey, which assumes no such clean tail (README.md).
contract StorageArraysTail {
    struct Token { uint value; }

    uint[] values;
    uint[9] copySource9;
    uint[] copyTargetDyn;
    uint[3][] rows;
    uint[][] matrix;
    Token[] tokens;

    function testEmptyPushReadsDefaultValue() public {
        uint n = values.length;
        values.push();
        assert(values.length == n + 1);
        assert(values[n] == 0);
    }

    // import
    function testFixedCopyThenEmptyPushReadsZero() public {
        copySource9[8] = 41;
        copyTargetDyn = copySource9;
        copySource9[8] = 42;
        copyTargetDyn.push();
        assert(copySource9.length == 9);
        assert(copyTargetDyn.length == 10);
        assert(copyTargetDyn[8] == 41);
        assert(copyTargetDyn[9] == 0);
    }

    function testFixedShapeEmptyPushFixedElementReadsZero() public {
        uint n = rows.length;
        rows.push();
        assert(rows.length == n + 1);
        assert(rows[n].length == 3);
        assert(rows[n][0] == 0);
        assert(rows[n][2] == 0);
    }

    function testEmptyPushFixedArrayReturnReadsZero() public {
        uint[3] storage row = rows.push();
        assert(row.length == 3);
        assert(row[0] == 0);
        assert(row[2] == 0);
    }

    function testEmptyPushDynamicArrayReturnReadsZero() public {
        uint[] storage row = matrix.push();
        assert(row.length == 0);
        row.push();
        assert(row[0] == 0);
    }

    function testEmptyPushStructReturnReadsZero() public {
        Token storage token = tokens.push();
        assert(token.value == 0);
    }
}
