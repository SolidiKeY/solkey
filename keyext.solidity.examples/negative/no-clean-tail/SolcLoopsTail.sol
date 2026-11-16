// SPDX-License-Identifier: GPL-2.0-only
pragma solidity ^0.8.0;

// From semantics/SolcLoops.sol. Each claim holds on a chain whose storage beyond every array length
// is zero, and stays open in SolKey, which assumes no such clean tail (README.md).
contract SolcLoopsTail {
    uint[] storageArray;
    uint[][] array2d;

    /// solc: array/array_storage_index_boundary_test.sol
    function boundaryTestPushedSlotsAreZero() public {
        delete storageArray;
        uint len = 10;
        while (storageArray.length < len)
            storageArray.push();
        while (storageArray.length > len)
            storageArray.pop();
        uint l = storageArray.length;
        uint r = storageArray[9];
        assert(l == 10);
        assert(r == 0);
    }

    /// solc: array/push/push_no_args_2d.sol
    /// @custom:key box
    function pushChainedAssign(uint value) public {
        require(value == 512);
        require(array2d.length == 0);
        array2d.push().push() = value;
        uint a = array2d[0][0];
        assert(a == 512);
    }
}
