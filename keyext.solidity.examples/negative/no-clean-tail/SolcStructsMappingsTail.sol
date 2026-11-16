// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// From semantics/SolcStructsMappings.sol. Each claim holds on a chain whose storage beyond every array length
// is zero, and stays open in SolKey, which assumes no such clean tail (README.md).
contract SolcStructsMappingsTail {
    struct V { uint value; }
    struct PushZero { uint x; uint y; uint z; uint[3] a1; uint[] a2; }

    uint pushB;
    PushZero[] pushZero;
    uint pushA;
    V[][] va;
    V[] vb;

    /// solc: semanticTests/structs/copy_struct_array_from_storage.sol
    /// @custom:key box
    function copyStructArrayIntoNestedArray() public {
        require(va.length == 0);
        require(vb.length == 0);
        va.push();
        va[0].push();
        va[0][0].value = 1;
        va[0].push();
        va[0][1].value = 2;
        va[0].push();
        va[0][2].value = 3;
        vb.push();
        vb[0].value = 4;
        vb.push();
        vb[1].value = 5;
        vb.push();
        vb[2].value = 6;
        vb.push();
        vb[3].value = 7;
        va.push();
        va[1] = vb;
        assert(va.length == 2);
        assert(va[0].length == 3);
        assert(va[1].length == 4);
        assert(va[1][0].value == 4);
        assert(va[1][1].value == 5);
        assert(va[1][2].value == 6);
        assert(va[1][3].value == 7);
    }

    /// solc: semanticTests/structs/struct_storage_push_zero_value.sol
    /// @custom:key box
    function pushedStructIsZero() public {
        require(pushZero.length == 0);
        pushB = 23;
        pushA = 17;
        pushZero.push();
        assert(pushZero[0].x == 0);
        assert(pushZero[0].y == 0);
        assert(pushZero[0].z == 0);
        assert(pushZero[0].a1[0] == 0);
        assert(pushZero[0].a1[1] == 0);
        assert(pushZero[0].a1[2] == 0);
        assert(pushZero[0].a2.length == 0);
        assert(pushB == 23);
        assert(pushA == 17);
    }
}
