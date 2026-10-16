// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

contract SolcStructsMappingsOpen {
    struct V { uint value; }
    struct PushZero { uint x; uint y; uint z; uint[3] a1; uint[] a2; }

    uint pushB;
    PushZero[] pushZero;
    uint pushA;
    V[][] va;
    V[] vb;

    /// solc: semanticTests/structs/copy_struct_array_from_storage.sol
    // open: temp[0] = va[0] (storage array copied into an element of a memory array of arrays) has no rule: symbolic execution stops on the statement
    /// @custom:key box
    function copyStructArraysIntoMemoryRows() public {
        require(va.length == 1);
        require(va[0].length == 3);
        require(vb.length == 4);
        vb[0].value = 4;
        vb[1].value = 5;
        vb[2].value = 6;
        vb[3].value = 7;
        V[][] memory temp = new V[][](2);
        temp[0] = va[0];
        temp[1] = vb;
        assert(temp.length == 2);
        assert(temp[0].length == 3);
        assert(temp[1].length == 4);
        assert(temp[1][0].value == 4);
        assert(temp[1][3].value == 7);
    }



}
